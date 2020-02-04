package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;

public class JointActionIterator {

	private ActionIterator[] iterators;
	private Action[] actions;

	public JointActionIterator(int nOperators)
	{
		iterators = new ActionIterator[nOperators];
		actions = new Action[nOperators];

		for (int i = 0; i<nOperators; i++) {
			iterators[i] = new ActionIterator();
		}
		for (int i = 1; i<nOperators; i++) {
			// 0 is skipped
			actions[i] = iterators[i].next();
		}
	}

	private void resetIterator (int i) {
		iterators[i] = new ActionIterator();
		actions[i] = iterators[i].next();
	}

	public boolean hasNext()
	{
		for (ActionIterator i : iterators) {
			if (i.hasNext())
				return true;
		}
		return false;
	}


	public JointAction next()
	{
		int i = 0;
		boolean done = false;

		while (!done && i < actions.length) {
			if (iterators[i].hasNext()) {
				actions[i] = iterators[i].next();
				done = true;
			} else {
				resetIterator(i);
				i++;
			}
		}

		return new JointAction (actions.clone());
	}


}
