package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractState;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

public class QLearningOM extends OperatorManager {

	public class State extends AbstractState {
		private int k[];
		private int lambda;

		public State (int[] k, int lambda) {
			this.k = k;
			this.lambda = lambda;
		}
		
		public int overallParallelism() {
			int p = 0;
			for (int _k : k) {
				p += _k;
			}
			
			return p;
		}
	}

	public class Action extends AbstractAction {
		private int delta;
		private int resTypeIndex;

		public Action (int delta, int resTypeIndex) {
			this.delta = delta;
			this.resTypeIndex = resTypeIndex;
		}
		
		public boolean isValidInState (State s) {
			if (delta == 0)
				return true;
			if (delta < 0) {
				if (s.k[resTypeIndex] < delta)
					return false;
			}
			
			return s.overallParallelism() + delta >= 1 && s.overallParallelism() + delta <= QLearningOM.super.operator.getMaxParallelism();
		}
	}

	private class ActionIterator implements Iterator<Action> {

		/* We iterate over actions in this order:
		 * (do nothing action), (-1, 0), (-1, 1), ...
		 * (+1, 0), (+1, 1...)
		 */
		private int delta = 0;
		private int resTypeIndex = 0;
		boolean first = true;

		@Override
		public boolean hasNext() {
			return delta < 1 || resTypeIndex < (ComputingInfrastructure.getInfrastructure().getNodeTypes().length-1);
		}

		@Override
		public Action next() {
			if (first) {
				first = false;
				return new Action(0, 0);
			}

			if (!hasNext())
				return null;

			++resTypeIndex;
			if (resTypeIndex >= (ComputingInfrastructure.getInfrastructure().getNodeTypes().length)) {
				resTypeIndex = 0;

				/* Next delta */
				if (delta == 0)
					delta = -1;
				else
					delta = 1;
			}

			return new Action(delta, resTypeIndex);
		}
	}

	private QTable qTable;
	private Action lastChosenAction = null;
	private State lastState = null;
	private Random actionSelectionRng  = new Random();

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
		final int inputRateLevel = discretizeInputRate(600, 20, monitoringInfo.getInputRate()); // TODO define new configuration params
		State currentState = new State(deployment, inputRateLevel);

		// TODO learning step here
		if (lastChosenAction != null) {
			final double reward = computeCost(lastChosenAction, currentState, monitoringInfo.getInputRate());

			final double ALPHA = 0.2; // TODO should change over time
			final double oldQ  = qTable.getQ(lastState, lastChosenAction);
			final double newQ = (1.0-ALPHA)*oldQ + ALPHA*(reward + qTable.getQ(currentState, greedyActionSelection(currentState)));

			final double bellmanError = Math.abs(newQ-oldQ);
			System.out.println(bellmanError); // TODO create a statistic

			qTable.setQ(lastState, lastChosenAction, newQ);
		}

		//  pick best action
		lastChosenAction = epsilonGreedyActionSelection(currentState, 0.05);// TODO configurable param, varying over time?
		lastState = currentState;

		 return action2reconfiguration(lastChosenAction);
	}

	private Reconfiguration action2reconfiguration(Action a) {
		if (a.delta == 0)
			return Reconfiguration.doNothing();

		if (a.delta == 1)  {
			return Reconfiguration.scaleOut(ComputingInfrastructure.getInfrastructure().getNodeTypes()[a.resTypeIndex]);
		}

		if (a.delta == -1) {
			return Reconfiguration.scaleIn(ComputingInfrastructure.getInfrastructure().getNodeTypes()[a.resTypeIndex]);
		}

		throw new RuntimeException("Unsupported action!");
	}

	private Action epsilonGreedyActionSelection (State s, double epsilon) {
		if (actionSelectionRng.nextDouble() <= epsilon)
			return randomActionSelection(s);
		else
			return greedyActionSelection(s);
	}

	private Action randomActionSelection(State s) {
		ArrayList<Action> actions = new ArrayList<>();
		ActionIterator ait = new ActionIterator();
		while (ait.hasNext()) {
			final Action a = ait.next();
			if (!a.isValidInState(s))
				continue;
			actions.add(a);
		}

		return actions.get(actionSelectionRng.nextInt(actions.size()));
	}


	private Action greedyActionSelection (State s) {
		ActionIterator ait = new ActionIterator();
		Action newAction = null;
		double bestQ = 0.0;
		while (ait.hasNext()) {
			final Action a = ait.next();
			if (!a.isValidInState(s))
				continue;
			final double q = qTable.getQ(s, a);

			if (newAction == null || q < bestQ) {
				bestQ = q;
				newAction = a;
			}
		}

		return newAction;
	}

	private double computeCost (Action a, State newState, double newInputRate) {
		final double wReconf = 0.33; // TODO config param
		final double wSLO = 0.33;// TODO config param
		final double wResources = 0.33;// TODO config param

		double cost = 0.0;

		if (a.delta != 0)
			cost += wReconf;

		/* TODO Given the application latency SLO,
		   we should give the OperatorManager a per-operator SLO
		   at the beginning (e.g., each operator gets at most 1/n of the application SLO,
		   where n is the number of operators on a source-sink path.
		*/
		final double OPERATOR_SLO = 0.1;

		if (operator.responseTime(newInputRate) > OPERATOR_SLO)
			cost += wSLO;

		cost += operator.computeNormalizedDeploymentCost()*wResources;

		return cost;
	}
}
