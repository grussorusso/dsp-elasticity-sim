package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.sql.Array;
import java.util.Arrays;

public class JointState {

	protected State[] states;

	public JointState (State ...s)
	{
		this.states = s;
	}

	public boolean validateAction (JointAction a) {
		for (int i = 0; i<states.length; i++) {
			if (!states[i].validateAction(a.actions[i]))
				return false;
		}

		return true;
	}

	@Override
	public String toString() {
		//return "{" + Arrays.toString(states) + '}';
		StringBuilder sb = new StringBuilder();
		int lambdas[] = new int[states.length];
		for (int i = 0; i<states.length; i++)
			lambdas[i] = states[i].getLambda();

		sb.append("L=");
		sb.append(Arrays.toString(lambdas));
		sb.append(" k=");
		for (int j = 0; j<states.length; j++) {
			sb.append(Arrays.toString(states[j].getActualDeployment()));
		}

		return sb.toString();
	}

	public State[] getStates()
	{
		return states;
	}
}
