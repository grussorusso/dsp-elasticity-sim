package it.uniroma2.dspsim;

/**
 * Abbreviations:
 * 	- ASP : Action Selection Policy
 * 	- OM : Operator Manager
 * 	- RL : Reinforcement Learning
 * 	- SLO : Service Level Objective
 * 	- R : Random
 * 	- EG : Epsilon Greedy
 * 	- DQL : Deep Q-Learning
 */
public abstract class ConfigurationKeys {

	/**
	 * Test base path
	 */
	public static final String OUTPUT_BASE_PATH_KEY = "output.base.path";
	public static final String OUTPUT_LOG_FILENAME = "output.log";

	/**
	 * Data source.
	 */
	public static final String INPUT_FILE_PATH_KEY = "input.file.path";

	/** File used to train dynamic programming oms **/
	public static final String TRAINING_INPUT_FILE_PATH_KEY = "input.file.path.training";

	/**
	 * Simulation.
	 */
	public static final String SIMULATION_STOP_TIME = "simulation.stoptime";
	public static final String SIMULATION_DETAILED_SCALING_LOG = "simulation.log.detailedscaling";


	/**
	 * Node types number.
	 */
	public static final String NODE_TYPES_NUMBER_KEY = "node.types.number";

	/**
	 * Operator.
	 */
	/** Max Parallelism **/
	public static final String OPERATOR_MAX_PARALLELISM_KEY = "operator.max.parallelism";
	/** Speedup considered while computing response time or utilization **/
	public static final String OPERATOR_VALUES_COMPUTING_CASE_KEY = "operator.values.computing.case";
	/** Load Balancer**/
	public static final String OPERATOR_LOAD_BALANCER_TYPE_KEY = "operator.load.balancer.type";
	/** Method for computing per-operator SLO. */
	public static final String OPERATOR_SLO_COMPUTATION_METHOD = "dsp.slo.operator.method";
	public static final String OPERATOR_SLO_COMPUTATION_CUSTOM_QUOTAS = "dsp.slo.operator.custom.quotas";


	/**
	 * Application.
	 */
	/** Application topology to use. */
	public static final String APPLICATION = "dsp.app.type";
	/** File describing the application (if 'fromfile' app is used) */
	public static final String APPLICATION_FILE_SPEC = "dsp.app.file";
	/** Current SLO in terms of application latency (seconds). */
	public static final String SLO_LATENCY_KEY = "dsp.slo.latency";

	/**
	 * Type of ApplicationManager to use.
	 */
	public static final String AM_TYPE_KEY = "edf.am.type";

	public static final String AM_CENTRALIZED_PRECOMPUTED_QTABLE_FILE = "edf.am.centralized.qfilename";

	/**
	 * Type of OperatorManager to use.
	 */
	public static final String OM_TYPE_KEY = "edf.om.type";

	/**
	 * Threshold based operator manager params.
	 */
	/** For ThresholdBasedOM, scaling threshold in (0,1). */
	public static final String OM_THRESHOLD_KEY = "edf.om.threshold";

	/**
	 * Reinforcement learning operator manager params.
	 */
	/** Reconfiguration cost weight. */
	public static final String RL_OM_RECONFIG_WEIGHT_KEY = "edf.rl.om.reconfiguration.weight";
	/** SLO violation weight. */
	public static final String RL_OM_SLO_WEIGHT_KEY = "edf.rl.om.slo.weight";
	/** Operator's deployment cost weight */
	public static final String RL_OM_RESOURCES_WEIGHT_KEY = "edf.rl.om.resources.weight";
	/** Max input rate considered to discretize lambda */
	public static final String RL_OM_MAX_INPUT_RATE_KEY = "edf.rl.om.max.input.rate";
	/** Lambda levels */
	public static final String RL_OM_INPUT_RATE_LEVELS_KEY = "edf.rl.om.input.rate.levels";
	/** State representation */
	public static final String RL_OM_STATE_REPRESENTATION_KEY = "edf.rl.om.state.representation";

	/**
	 * Dynamic Programming params
	 */
	public static final String DP_GAMMA_KEY = "edf.dp.gamma";

	/**
	 * VI operator manager params
	 */
	public static final String VI_MAX_ITERATIONS_KEY = "edf.vi.max.iterations";
	public static final String VI_MAX_TIME_SECONDS_KEY = "edf.vi.max.time.seconds";
	public static final String VI_THETA_KEY = "edf.vi.theta";
	public static final String VI_APPROX_MODEL = "edf.vi.approximate";
	public static final String VI_APPROX_MODEL_SEED = "edf.vi.approximate.seed";
	public static final String VI_APPROX_MODEL_MAX_ERR = "edf.vi.approximate.maxerr";
	public static final String VI_APPROX_MODEL_MIN_ERR = "edf.vi.approximate.minerr";

	/**
	 * TBVI operator manager params
	 */
	public static final String TBVI_EXEC_ITERATIONS_KEY = "edf.tbvi.exec.iterations";
	public static final String TBVI_EXEC_SECONDS_KEY = "edf.tbvi.exec.seconds";
	public static final String TBVI_TRAJECTORY_LENGTH_KEY = "edf.tbvi.trajectory.length";
	/** Function Approximation **/
	public static final String TBVI_FA_ALPHA_KEY = "edf.tbvi.fa.alpha";

	/**
	 * Model-based OM.
	 */
	public static final String MB_INIT_WITH_VI = "edf.mb.initvi.enabled";
	public static final String MB_INIT_VI_APPROX = "edf.mb.initvi.approximate";
	public static final String MB_APPROX_MODEL_SEED = "edf.mb.initvi.approximate.seed";
	public static final String MB_SKIP_ITER_AFTER = "edf.mb.skipafter";
	public static final String MB_REDUCED_ITER_PERIOD = "edf.mb.reducedperiod";
	public static final String MB_MAX_ONLINE_ITERS = "edf.mb.maxiters";

	/**
	 * Q-learning operator manager params.
	 */
	public static final String QL_OM_ALPHA_KEY = "edf.ql.om.alpha";
	public static final String QL_OM_ALPHA_DECAY_KEY = "edf.ql.om.alpha.decay";
	public static final String QL_OM_ALPHA_MIN_VALUE_KEY = "edf.ql.om.alpha.min.value";
	public static final String QL_OM_ALPHA_DECAY_STEPS_KEY = "edf.ql.om.alpha.decay.steps";

	/**
	 * Deep-learning operator manager params.
	 */
	public static final String DL_OM_ND4j_RANDOM_SEED_KET = "edf.dl.om.nd4j.random.seed";
	public static final String DL_OM_ENABLE_NETWORK_UI_KEY = "edf.dl.om.enable.network.ui";
	public static final String DL_OM_SAMPLES_MEMORY_SIZE_KEY = "edf.dl.samples.memory.size";
	public static final String DL_OM_SAMPLES_MEMORY_BATCH_KEY = "edf.dl.samples.memory.batch";
	public static final String DL_OM_FIT_EVERY_ITERS = "edf.dl.fit.every";
	public static final String DL_OM_NETWORK_CACHE_SIZE = "edf.dl.network.cache.size";

	/**
	 * Type of ActionSelectionPolicy (ASP) to use
	 * (available if OperatorManager is extension of ReinforcementLearningOM).
	 */
	public static final String ASP_TYPE_KEY = "asp.type";

	/**
	 * Epsilon greedy (EG) ASP params.
	 */
	/** epsilon param. */
	public static final String ASP_EG_EPSILON_KEY = "asp.eg.epsilon";
	/** epsilon decay param. */
	public static final String ASP_EG_EPSILON_DECAY_KEY = "asp.eg.epsilon.decay";
	public static final String ASP_EG_EPSILON_MIN_VALUE_KEY = "asp.eg.epsilon.min.value";
	public static final String ASP_EG_EPSILON_DECAY_STEPS_KEY = "asp.eg.om.epsilon.decay.steps";
	public static final String EPSGREEDY_SEED = "asp.eg.seed";
}
