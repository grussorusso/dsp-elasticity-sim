package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractState;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

public class QLearningOM extends OperatorManager {

	static public class State extends AbstractState {
		private int k[];
		private int lambda;

		public State (int[] k, int lambda) {
			this.k = k;
			this.lambda = lambda;
		}
	}

	static public class Action extends AbstractAction {
		private int delta;

		public Action (int delta) {
			this.delta = delta;
		}
	}

	private QTable qTable;
	private Action lastChosenAction = null;

	public QLearningOM(Operator operator) {
		super(operator);

		this.qTable = new GuavaBasedQTable(0.0);
	}

	protected int discretizeInputRate (double max, int levels, double inputRate)
	{
		final double quantum = max / levels;
		final int level = (int)Math.floor(inputRate/quantum);
		return level < levels? level : levels-1;
	}

	@Override
	public Reconfiguration pickReconfiguration(OMMonitoringInfo monitoringInfo) {
		final int deployment[] = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
		for (NodeType nt : operator.getInstances()) {
			deployment[nt.getIndex()] += 1;
		}
		final int inputRateLevel = discretizeInputRate(600, 20, monitoringInfo.getInputRate()); // TODO define constants
		State newState = new State(deployment, inputRateLevel);

		// TODO learning step here
		if (lastChosenAction != null) {
			// oldState, oldAction -> newState with reward to be computed
		}

		// TODO pick best action
		// for (Action : allActions) {
			// find action with minimum cost in current state
		//}
		// lastChosenAction = newAction;

		// TODO construct reconfiguration from Action
		// return action2reconfiguration(newAction);
		return null;
	}

}
