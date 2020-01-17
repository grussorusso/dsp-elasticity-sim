package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;

import java.util.Set;

public class ValueIterationSplitQOM extends ValueIterationOM {

	private QTable resourcesQ;
	private QTable reconfigurationQ;
	private QTable sloQ;

	public ValueIterationSplitQOM(Operator operator) {
		super(operator);
	}

	@Override
	protected void buildQ() {
		super.buildQ();

		this.reconfigurationQ = new GuavaBasedQTable(0.0);
		this.resourcesQ = new GuavaBasedQTable(0.0);
		this.sloQ = new GuavaBasedQTable(0.0);
	}

		@Override
	protected double computeValueIteration(State state) {
		double delta = 0.0;

		ActionIterator actionIterator = new ActionIterator();

		while (actionIterator.hasNext()) {
			Action action = actionIterator.next();
			if (!validateAction(state, action))
				continue;

			double oldQ = qTable.getQ(state, action);
			double newQresources = evaluateQresources(state, action);
			resourcesQ.setQ(state, action, newQresources);
			double newQreconfiguration = evaluateQreconfiguration(state, action);
			reconfigurationQ.setQ(state, action, newQreconfiguration);
			double newQslo = evaluateQslo(state, action);
			sloQ.setQ(state, action, newQslo);

			double newQ = newQreconfiguration + newQresources + newQslo;
			qTable.setQ(state, action, newQ);

			delta = Math.max(delta, Math.abs(newQ - oldQ));
		}

		return delta;
	}

	private double evaluateQresources (State s, Action a) {
		double cost = 0.0;
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// compute deployment cost using pds wighted on wRes
		cost += StateUtils.computeDeploymentCostNormalized(pds, this) * this.getwResources();
		// for each lambda level with p != 0 in s.getLambda() row
		Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
		for (int lambda : possibleLambdas) {
			// change pds.lambda to lambda
			pds.setLambda(lambda);
			// get Q(s, a) using the greedy action selection policy
			// from post decision state with lambda as pds.lambda
			Action greedyAction = getActionSelectionPolicy().selectAction(pds);
			double q = resourcesQ.getQ(pds, greedyAction);
			// get transition probability from s.lambda to lambda level
			double p = getpMatrix().getValue(s.getLambda(), lambda);

			cost += p * getGamma() * q;
		}
		return cost;
	}

	private double evaluateQreconfiguration (State s, Action a) {
		double cost = 0.0;
		// compute reconfiguration cost
		if (a.getDelta() != 0)
			cost += this.getwReconf();
		// from s,a compute pds
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// for each lambda level with p != 0 in s.getLambda() row
		Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
		for (int lambda : possibleLambdas) {
			// change pds.lambda to lambda
			pds.setLambda(lambda);
			// get Q(s, a) using the greedy action selection policy
			// from post decision state with lambda as pds.lambda
			Action greedyAction = getActionSelectionPolicy().selectAction(pds);
			double q = reconfigurationQ.getQ(pds, greedyAction);
			// get transition probability from s.lambda to lambda level
			double p = getpMatrix().getValue(s.getLambda(), lambda);

			cost += p * getGamma() * q;
		}
		return cost;
	}

	private double evaluateQslo (State s, Action a) {
		double cost = 0.0;
		// from s,a compute pds
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// for each lambda level with p != 0 in s.getLambda() row
		Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
		for (int lambda : possibleLambdas) {
			// change pds.lambda to lambda
			pds.setLambda(lambda);
			// get Q(s, a) using the greedy action selection policy
			// from post decision state with lambda as pds.lambda
			Action greedyAction = getActionSelectionPolicy().selectAction(pds);
			double q = sloQ.getQ(pds, greedyAction);
			// get transition probability from s.lambda to lambda level
			double p = getpMatrix().getValue(s.getLambda(), lambda);
			// compute slo violation cost
			double pdCost = StateUtils.computeSLOCost(pds, this);

			cost += p * (pdCost + getGamma() * q);
		}
		return cost;
	}
}
