package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.om.DynamicProgrammingOM;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import static it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM.action2reconfiguration;


/**
 * Experimental AM.
 * Adaptation is controlled in a centralized way,
 * requests from any OM are ignored.
 */
public class CentralizedAM extends ApplicationManager {

	static private final Logger logger = LoggerFactory.getLogger(CentralizedAM.class);

	private int nOperators;
	private int maxParallelism[];

	private int maxInputRate;
	private int inputRateLevels;

	private double wReconf;
	private double wSLO;
	private double wResources;

	private double gamma;
	private DoubleMatrix<Integer, Integer> pMatrix;
	private JointQTable qTable;
	private Operator[] operators;

	public CentralizedAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();
		this.operators = application.getOperators().toArray(new Operator[]{});

		Configuration configuration = Configuration.getInstance();

		// input rate discretization
		this.maxInputRate = configuration.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);
		this.inputRateLevels = configuration.getInteger(ConfigurationKeys.RL_OM_INPUT_RATE_LEVELS_KEY, 20);

		// reward weights
		this.wReconf = configuration.getDouble(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY, 0.33);
		this.wSLO = configuration.getDouble(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY, 0.33);
		this.wResources = configuration.getDouble(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY, 0.33);
		this.gamma = Configuration.getInstance().getDouble(ConfigurationKeys.DP_GAMMA_KEY, 0.99);

		logger.info("Using wRcf={}, wRes={}, wSLO={}, gamma={}", wReconf, wResources, wSLO, gamma);

		for (Operator op : application.getOperators()) {
			if (op.getSelectivity() > 1.01 || op.getSelectivity() < 0.99) {
				throw new RuntimeException("CentralizedAM currently does not support selectivity != 1");
			}
		}

		this.maxParallelism = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
		}

		 String trainingInputRateFilePath = configuration.getString(ConfigurationKeys.TRAINING_INPUT_FILE_PATH_KEY, "");

		try {
			this.pMatrix = DynamicProgrammingOM.buildPMatrix(trainingInputRateFilePath, this.maxInputRate, inputRateLevels);
		} catch (IOException e) {
			e.printStackTrace();
		}

		final String outputBasePath = Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, "");
		String qTableFilename = configuration.getString(ConfigurationKeys.AM_CENTRALIZED_PRECOMPUTED_QTABLE_FILE, "");
		if (qTableFilename == null || qTableFilename.isEmpty()) {
			this.qTable = JointQTable.createQTable(nOperators, maxParallelism, inputRateLevels);
			computePolicy();
			serializeQ(String.format("%s/centralizedQtable.ser", outputBasePath));
		} else {
			try {
				this.qTable = loadQTable(qTableFilename);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		dumpQ(String.format("%s/centralizedQtable", outputBasePath));
		dumpPolicy(String.format("%s/policy", outputBasePath));
	}

	private JointQTable loadQTable(String qTableFilename) throws IOException, ClassNotFoundException {
		FileInputStream file;
		file = new FileInputStream(qTableFilename);
		ObjectInputStream in = new ObjectInputStream(file);

		// TODO: check if the state space is consistent
		JointQTable table = (JointQTable)in.readObject();

		return table;
	}

	private void computePolicy() {
		double delta;
		long iter = 0;

		do {
			delta = 0.0;
			JointStateIterator sit = new JointStateIterator(nOperators, maxParallelism,
					ComputingInfrastructure.getInfrastructure(), inputRateLevels);
			while (sit.hasNext()) {
				JointState s = sit.next();

				JointActionIterator ait = new JointActionIterator(nOperators);
				while (ait.hasNext()) {
					JointAction a = ait.next();
					if (!s.validateAction(a))
						continue;
					double _delta = updateQ(s,a);
					delta = Math.max(delta, _delta);
				}
			}
			iter++;
			if (iter % 25 == 0)
				System.err.println(delta);
		} while (delta > 0.0001);
	}

	private double updateQ(JointState s, JointAction a) {
		/* immediate cost */
		double crcf = 0.0;
		if (a.isReconfiguration())
			crcf = 1.0;

		JointState newS = JointStateUtils.computePDS(s,a,inputRateLevels,maxParallelism);
		double cres = JointStateUtils.computeNormalizedResourcesCost(newS, maxParallelism);

		double cslo = 0.0;
		double futureCost = 0.0;

		Map<Operator, int[]> opDeployment = new HashMap<>();
		for (int i = 0; i<nOperators; i++) {
			opDeployment.put(operators[i], newS.states[i].getActualDeployment());
		}


		for (int lambda = 0; lambda < inputRateLevels; ++lambda) {
			// compute per operator lambda...
			newS.states[0].setLambda(lambda);
			double realInputRate = MathUtils.remapDiscretizedValue(maxInputRate, lambda, inputRateLevels);
			Map<Operator, Double> opRealInputRate = application.computePerOperatorInputRate(realInputRate, opDeployment);
			for (int i = 1; i < nOperators; i++) {
				int lambdaOp = MathUtils.discretizeValue(maxInputRate, opRealInputRate.get(operators[i]), inputRateLevels);
				newS.states[i].setLambda(lambdaOp);
			}

			// NOTE: We are assuming lambdas are proportional among different operators...
			double p = pMatrix.getValue(s.states[0].getLambda(), newS.states[0].getLambda());

			if(isAppSLOViolationExpectedInState(newS)) {
				cslo += p;
			}

			JointAction bestAinNewS = greedyAction(newS);
			futureCost += p*qTable.getQ(newS, bestAinNewS);
		}

		//logger.info("Q({},{}): {} + {} + {} + g*{}", s, a, cslo, crcf, cres, futureCost);
		double q = wSLO*cslo+wReconf*crcf+wResources*cres + gamma*futureCost;

		double oldQ = qTable.getQ(s, a);
		qTable.setQ(s,a,q);

		return Math.abs(oldQ-q);
	}

	public JointAction greedyAction(JointState s) {
		JointAction bestA = null;
		Double bestQ = null;

		JointActionIterator iterator = new JointActionIterator(nOperators);
		while (iterator.hasNext()) {
			JointAction a = iterator.next();
			if (!s.validateAction(a))
				continue;
			double q = qTable.getQ(s,a);
			if (bestQ == null || q < bestQ) {
				bestA = a;
				bestQ = q;
			}
		}

		return bestA;
	}

	public double computeSLOViolationProbability (JointState s, JointAction a) {
		JointState newS = JointStateUtils.computePDS(s,a,inputRateLevels,maxParallelism);

		Map<Operator, int[]> opDeployment = new HashMap<>();
		for (int i = 0; i<nOperators; i++) {
			opDeployment.put(operators[i], newS.states[i].getActualDeployment());
		}

		double prob = 0.0;
		for (int lambda = 0; lambda < inputRateLevels; ++lambda) {
			// compute per operator lambda...
			newS.states[0].setLambda(lambda);
			double realInputRate = MathUtils.remapDiscretizedValue(maxInputRate, lambda, inputRateLevels);
			Map<Operator, Double> opRealInputRate = application.computePerOperatorInputRate(realInputRate, opDeployment);
			for (int i = 1; i < nOperators; i++) {
				int lambdaOp = MathUtils.discretizeValue(maxInputRate, opRealInputRate.get(operators[i]), inputRateLevels);
				newS.states[i].setLambda(lambdaOp);
			}

			// NOTE: We are assuming lambdas are proportional among different operators...
			double p = pMatrix.getValue(s.states[0].getLambda(), newS.states[0].getLambda());

			if(isAppSLOViolationExpectedInState(newS)) {
				prob += p;
			}
		}

		return prob;
	}

	public boolean isAppSLOViolationExpectedInState(JointState newS) {
		Map<Operator, Double> opRespTime = new HashMap<>();

		for (int i = 0; i < application.getOperators().size(); i++) {
			State opS = newS.states[i];
			Operator op = application.getOperators().get(i);
			double r = StateUtils.computeRespTime(opS, op, maxInputRate, inputRateLevels);
			opRespTime.put(op, r);
		}

		//logger.info("RespTime in {}: {}", newS, opRespTime);

		return isAppSLOViolated(opRespTime);
	}

	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {
		JointState currentState = JointStateUtils.computeCurrentState(application, omMonitoringInfo, maxInputRate, inputRateLevels);

		logger.info("Expected violation: " + isAppSLOViolationExpectedInState(currentState));

		// Pick best global action
		JointAction a = greedyAction(currentState);

		// Build map op->reconf based on global action
		Map<Operator, Reconfiguration> opReconfs = new HashMap<>(nOperators);
		for (int i = 0; i<nOperators; i++) {
			Reconfiguration rcf = action2reconfiguration(a.actions[i]);
			opReconfs.put(application.getOperators().get(i), rcf);
		}

		return opReconfs;
	}

	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														Map<Operator, OMMonitoringInfo> omMonitoringInfoMap) {
		throw new RuntimeException("This method should never be called!");
	}

	private void serializeQ (String filename) {

		FileOutputStream file = null;
		try {
			file = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(file);
			out.writeObject(qTable);
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void dumpQ(String filename) {

		File file = new File(filename);
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
			JointStateIterator sit = new JointStateIterator(nOperators, maxParallelism,
					ComputingInfrastructure.getInfrastructure(), inputRateLevels);
			while (sit.hasNext()) {
				JointState s = sit.next();

				JointActionIterator ait = new JointActionIterator(nOperators);
				while (ait.hasNext()) {
					JointAction a = ait.next();
					if (!s.validateAction(a))
						continue;
					double q = qTable.getQ(s,a);
					printWriter.println(String.format("Q(%s,%s) = %f", s.toString(), a.toString(), q));
				}
			}
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void dumpPolicy (String filename) {

		File file = new File(filename);
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
			JointStateIterator sit = new JointStateIterator(nOperators, maxParallelism,
					ComputingInfrastructure.getInfrastructure(), inputRateLevels);
			while (sit.hasNext()) {
				JointState s = sit.next();
				JointAction a = greedyAction(s);
				printWriter.println(String.format("%s -> %s", s.toString(), a.toString()));
			}
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
