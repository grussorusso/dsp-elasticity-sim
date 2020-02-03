package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

public class JointActionIterator {

	private ActionIterator ai1;
	private Action a1;
	private ActionIterator ai2;
	private Action a2;

	public JointActionIterator(int nOperators)
	{
		if (nOperators != 2) {
			throw new RuntimeException("JointActionIterator only supports 2 operators");
		}

		ai1 = new ActionIterator();
		ai2 = new ActionIterator();
		a2 = ai2.next();
	}

	public boolean hasNext()
	{
		return ai1.hasNext() || ai2.hasNext();
	}


	public JointAction next()
	{
		if (!ai1.hasNext()) {
			ai1 = new ActionIterator();
			a2 = ai2.next();
		}

		a1 = ai1.next();

		return new JointAction (a1, a2);
	}


}
