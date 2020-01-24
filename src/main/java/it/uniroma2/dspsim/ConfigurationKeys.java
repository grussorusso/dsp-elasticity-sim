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

	/**
	 * Data source.
	 */
	public static final String INPUT_FILE_PATH_KEY = "input.file.path";

	/** File used to train dynamic programming oms **/
	public static final String TRAINING_INPUT_FILE_PATH_KEY = "input.file.path.training";

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
	public static final String OPERATOR_SPEEDUP_CASE_CONSIDERED_KEY = "operator.speedup.case.considered";
	/** Load Balancer**/
	public static final String OPERATOR_LOAD_BALANCER_TYPE_KEY = "operator.load.balancer.type";

	/**
	 * Constraints.
	 */
	/** Current SLO in terms of application latency (seconds). */
	public static final String SLO_LATENCY_KEY = "dsp.slo.latency";

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
	 * Q-learning operator manager params.
	 */
	public static final String QL_OM_ALPHA_KEY = "edf.ql.om.alpha";
	public static final String QL_OM_ALPHA_DECAY_KEY = "edf.ql.om.alpha.decay";
	public static final String QL_OM_ALPHA_MIN_VALUE_KEY = "edf.ql.om.alpha.min.value";
	public static final String QL_OM_ALPHA_DECAY_STEPS_KEY = "edf.ql.om.alpha.decay.steps";

	/**
	 * Deep-learning operator manager params.
	 */
	public static final String DL_OM_GAMMA_KEY = "edf.dl.om.gamma";
	public static final String DL_OM_GAMMA_DECAY_KEY = "edf.dl.om.gamma.decay";
	public static final String DL_OM_GAMMA_MIN_VALUE_KEY = "edf.dl.om.gamma.min.value";
	public static final String DL_OM_GAMMA_DECAY_STEPS_KEY = "edf.dl.om.gamma.decay.steps";
	public static final String DL_OM_ND4j_RANDOM_SEED_KET = "edf.dl.om.nd4j.random.seed";
	public static final String DL_OM_ENABLE_NETWORK_UI_KEY = "edf.dl.om.enable.network.ui";

	/**
	 * Type of ActionSelectionPolicy (ASP) to use
	 * (available if OperatorManager is extension of ReinforcementLearningOM).
	 */
	public static final String ASP_TYPE_KEY = "asp.type";

	/**
	 * Random (R) ASP params.
	 */
	/** random action selection seed */
	public static final String ASP_R_RANDOM_SEED_KEY = "asp.r.random.seed";

	/**
	 * Epsilon greedy (EG) ASP params.
	 */
	/** epsilon param. */
	public static final String ASP_EG_EPSILON_KEY = "asp.eg.epsilon";
	/** epsilon decay param. */
	public static final String ASP_EG_EPSILON_DECAY_KEY = "asp.eg.epsilon.decay";
	public static final String ASP_EG_EPSILON_MIN_VALUE_KEY = "asp.eg.epsilon.min.value";
	public static final String ASP_EG_EPSILON_DECAY_STEPS_KEY = "asp.eg.om.epsilon.decay.steps";
	/** epsilon random action selection seed. */
	public static final String ASP_EG_RANDOM_SEED_KEY = "asp.eg.epsilon.random.seed";
}
