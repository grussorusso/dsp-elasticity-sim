package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.*;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.*;

public class EDF {

	private Application application;

	private ApplicationManager applicationManager;
	private Map<Operator, OperatorManager> operatorManagers;

	public EDF (Application application)
	{
		this.application = application;

		/* Create OMs */
		final List<Operator> operators = application.getOperators();
		final int numOperators = operators.size();

		Configuration conf = Configuration.getInstance();

		operatorManagers = new HashMap<>(numOperators);
		for (Operator op : operators) {
			operatorManagers.put(op, newOperatorManager(op, conf));
		}
	}

	protected OperatorManager newOperatorManager (Operator op, Configuration configuration) {
		OperatorManager om = null;

		// operator manager metadata
		// operator managers uses metadata to configure parameters
		// if a parameter isn't in metadata, operator manager will use inner default value
		HashMap<String, Object> omMetadata = new HashMap<>();

		final String omType = configuration.getString(ConfigurationKeys.OM_TYPE_KEY, "qlearning");

		// TODO clean code
		if (omType.equalsIgnoreCase("rl-qlearning") ||
				omType.equalsIgnoreCase("deep-qlearning")) {
			// action selection metadata
			HashMap<String, Object> aspMetadata = new HashMap<>();
			// add action selection policy to metadata
			final String aspType = configuration.getString(ConfigurationKeys.ASP_TYPE_KEY, "e-greedy");
			if (aspType.equalsIgnoreCase("random")) {
				//asp type
				omMetadata.put(ConfigurationKeys.ASP_TYPE_KEY, ActionSelectionPolicyType.RANDOM);
				// random asp seed
				aspMetadata.put(ConfigurationKeys.ASP_R_RANDOM_SEED_KEY,
						configuration.getLong(ConfigurationKeys.ASP_R_RANDOM_SEED_KEY, 1234L));
			} else if (aspType.equalsIgnoreCase("greedy")) {
				// asp type
				omMetadata.put(ConfigurationKeys.ASP_TYPE_KEY, ActionSelectionPolicyType.GREEDY);
			} else if (aspType.equalsIgnoreCase("e-greedy")) {
				// asp type
				omMetadata.put(ConfigurationKeys.ASP_TYPE_KEY, ActionSelectionPolicyType.EPSILON_GREEDY);
				// random asp seed
				aspMetadata.put(ConfigurationKeys.ASP_R_RANDOM_SEED_KEY,
						configuration.getLong(ConfigurationKeys.ASP_R_RANDOM_SEED_KEY, 1234L));
				// eg asp random seed
				aspMetadata.put(ConfigurationKeys.ASP_EG_RANDOM_SEED_KEY,
						configuration.getLong(ConfigurationKeys.ASP_EG_RANDOM_SEED_KEY, 1234L));
				// eg epsilon
				aspMetadata.put(ConfigurationKeys.ASP_EG_EPSILON_KEY,
						configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_KEY, 0.05));
				// eg epsilon decay
				aspMetadata.put(ConfigurationKeys.ASP_EG_EPSILON_DECAY_KEY,
						configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_DECAY_KEY, 0.9));
			} else {
				throw new IllegalArgumentException("Invalid aspType: " + aspType);
			}
			// asp metadata
			omMetadata.put(ConfigurationKeys.RL_OM_ASP_METADATA_KEY, aspMetadata);
			// reconfiguration weight
			omMetadata.put(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY,
					configuration.getDouble(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY, 0.33));
			// slo violation weight
			omMetadata.put(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY,
					configuration.getDouble(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY, 0.33));
			// resources cost weight
			omMetadata.put(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY,
					configuration.getDouble(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY, 0.33));
			// max input rate
			omMetadata.put(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY,
					configuration.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600));
			// input rate levels
			omMetadata.put(ConfigurationKeys.RL_OM_INPUT_RATE_LEVELS_KEY,
					configuration.getInteger(ConfigurationKeys.RL_OM_INPUT_RATE_LEVELS_KEY, 20));
		}

		if (omType.equalsIgnoreCase("threshold")) {
			om = new ThresholdBasedOM(op);
		} else if (omType.equalsIgnoreCase("qlearning")) {
			om = new QLearningOM(op);
		} else if (omType.equalsIgnoreCase("rl-qlearning")) {
			// add alpha
			omMetadata.put(ConfigurationKeys.QL_OM_ALPHA_KEY,
					configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.2));
			// add alpha decay
			omMetadata.put(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY,
					configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.9));
			om = new RLQLearningOM(op);
			for (String key : omMetadata.keySet()) {
				((RLQLearningOM) om).addMetadata(key, omMetadata.get(key));
			}
			((RLQLearningOM) om).configure();
		} else if (omType.equalsIgnoreCase("deep-qlearning")) {
			// add gamma
			omMetadata.put(ConfigurationKeys.DQL_OM_GAMMA_KEY,
					configuration.getDouble(ConfigurationKeys.DQL_OM_GAMMA_KEY, 0.9));
			// add gamma decay
			omMetadata.put(ConfigurationKeys.DQL_OM_GAMMA_DECAY_KEY,
					configuration.getDouble(ConfigurationKeys.DQL_OM_GAMMA_DECAY_KEY, 0.9));
			// add nd4j random seed
			omMetadata.put(ConfigurationKeys.DQL_OM_ND4j_RANDOM_SEED_KET,
					configuration.getLong(ConfigurationKeys.DQL_OM_ND4j_RANDOM_SEED_KET, 1234L));
			om = new DeepQLearningOM(op);
			for (String key : omMetadata.keySet()) {
				((DeepQLearningOM) om).addMetadata(key, omMetadata.get(key));
			}
			((DeepQLearningOM) om).configure();
		} else if (omType.equalsIgnoreCase("donothing")) {
			om = new DoNothingOM(op);
		} else {
			throw new RuntimeException("Invalid omType: " + omType);
		}

		return om;
	}

	public Map<Operator, Reconfiguration> pickReconfigurations (MonitoringInfo monitoringInfo) {
		Map<Operator, OMMonitoringInfo> omMonitoringInfo = new HashMap<>();
		for (Operator op : operatorManagers.keySet()) {
			omMonitoringInfo.put(op, new OMMonitoringInfo());
			omMonitoringInfo.get(op).setInputRate(0.0);
		}

		/* Compute operator input rate */
		boolean done = false;

		while (!done) {
			done = true;

			for (Operator src : application.getOperators()) {
				if (!src.isSource())
					continue;

				/* BFS visit */
				Set<Operator> checked = new HashSet<>();
				Deque<Operator> queue = new ArrayDeque<>();
				queue.push(src);

				while (!queue.isEmpty()) {
					Operator op = queue.pop();
					if (!checked.contains(op)) {
						double rate = 0.0;

						if (op.isSource())	{
							rate = monitoringInfo.getInputRate(); // TODO what if we have multiple sources?
						} else {
							for (Operator up : op.getUpstreamOperators()) {
								rate += omMonitoringInfo.get(up).getInputRate() * up.getSelectivity();
							}
						}

						double oldValue = omMonitoringInfo.get(op).getInputRate();
						done &= (oldValue == rate);

						omMonitoringInfo.get(op).setInputRate(rate);

						for (Operator down : op.getDownstreamOperators())  {
							queue.push(down);
						}

						checked.add(op);
					}
				}
			}
		}

		/* Add other monitoring information */
		for (Operator op : application.getOperators()) {
			OMMonitoringInfo opInfo = omMonitoringInfo.get(op);
			final double u = op.utilization(opInfo.getInputRate());
			opInfo.setCpuUtilization(u);
		}

		/* Let each OM make a decision. */
		Map<Operator, Reconfiguration> reconfigurations = new HashMap<>();
		for (Operator op : application.getOperators()) {
			Reconfiguration rcf = operatorManagers.get(op).pickReconfiguration(omMonitoringInfo.get(op));
			reconfigurations.put(op, rcf);
		}

		// TODO let the AM filter reconfigurations

		return reconfigurations;
	}

}
