package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class ValueIterationSplitQOM extends ValueIterationOM {

	private QTable resourcesQ;
	private QTable reconfigurationQ;
	private QTable sloQ;

	// TODO: a VTable would we enough
	private QTable respTimeQ;

	public ValueIterationSplitQOM(Operator operator) {
		super(operator);
	}

	@Override
	protected void buildQ() {
		super.buildQ();

		this.reconfigurationQ = new GuavaBasedQTable(0.0);
		this.resourcesQ = new GuavaBasedQTable(0.0);
		this.sloQ = new GuavaBasedQTable(0.0);

		this.respTimeQ = new GuavaBasedQTable(0.0);
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


			double newQrespTime = evaluateQrespTime(state, action);
			respTimeQ.setQ(state, action, newQrespTime);
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

	private double evaluateQrespTime (State s, Action a) {
		double cost = 0.0;
		// from s,a compute pds
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// for each lambda level with p != 0 in s.getLambda() row
		Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
		for (int lambda : possibleLambdas) {
			// change pds.lambda to lambda
			pds.setLambda(lambda); // this is now the next state
			// get Q(s, a) using the greedy action selection policy
			// from post decision state with lambda as pds.lambda
			Action greedyAction = getActionSelectionPolicy().selectAction(pds);
			double q = respTimeQ.getQ(pds, greedyAction);
			// get transition probability from s.lambda to lambda level
			double p = getpMatrix().getValue(s.getLambda(), lambda);

			double pdCost = StateUtils.computeRespTime(pds, this);

			cost += p * (pdCost + getGamma() * q);
		}
		return cost;
	}

	/**
	 * Dump policy on file
	 */
	@Override
	protected void dumpQOnFile(String filename) {
		// create file
		File file = new File(filename);
		try {
			if (!file.exists()) {
				file.getParentFile().mkdirs();
				file.createNewFile();
			}
			PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));

			dumpQOnFile(printWriter, this.qTable);
			dumpQOnFile(printWriter, this.resourcesQ);
			dumpQOnFile(printWriter, this.reconfigurationQ);
			dumpQOnFile(printWriter, this.sloQ);
			dumpQOnFile(printWriter, this.respTimeQ);

			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
