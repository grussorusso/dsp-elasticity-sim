package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

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
			double pdCost = StateUtils.computeSLOCost(pds, this)*getwSLO();

			cost += p * (pdCost + getGamma() * q);
		}
		return cost;
	}

	private double evaluateQrespTime (State s, Action a) {
		final double MAX_Q_RESPTIME = 100000.0; // TODO
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
			if (Double.isInfinite(pdCost) || Double.isNaN(pdCost)) {
				pdCost = MAX_Q_RESPTIME;
			}

			cost += p * (pdCost + getGamma() * q);
		}
		return cost;
	}

	@Override
	protected OMRequest prepareOMRequest(State currentState, Action chosenAction) {
		/*
		 * Provide information to the AM.
		 */
		// TODO
//			actionScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, chosenAction);
//			Action nop = ActionIterator.getDoNothingAction();
//			if (nop.equals(chosenAction)) {
//				noReconfigurationScore = actionScore;
//			} else {
//				noReconfigurationScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, nop);
//			}

		// TODO
		return new OMRequest(action2reconfiguration(chosenAction), -1, -1);
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
			StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
					ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
			while (stateIterator.hasNext()) {
				State s = stateIterator.next();
				// print state line
				printWriter.println(s.dump());
				ActionIterator ait = new ActionIterator();
				while (ait.hasNext()) {
					Action a = ait.next();
					if (s.validateAction(a)) {
						double v = this.qTable.getQ(s, a);
						double qres = resourcesQ.getQ(s,a);
						double qrcf = reconfigurationQ.getQ(s,a);
						double qslo = sloQ.getQ(s,a);
						double qresptime = respTimeQ.getQ(s,a);
						printWriter.print(String.format("%s\t%f\t%f\t%f\t%f\t%f\tR=%f (avg %f)\n",
								a.dump(), v, qres, qrcf, qslo, qresptime,
								StateUtils.computeRespTime(StateUtils.computePostDecisionState(s,a,this),this),
								qresptime*(1-this.getGamma())
								));
					}
				}
			}
			printWriter.flush();
			printWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
