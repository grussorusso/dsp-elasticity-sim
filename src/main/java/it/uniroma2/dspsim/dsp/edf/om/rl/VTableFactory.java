package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

public class VTableFactory {

	private VTableFactory() {}

	public static VTable newVTable(int maxParallelism, int lambdaLevels)
	{
		String impl = Configuration.getInstance().getString(ConfigurationKeys.RL_DEFAULT_QTABLE_IMPL, "map");
		if (impl.equalsIgnoreCase("array"))
			return newArrayBasedVTable(maxParallelism, lambdaLevels);
		else
			return newMapBasedVTable();
	}

	public static MapBasedVTable newMapBasedVTable ()
	{
		return new MapBasedVTable(0.0);
	}

	public static ArrayBasedVTable newArrayBasedVTable (int maxParallelism, int lambdaLevels)
	{
		int maxStateHash = -1;
		StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, maxParallelism,
				ComputingInfrastructure.getInfrastructure(), lambdaLevels);
		while (stateIterator.hasNext()) {
			State state = stateIterator.next();
			int h = state.hashCode();
			if (h  > maxStateHash)
				maxStateHash = h;
		}

		return new ArrayBasedVTable(0.0, maxStateHash);
	}
}
