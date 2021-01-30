package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.ArrayBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

public class FullBackupModelBasedOM extends ReinforcementLearningOM {

	private QTable qTable;

	int[][] transitionsMatrix;
	double[][] pMatrix;
	double[][] unknownCostEst;

	private double gamma;

	// TODO: restore contraints in the learning step
	private boolean initWithApproximateModel = true;
	private int offlineObservationsParam = 10000;

	private VariableParameter alpha;
	private int alphaDecaySteps;
	private int alphaDecayStepsCounter;

	private final int skipFullBackupAfter = 100000;
	private final int fullBackupEvery = 100;
	private int time = 0;

	private int onlineVIMaxIter = 3; // TODO configurable

	private Logger logger = LoggerFactory.getLogger(FullBackupModelBasedOM.class);


	public FullBackupModelBasedOM (Operator operator) {
		super(operator);

		// get configuration instance
		Configuration configuration = Configuration.getInstance();

		/* Check we are not in the heterogeneous scenario */
		if (ComputingInfrastructure.getInfrastructure().getNodeTypes().length != 1) {
			throw new RuntimeException(this.getClass().getName() + " only supports 1 resource type.");
		}

		this.qTable = buildQ();
		this.gamma = configuration.getDouble(ConfigurationKeys.QL_OM_GAMMA_KEY,0.99);

		double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.1);
		double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 1.0);
		double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

		this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);
		this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
		this.alphaDecayStepsCounter = 0;

		logger.info("Alpha conf: init={}, decay={}, currValue={}", alphaInitValue,
				alphaDecay, alpha.getValue());

		if (initWithApproximateModel) {
			offlinePlanning();
		} else {
			offlineObservationsParam = 0;
		}

	}

	private Operator approximateOperatorModel (Operator op)
	{
		Random r = new Random(123); // TODO seed
		//final double maxErr = 0.2;
		//final double err = maxErr * r.nextDouble() -maxErr/2.0;
		//final double stMean = op.getQueueModel().getServiceTimeMean() * (1.0 + err);
		//final double stVar = Math.pow(stMean,2);

		//logger.info("Approximate stMean: {} -> {}", op.getQueueModel().getServiceTimeMean(), stMean);
		//OperatorQueueModel queueModel = new MG1OperatorQueueModel(stMean, stVar);
		OperatorQueueModel queueModel = op.getQueueModel().getApproximateModel(r);
		Operator tempOperator = new Operator("temp", queueModel, op.getMaxParallelism());
		tempOperator.setSloRespTime(op.getSloRespTime());
		return tempOperator;
	}

	private void offlinePlanning() {
		ValueIterationOM viOM = new ValueIterationOM(approximateOperatorModel(operator));
		//ValueIterationOM viOM = new ValueIterationOM(operator);
		QTable viQ = viOM.getQTable();
		DoubleMatrix<Integer,Integer> viP = viOM.getPMatrix();

		// init costs and transition estimates based on offline phase output
		/* Update cost estimate */
		StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
				ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

		while (stateIterator.hasNext()) {
			State s = stateIterator.next();
			double uc = StateUtils.computeSLOCost(s, viOM);
			unknownCostEst[s.overallParallelism()-1][s.getLambda()] = uc;

			ActionIterator actionIterator = new ActionIterator();
			while (actionIterator.hasNext()) {
				Action action = actionIterator.next();
				if (!validateAction(s, action))
					continue;

				double oldQ = viQ.getQ(s, action);
				qTable.setQ(s, action, oldQ);
			}
		}
		//dumpEstimatedCost();

		/* Update transition model */
		for (int startState = 0; startState < getInputRateLevels(); startState++) {
			for (int endState = 0; endState < getInputRateLevels(); endState++) {
				pMatrix[startState][endState] = viP.getValue(startState,endState);
			}
		}
		for (int startState = 0; startState < getInputRateLevels(); startState++) {
			int totalTrans = offlineObservationsParam;
			for (int endState = 0; endState < getInputRateLevels(); endState++) {
				transitionsMatrix[startState][endState] = (int)(pMatrix[startState][endState] * totalTrans);
			}
		}
	}

	@Override
	protected ActionSelectionPolicy initActionSelectionPolicy() {
		return  ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
	}

	private void decrementAlpha() {
		if (this.alphaDecaySteps > 0) {
			this.alphaDecayStepsCounter++;
			if (this.alphaDecayStepsCounter >= this.alphaDecaySteps) {
				this.alpha.update();
				this.alphaDecayStepsCounter = 0;
			}
		}
	}

	private QTable buildQ() {
		int maxActionHash = -1;
		int maxStateHash = -1;
		StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
				ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
		while (stateIterator.hasNext()) {
			State state = stateIterator.next();
			int h = state.hashCode();
			if (h  > maxStateHash)
				maxStateHash = h;
			ActionIterator actionIterator = new ActionIterator();
			while (actionIterator.hasNext()) {
				Action action = actionIterator.next();
				h = action.hashCode();
				if (h > maxActionHash)
					maxActionHash = h;
			}
		}

		this.transitionsMatrix = new int[getInputRateLevels()][getInputRateLevels()];
		this.pMatrix = new double[getInputRateLevels()][getInputRateLevels()];

		for (int i = 0; i<getInputRateLevels(); i++) {
			for (int j = 0; j<getInputRateLevels(); j++) {
				transitionsMatrix[i][j] = i == j? 1 : 0;
				pMatrix[i][j] = i == j? 1.0 : 0.0;
			}
		}

		this.unknownCostEst = new double[operator.getMaxParallelism()][getInputRateLevels()];

		//return new GuavaBasedQTable(0.0);
		return new ArrayBasedQTable(0.0, maxStateHash, maxActionHash);
	}

	@Override
	protected void registerMetrics(Statistics statistics) {
		super.registerMetrics(statistics);
	}

	@Override
	protected void learningStep(State oldState, Action action, State currentState, double reward) {
		++time;

		final State pds = StateUtils.computePostDecisionState(oldState, action, this);

		double unknownCost = reward - getwResources()*StateUtils.computeDeploymentCostNormalized(pds,this);
		if (action.getDelta()!=0)
			unknownCost -= getwReconf();
		unknownCost = unknownCost/getwSLO();

		/* Update transition model */
		final int iLambda = oldState.getLambda();
		final int jLambda = currentState.getLambda();
		transitionsMatrix[iLambda][jLambda] += 1;

		int totalTrans = 0;
		for (int endState = 0; endState < getInputRateLevels(); endState++) {
			totalTrans += transitionsMatrix[iLambda][endState];
		}
		for (int endState = 0; endState < getInputRateLevels(); endState++) {
			final double newP = transitionsMatrix[iLambda][endState]/(double)totalTrans;
			pMatrix[iLambda][endState] = newP;
		}

		/* Update cost estimate */
		final int k = currentState.overallParallelism();
		final double oldval = unknownCostEst[k-1][jLambda];
		final double newval = (1.0-alpha.getValue())*oldval + alpha.getValue() * unknownCost;
		//final double newval = (oldval * (offlineObservationsParam + time - 1) + unknownCost) / (offlineObservationsParam + time);
		//if (newval != newval2)
		//	logger.info("Diff: {} - {}", newval, newval2);
		unknownCostEst[k-1][jLambda] = newval;

		//int k0,k1;
		//int l0,l1;

		//if (newval > oldval) {
		//	k0 = 1;
		//	k1 = k;
		//	l0 = jLambda;
		//	l1 = currentState.getMaxLambda();
		//	for (int l = l0; l<=l1; ++l) {
		//		for (int _k = k0; _k<=k1; ++_k) {
		//			if (unknownCostEst[_k-1][l] < newval)
		//				unknownCostEst[_k-1][l] = newval;
		//		}
		//	}
		//} else if (newval < oldval) {
		//	k0 = k;
		//	k1 = currentState.getMaxParallelism();
		//	l0 = 0;
		//	l1 = jLambda;
		//	for (int l = l0; l<=l1; ++l) {
		//		for (int _k = k0; _k<=k1; ++_k) {
		//			if (unknownCostEst[_k-1][l] > newval)
		//				unknownCostEst[_k-1][l] = newval;
		//		}
		//	}
		//}

		//System.out.println("----------");
		//for (int _k = 1; _k<= operator.getMaxParallelism(); ++_k) {
		//	for (int l = 0; l < getInputRateLevels(); ++l) {
		//		System.out.print(String.format("%.2f\t",unknownCostEst[_k-1][l]));
		//	}
		//	System.out.println("");
		//}
		//System.out.println("----------");
		//for (int _l = 0; _l< getInputRateLevels(); ++_l) {
		//	for (int l = 0; l < getInputRateLevels(); ++l) {
		//		System.out.print(String.format("%.2f\t",pMatrix[_l][l]));
		//	}
		//	System.out.println("");
		//}
		//System.out.println("----------");

		decrementAlpha();

		/* Do a full backup */
		if (time < skipFullBackupAfter || time % fullBackupEvery == 0)
			this.fullBackup();

		if (time == 399000)
			dumpEstimatedCost();
			//dumpQOnFile("/tmp/fbq");
	}


	@Override
	public double evaluateAction(State s, Action a) {
		return qTable.getQ(s,a);
	}

	private double fullBackup() {
		double delta = 0.0;
		for (int iter = 0; iter < onlineVIMaxIter; ++iter) {
			delta = 0.0;

			StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
					ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

			while (stateIterator.hasNext()) {
				State state = stateIterator.next();
				double newDelta = computeValueIteration(state);
				delta = Math.max(delta, newDelta);
			}

			logger.info("Delta: " + delta);
			if (delta < 0.0001)
				break;
		}

		return delta;
	}

	protected double computeValueIteration(State state) {
		double delta = 0.0;

		ActionIterator actionIterator = new ActionIterator();

		while (actionIterator.hasNext()) {
			Action action = actionIterator.next();
			if (!validateAction(state, action))
				continue;

			double oldQ = qTable.getQ(state, action);
			double newQ = evaluateQ(state, action);
			qTable.setQ(state, action, newQ);

			delta = Math.max(delta, Math.abs(newQ - oldQ));
		}

		return delta;
	}

	private double evaluateQ(State s, Action a) {
		double cost = 0.0;
		// compute reconfiguration cost
		if (a.getDelta() != 0)
			cost += this.getwReconf();
		// from s,a compute pds
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// compute deployment cost using pds wighted on wRes
		cost += StateUtils.computeDeploymentCostNormalized(pds, this) * this.getwResources();

		for (int lambda = 0; lambda < pMatrix.length; lambda++) {
			// change pds.lambda to lambda
			pds.setLambda(lambda);
			// get Q(s, a) using the greedy action selection policy
			// from post decision state with lambda as pds.lambda
			Action greedyAction = getActionSelectionPolicy().selectAction(pds);
			double q = qTable.getQ(pds, greedyAction);
			// get transition probability from s.lambda to lambda level
			double p = pMatrix[s.getLambda()][lambda];
			// compute slo violation cost
			double pdCost = unknownCostEst[pds.overallParallelism()-1][lambda] * this.getwSLO();

			cost += p * (pdCost + gamma * q);
		}
		return cost;
	}

	protected void dumpQOnFile(String filename) {
		// create file
		File file = new File(filename);
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
			StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
					ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
			while (stateIterator.hasNext()) {
				State s = stateIterator.next();
				// print state line
				printWriter.println(s.dump());
				ActionIterator ait = new ActionIterator();
				while (ait.hasNext()) {
					Action a = ait.next();
					double v = this.qTable.getQ(s, a);
					if (s.validateAction(a)) {
						printWriter.print(String.format("%s\t%f\n", a.dump(), v));
					}
				}
			}
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpEstimatedCost () {
		System.out.println("----------");
		for (int _k = 1; _k<= operator.getMaxParallelism(); ++_k) {
			for (int l = 0; l < getInputRateLevels(); ++l) {
				System.out.print(String.format("%.2f\t",unknownCostEst[_k-1][l]));
			}
			System.out.println("");
		}
	}
}
