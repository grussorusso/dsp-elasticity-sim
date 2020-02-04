package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;

import java.util.Arrays;

public class JointAction {

	protected Action[] actions;

	public JointAction(Action... actions) {
		this.actions = actions;
	}

	@Override
	public String toString() {
		return "{" +
				Arrays.toString(actions) +
				'}';
	}

	public Action[] getActions() {
		return actions;
	}

	public boolean isReconfiguration() {
		for (Action a : actions) {
			if (a.getDelta() != 0) { // TODO: migration?
				return true;
			}
		}

		return false;
	}
}
