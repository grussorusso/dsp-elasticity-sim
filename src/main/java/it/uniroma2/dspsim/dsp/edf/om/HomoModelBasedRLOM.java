package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.QBasedReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.rl.*;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.KLambdaState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;
import org.checkerframework.checker.units.qual.K;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Random;

public class HomoModelBasedRLOM extends ReinforcementLearningOM {

	private QTable qTable;
	private VTable estimatedCost;

	private int[][] transitionsMatrix;
	private double[][] pMatrix;

	private double gamma;

	private int APPROXIMATION_SEED;
	double maxErr;
	double minErr;

	private VariableParameter alpha;
	private int alphaDecaySteps;
	private int alphaDecayStepsCounter;

	/**
	 * FullBackup every time step while t <= skipFullBackupAfter.
	 * Then, full backup every fullBackupInterval.
	 * fullBackupInterval <- interval * updateCoeff every time
	 */
	private int skipFullBackupAfter;
	private double fullBackupInterval;
	private int nextFullBackupTime = 1;
	private double fullBackupIntervalUpdateCoeff;
	private int onlineVIMaxIter;
	private  ComputingInfrastructure homoInfra;

	private int updatedStateActions = 0;

	private int time = 0;
	private Logger logger = LoggerFactory.getLogger(HomoModelBasedRLOM.class);


	private static final int SINGLE_ITERATION_HARD_TIMEOUT_SEC = 30;

	public HomoModelBasedRLOM(Operator operator) {
		super(operator);

		// get configuration instance
		Configuration conf = Configuration.getInstance();

		this.qTable = buildQ();
		this.gamma = conf.getDouble(ConfigurationKeys.DP_GAMMA_KEY,0.99);

		double alphaInitValue = conf.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.1);
		double alphaDecay = conf.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 1.0);
		double alphaMinValue = conf.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

		this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);
		this.alphaDecaySteps = conf.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
		this.alphaDecayStepsCounter = 0;

		logger.info("Alpha conf: init={}, decay={}, currValue={}", alphaInitValue,
				alphaDecay, alpha.getValue());

		this.skipFullBackupAfter = conf.getInteger(ConfigurationKeys.MB_SKIP_ITER_AFTER, 100);
		this.fullBackupInterval = conf.getInteger(ConfigurationKeys.MB_REDUCED_ITER_PERIOD, 50);
		this.fullBackupIntervalUpdateCoeff = conf.getDouble(ConfigurationKeys.MB_REDUCED_PERIOD_UPDATE_COEFF, 1.0);

		this.onlineVIMaxIter = conf.getInteger(ConfigurationKeys.MB_MAX_ONLINE_ITERS, 1);
		this.APPROXIMATION_SEED = conf.getInteger(ConfigurationKeys.VI_APPROX_MODEL_SEED, 123);
		this.maxErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MAX_ERR, 0.1);
		this.minErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MIN_ERR, 0.05);

		homoInfra = ComputingInfrastructure.getInfrastructure().cloneWithSingleNodeType(0); // TODO

	}

	private Operator approximateOperatorModel (Operator op)
	{
		Random r = new Random(APPROXIMATION_SEED);

		OperatorQueueModel queueModel = op.getQueueModel().getApproximateModel(r, maxErr, minErr);
		logger.info("Approximate stMean: {} -> {}", op.getQueueModel().getServiceTimeMean(), queueModel.getServiceTimeMean());
		Operator tempOperator = new Operator("temp", queueModel, op.getMaxParallelism());
		tempOperator.setSloRespTime(op.getSloRespTime());
		return tempOperator;
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
		this.transitionsMatrix = new int[getInputRateLevels()][getInputRateLevels()];
		this.pMatrix = new double[getInputRateLevels()][getInputRateLevels()];

		for (int i = 0; i<getInputRateLevels(); i++) {
			for (int j = 0; j<getInputRateLevels(); j++) {
				transitionsMatrix[i][j] = i == j? 1 : 0;
				pMatrix[i][j] = i == j? 1.0 : 0.0;
			}
		}

		this.estimatedCost = VTableFactory.newVTable(operator.getMaxParallelism(), getInputRateLevels());
		QTable q = QTableFactory.newQTable(operator.getMaxParallelism(), getInputRateLevels());

		if (PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
			q.load(PolicyIOUtils.getFileForLoading(this.operator, "qTable"));
			estimatedCost.load(PolicyIOUtils.getFileForLoading(this.operator, "cost"));
			loadProbabilityMatrix(PolicyIOUtils.getFileForLoading(this.operator, "matrix"));
		}

		return q;
	}

	private void dumpProbabilityMatrix (File f) {
		try {
			FileOutputStream fileOut = new FileOutputStream(f.getAbsolutePath());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.pMatrix);
			out.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void loadProbabilityMatrix (File f) {
		try {
			FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
			ObjectInputStream in = new ObjectInputStream(fileIn);
			this.pMatrix = (double[][])  in.readObject();
			in.close();
			fileIn.close();

		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}

	@Override
	public void savePolicy()
	{
		this.qTable.dump(PolicyIOUtils.getFileForDumping(this.operator, "qTable"));
		this.estimatedCost.dump(PolicyIOUtils.getFileForDumping(this.operator, "cost"));
		dumpProbabilityMatrix(PolicyIOUtils.getFileForDumping(this.operator, "matrix"));
	}

	@Override
	protected void registerMetrics(Statistics statistics) {
		super.registerMetrics(statistics);
	}

	@Override
	protected void learningStep(State oldState, Action action, State currentState, double reward) {
		++time;

		oldState = homogeneousState(oldState);
		currentState = homogeneousState(currentState);

		final State pds = StateUtils.computePostDecisionState(oldState, action, this);

		double unknownCost = reward - getwResources() * StateUtils.computeDeploymentCostNormalized(pds, this);
		if (action.getDelta() != 0)
			unknownCost -= getwReconf();
		unknownCost = unknownCost / getwSLO();

		/* Update transition model */
		final int iLambda = oldState.getLambda();
		final int jLambda = currentState.getLambda();
		transitionsMatrix[iLambda][jLambda] += 1;

		int totalTrans = 0;
		for (int endState = 0; endState < getInputRateLevels(); endState++) {
			totalTrans += transitionsMatrix[iLambda][endState];
		}
		for (int endState = 0; endState < getInputRateLevels(); endState++) {
			final double newP = transitionsMatrix[iLambda][endState] / (double) totalTrans;
			pMatrix[iLambda][endState] = newP;
		}

		/* Update cost estimate */
		final double oldval = estimatedCost.getV(currentState);
		final double newval = (1.0 - alpha.getValue()) * oldval + alpha.getValue() * unknownCost;
		//final double newval = (oldval * (offlineObservationsParam + time - 1) + unknownCost) / (offlineObservationsParam + time);
		//if (newval != newval2)
		//	logger.info("Diff: {} - {}", newval, newval2);
		estimatedCost.setV(currentState, newval);

		int l0,l1;

		State s = currentState;
		if (newval > oldval) {
			l0 = jLambda;
			l1 = currentState.getMaxLambda();
			for (int l = l0+1; l<=l1; ++l) {
				s.setLambda(l);
				if (estimatedCost.getV(s) < newval)
					estimatedCost.setV(s, newval);
			}
		} else if (newval < oldval) {
			l0 = 0;
			l1 = jLambda;
			for (int l = l0; l<l1; ++l) {
				s.setLambda(l);
				if (estimatedCost.getV(s) > newval)
					estimatedCost.setV(s, newval);
			}
		}
		decrementAlpha();

		/* Do a full backup */
		if (time == nextFullBackupTime) {
			this.fullBackup();
			if (time < skipFullBackupAfter) {
				this.nextFullBackupTime++;
			} else {
				this.nextFullBackupTime += Math.ceil(fullBackupInterval);
				this.fullBackupInterval = Math.min(this.fullBackupInterval * this.fullBackupIntervalUpdateCoeff, 50000);
			}

		}
	}

	private State homogeneousState(State oldState) {
		int k = oldState.overallParallelism();
		int l = oldState.getLambda();
		return new KLambdaState(0, new int[]{k}, l, oldState.getMaxLambda(), oldState.getMaxParallelism());
	}


	@Override
	public double evaluateAction(State s, Action a) {
		if (a.getResTypeIndex() > 0)
			return 9999;
		return qTable.getQ(s,a);
	}

	private double fullBackup() {
		long _t0 = System.currentTimeMillis();


		double delta = 0.0;
		for (int iter = 0; iter < onlineVIMaxIter; ++iter) {
			delta = 0.0;

			StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
					homoInfra, this.getInputRateLevels());

			while (stateIterator.hasNext()) {
				State state = stateIterator.next();
				double newDelta = computeValueIteration(state);
				delta = Math.max(delta, newDelta);

				if (System.currentTimeMillis() - _t0 > 1000.0*SINGLE_ITERATION_HARD_TIMEOUT_SEC)
					throw new RuntimeException("ModelBased single iteration taking more than SINGLE_ITERATION_HARD_TIMEOUT_SEC");
			}

			logger.info("Delta: " + delta);
			if (delta < 0.0001)
				break;
		}

		this.trainingEpochsCount.update(updatedStateActions);
		updatedStateActions = 0;
		this.planningTimeMetric.update((int)(System.currentTimeMillis() - _t0));

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
			++updatedStateActions;
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
			double pdCost = estimatedCost.getV(pds) * this.getwSLO();

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
		StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
				ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
		int lastLambda = -1;
		while (stateIterator.hasNext()) {
			State s = stateIterator.next();
			if (lastLambda != s.getLambda())
				System.out.println("");
			System.out.print(String.format("%.2f\t", estimatedCost.getV(s)));
			lastLambda = s.getLambda();
		}
	}

	@Override
	protected OMRequest prepareOMRequest(State currentState, Action chosenAction) {
		/*
		 * Provide information to the AM.
		 */
		double actionScore = 0.0;
		double noReconfigurationScore = 0.0;
		if (this instanceof ActionSelectionPolicyCallback) {
			/* V(pds) */
			actionScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, chosenAction);
			Action nop = ActionIterator.getDoNothingAction();
			if (nop.equals(chosenAction)) {
				noReconfigurationScore = actionScore;
			} else {
				noReconfigurationScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, nop);
			}
		}

		return new RewardBasedOMRequest(action2reconfigurationHomo(chosenAction),
				new QBasedReconfigurationScore(actionScore), new QBasedReconfigurationScore(noReconfigurationScore));
	}

	private Reconfiguration action2reconfigurationHomo(Action action) {
		int delta = action.getDelta();

		if (delta > 1 || delta < -1) throw new RuntimeException("Unsupported action!");

		if (delta == 0) return Reconfiguration.doNothing();

		return delta > 0 ?
				Reconfiguration.scaleOut(homoInfra.getNodeTypes()[0]):
				Reconfiguration.scaleIn(homoInfra.getNodeTypes()[0]);
	}
}
