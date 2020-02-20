package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;

import java.util.Map;

public class JointStateUtils {

	public static JointState computeCurrentState (Application application, Map<Operator, OMMonitoringInfo> omMonitoringInfo,
										   int maxInputRate, int inputRateLevels)
	{
		final int nOperators = application.getOperators().size();
		State s[] = new State[nOperators];
		for (int i = 0; i<nOperators; i++) {
			Operator op = application.getOperators().get(i);
			s[i] = StateUtils.computeCurrentState(omMonitoringInfo.get(op), op, maxInputRate, inputRateLevels, StateType.K_LAMBDA);
		}
		JointState currentState = new JointState(s);

		return currentState;
	}

	static public double computeNormalizedResourcesCost (JointState s, int maxParallelism[]) {
		double cost = 0.0;
		for (int i = 0; i < s.states.length; i++) {
			cost += StateUtils.computeDeploymentCostNormalized(s.states[i], maxParallelism[i]);
		}
		return cost/maxParallelism.length;
	}

	static public JointState computePDS(JointState s, JointAction a, int inputRateLevels, int maxParallelism[]) {
		State pds[] = new State[s.states.length];
		for (int i = 0; i<s.states.length; i++)
			pds[i] = StateUtils.computePostDecisionState(s.states[i], a.actions[i],
					StateType.K_LAMBDA, inputRateLevels, maxParallelism[i]);

		return new JointState(pds);
	}

}
