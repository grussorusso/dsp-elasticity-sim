package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

public class QTableFactory {

	private QTableFactory() {}

	public static QTable newQTable(int maxParallelism, int lambdaLevels)
	{
		String impl = Configuration.getInstance().getString(ConfigurationKeys.RL_DEFAULT_QTABLE_IMPL, "map");
		if (impl.equalsIgnoreCase("array"))
			return newArrayBasedQTable(maxParallelism, lambdaLevels);
		else
			return newMapBasedQTable();
	}

	public static MapBasedQTable newMapBasedQTable ()
	{
		return new MapBasedQTable(0.0);
	}

	public static ArrayBasedQTable newArrayBasedQTable (int maxParallelism, int lambdaLevels)
	{
		long states = 0;

		int maxActionHash = -1;
		int maxStateHash = -1;
		StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, maxParallelism,
				ComputingInfrastructure.getInfrastructure(), lambdaLevels);
		while (stateIterator.hasNext()) {
			State state = stateIterator.next();
			states++;
			int h = state.hashCode();
			if (h  > maxStateHash)
				maxStateHash = h;
			ActionIterator actionIterator = new ActionIterator();
			while (actionIterator.hasNext()) {
				Action action = actionIterator.next();
				h = action.hashCode();
				if (h > maxActionHash)
					maxActionHash = h;
			}
		}

		System.out.printf("States = %d\n", states);

		return new ArrayBasedQTable(0.0, maxStateHash, maxActionHash);
	}
}
