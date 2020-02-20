package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.ReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.SplitQReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.GreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class ValueIterationSplitQOM extends ValueIterationOM {

	private static Logger log = LoggerFactory.getLogger(ValueIterationSplitQOM.class);

	private QTable resourcesQ;
	private QTable reconfigurationQ;
	private QTable sloQ;

	// TODO: a VTable would we enough
	private QTable voidPolicyRespTimeQ;

	private QTable respTimeQ;

	private ActionSelectionPolicy freeReconfigurationActionSelection;

	double MAX_Q_RESPTIME; // TODO

	public ValueIterationSplitQOM(Operator operator) {
		super(operator);
		MAX_Q_RESPTIME = 10*operator.getSloRespTime(); // TODO

		freeReconfigurationActionSelection = new GreedyActionSelectionPolicy(new FreeReconfSelectionPolicyCallback());
	}

	@Override
	protected void buildQ() {
		super.buildQ();

		this.reconfigurationQ = new GuavaBasedQTable(0.0);
		this.resourcesQ = new GuavaBasedQTable(0.0);
		this.sloQ = new GuavaBasedQTable(0.0);

		this.respTimeQ = new GuavaBasedQTable(0.0);

		this.voidPolicyRespTimeQ = new GuavaBasedQTable(0.0);
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

			double newQ = getwReconf()*newQreconfiguration + getwResources()*newQresources + getwSLO()*newQslo;
			qTable.setQ(state, action, newQ);

			delta = Math.max(delta, Math.abs(newQ - oldQ));

			double newQrespTime = evaluateQrespTime(state, action);
			respTimeQ.setQ(state, action, newQrespTime);

			/* Void policy Q */
			Action nop = ActionIterator.getDoNothingAction();
			double respTimeQ = evaluateVoidPolicyQRespTime(state);
			voidPolicyRespTimeQ.setQ(state, nop, respTimeQ);

		}

		return delta;
	}

	private double evaluateVoidPolicyQRespTime(State s) {
		double cost = 0.0;

		/* we just need a copy of the state object */
		State pds = StateUtils.computePostDecisionState(s, ActionIterator.getDoNothingAction(), this);

		Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
		for (int lambda : possibleLambdas) {
			// change pds.lambda to lambda
			pds.setLambda(lambda); // this is now the next state
			Action greedyAction = ActionIterator.getDoNothingAction();
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


	// Note: this is not weighted
	private double evaluateQresources (State s, Action a) {
		double cost = 0.0;
		State pds = StateUtils.computePostDecisionState(s, a, this);
		// compute deployment cost using pds wighted on wRes
		cost += StateUtils.computeDeploymentCostNormalized(pds, this);
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

	// Note: this is not weighted
	private double evaluateQreconfiguration (State s, Action a) {
		double cost = 0.0;
		// compute reconfiguration cost
		if (a.getDelta() != 0)
			cost += 1.0;
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

	// Note: this is not weighted
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
		//StringBuilder sb = new StringBuilder();
		//sb.append(String.format("%s-R(%s,%s)", operator.getName(), s.dump(), a.dump()));

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
			//sb.append(String.format("+%.3f*[%.3f+gam*%.3f]", p, pdCost, q));
		}

		//log.info(sb.toString());

		return cost;
	}

	private ReconfigurationScore computeReconfigurationScore (State currentState, Action chosenAction)
	{
		double qRes = resourcesQ.getQ(currentState, chosenAction);
		double qRcf = reconfigurationQ.getQ(currentState, chosenAction);
		double qResp = respTimeQ.getQ(currentState, chosenAction);

		State pds = StateUtils.computePostDecisionState(currentState, chosenAction, this);
		double nextRespTime = StateUtils.computeRespTime(pds, this);
		if (Double.isInfinite(nextRespTime) || Double.isNaN(nextRespTime)) {
			nextRespTime = MAX_Q_RESPTIME;
		}
		double avgFutureRespTime = (qResp-nextRespTime)/getGamma()*(1.0-getGamma());
		return new SplitQReconfigurationScore(qRes,qRcf,nextRespTime,avgFutureRespTime);
	}

	@Override
	protected OMRequest prepareOMRequest(State currentState, Action chosenAction) {
		ReconfigurationScore score = computeReconfigurationScore(currentState, chosenAction);

		Action nop = ActionIterator.getDoNothingAction();
		Action secondaryAction = null;
		ReconfigurationScore nopScore;

		if (nop.equals(chosenAction)) { /* chosen is no reconf action */
			nopScore = score;
			// as fallback, propose the action which you'd choose
			// if the immediate rcf cost is 0 (in case someone else reconfigures)
			secondaryAction = freeReconfigurationActionSelection.selectAction(currentState);
			if (secondaryAction.equals(nop)) {
				secondaryAction = null;
			}
		} else {
			nopScore = computeReconfigurationScore(currentState, nop);
		}

		RewardBasedOMRequest request = new RewardBasedOMRequest(action2reconfiguration(chosenAction), score, nopScore);
		if (secondaryAction != null) {
			log.info("{} proposing a secondary action", operator.getName());
			ReconfigurationScore secondaryScore = computeReconfigurationScore(currentState, secondaryAction);
			request.addRequestedReconfiguration(action2reconfiguration(secondaryAction), secondaryScore);
		}

		return request;
	}

	private class FreeReconfSelectionPolicyCallback implements ActionSelectionPolicyCallback
	{
		@Override
		public boolean validateAction(State s, Action a) {
			return ValueIterationSplitQOM.this.validateAction(s,a);
		}

		@Override
		public double evaluateAction(State s, Action a) {
			double q = ValueIterationSplitQOM.this.evaluateAction(s,a);
			if (a.getDelta() != 0) {
				q -= getwReconf();
			}

			return q;
		}
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
				ActionIterator ait = new ActionIterator();
				while (ait.hasNext()) {
					Action a = ait.next();
					if (s.validateAction(a)) {
						double v = this.qTable.getQ(s, a);
						double qres = resourcesQ.getQ(s,a);
						double qrcf = reconfigurationQ.getQ(s,a);
						double qslo = sloQ.getQ(s,a);
						double qresptime = respTimeQ.getQ(s,a);
						State pds = StateUtils.computePostDecisionState(s, a, this);
						double nextRespTime = StateUtils.computeRespTime(pds, this);
						if (Double.isInfinite(nextRespTime) || Double.isNaN(nextRespTime)) {
							nextRespTime = MAX_Q_RESPTIME;
						}
						double avgFutureRespTime = (qresptime-nextRespTime)/getGamma()*(1.0-getGamma());
						double nextRespTimeVoid = StateUtils.computeRespTime(s, this);
						if (Double.isInfinite(nextRespTimeVoid) || Double.isNaN(nextRespTimeVoid)) {
							nextRespTimeVoid = MAX_Q_RESPTIME;
						}
						double avgFutureRespTimeVoid = (voidPolicyRespTimeQ.getQ(s,ActionIterator.getDoNothingAction())-nextRespTimeVoid)/getGamma()*(1.0-getGamma());
						printWriter.print(String.format("%s+%s\t%f\t%f\t%f\t%f\tR=%f+%f, %f+%f\n",
								s.dump(), a.dump(),
								v, qres, qrcf, qslo,
								nextRespTime, avgFutureRespTime,
								nextRespTimeVoid, avgFutureRespTimeVoid));
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
