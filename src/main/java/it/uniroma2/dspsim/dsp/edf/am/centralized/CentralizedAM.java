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
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

	public CentralizedAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();

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

		if (this.nOperators != 2) {
			throw new RuntimeException("CentralizedAM currently supports 2 operators only");
		}
		if (application.getOperators().get(0).getSelectivity() > 1.01 ||
			application.getOperators().get(0).getSelectivity() < 0.99) {
			throw new RuntimeException("CentralizedAM currently does not support selectivity != 1");
		}

		this.maxParallelism = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
		}

		 String trainingInputRateFilePath = Configuration.getInstance()
				.getString(ConfigurationKeys.TRAINING_INPUT_FILE_PATH_KEY, "");

		try {
			this.pMatrix = DynamicProgrammingOM.buildPMatrix(trainingInputRateFilePath, this.maxInputRate, inputRateLevels);
		} catch (IOException e) {
			e.printStackTrace();
		}

		allocateQTable();
		computePolicy();
	}

	private void allocateQTable () {
		int maxAHash = -1;
		int maxSHash1 = -1;
		int maxSHash2 = -1;

		JointStateIterator it = new JointStateIterator(2, maxParallelism, ComputingInfrastructure.getInfrastructure(), inputRateLevels);

		while (it.hasNext()) {
			JointState s = it.next();
			maxSHash1 = Math.max(maxSHash1, s.getStates()[0].hashCode());
			maxSHash2 = Math.max(maxSHash2, s.getStates()[1].hashCode());
		}
		JointActionIterator ait = new JointActionIterator(2);
		while (ait.hasNext()) {
			JointAction a = ait.next();
			maxAHash = Math.max(maxAHash, a.getActions()[0].hashCode());
		}

		qTable = new JointQTable(0.0, maxSHash1, maxSHash2, maxAHash);
	}

	private void computePolicy() {
		double delta;

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
			System.err.println(delta);
		} while (delta > 0.0001);

		dumpQ();
	}

	private void dumpQ() {

		JointStateIterator sit = new JointStateIterator(nOperators, maxParallelism,
				ComputingInfrastructure.getInfrastructure(), inputRateLevels);
		while (sit.hasNext()) {
			JointState s = sit.next();
			if (s.states[0].getLambda() != s.states[1].getLambda()) // TODO
				continue;
			JointActionIterator ait = new JointActionIterator(nOperators);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (!s.validateAction(a))
					continue;
				double q = qTable.getQ(s,a);
				logger.info("Q({},{}) = {}", s, a, q);
			}
		}
	}

	private double updateQ(JointState s, JointAction a) {
		/* immediate cost */
		double crcf = 0.0;
		if (a.isReconfiguration())
			crcf = 1.0;

		JointState newS = computePDS(s,a);
		double cres = computeNormalizedResourcesCost(newS);

		double cslo = 0.0;
		double futureCost = 0.0;

		for (int lambda = 0; lambda < inputRateLevels; ++lambda) {
			for (int i = 0; i < nOperators; i++) {
				newS.states[i].setLambda(lambda);
			}

			// NOTE: We are assuming lambdas are proportional among different operators...
			double p = pMatrix.getValue(s.states[0].getLambda(), newS.states[0].getLambda());

			if(isAppSLOViolated(newS)) {
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

	private JointAction greedyAction(JointState s) {
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

	private boolean isAppSLOViolated(JointState newS) {
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

	private double computeNormalizedResourcesCost (JointState s) {
		double cost = 0.0;
		for (int i = 0; i < nOperators; i++) {
			cost += StateUtils.computeDeploymentCostNormalized(s.states[i], maxParallelism[i]);
		}
		return cost/nOperators;
	}

	private JointState computePDS(JointState s, JointAction a) {
		State pds1 = StateUtils.computePostDecisionState(s.states[0], a.actions[0], StateType.K_LAMBDA, inputRateLevels, maxParallelism[0]);
		State pds2 = StateUtils.computePostDecisionState(s.states[1], a.actions[1], StateType.K_LAMBDA, inputRateLevels, maxParallelism[1]);
		return new JointState(pds1, pds2);
	}

	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {

		// Compute current state
		Operator op1 = application.getOperators().get(0);
		Operator op2 = application.getOperators().get(1);
		State s1 = StateUtils.computeCurrentState(omMonitoringInfo.get(op1), op1, maxInputRate, inputRateLevels, StateType.K_LAMBDA);
		State s2 = StateUtils.computeCurrentState(omMonitoringInfo.get(op2), op2, maxInputRate, inputRateLevels, StateType.K_LAMBDA);
		JointState currentState = new JointState(s1,s2);

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
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap) {
		throw new RuntimeException("This method should never be called!");
	}
}
