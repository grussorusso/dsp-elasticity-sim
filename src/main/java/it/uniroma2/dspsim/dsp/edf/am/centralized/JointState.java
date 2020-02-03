package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

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
		return "JointState{" +
				"states=" + Arrays.toString(states) +
				'}';
	}

	public State[] getStates()
	{
		return states;
	}
}
