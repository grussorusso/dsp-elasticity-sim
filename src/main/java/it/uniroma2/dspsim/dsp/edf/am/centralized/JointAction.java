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
		return "JointAction{" +
				"actions=" + Arrays.toString(actions) +
				'}';
	}

	public Action[] getActions() {
		return actions;
	}
}
