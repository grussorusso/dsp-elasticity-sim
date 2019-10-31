package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.GreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.ArrayList;
import java.util.Random;

public class RLQLearningOM extends ReinforcementLearningOM {
    private QTable qTable;

    private double alpha;

    private ActionSelectionPolicy greedyActionSelection;

    public RLQLearningOM(Operator operator) {
        super(operator);

        this.qTable = new GuavaBasedQTable(0.0);

        this.alpha = 0.2; // TODO should change over time

        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                null,
                this
        );
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha) * oldQ +
                alpha * (reward + qTable.getQ(currentState, greedyActionSelection.selectAction(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);
        System.out.println(bellmanError); // TODO create a statistic

        qTable.setQ(oldState, action, newQ);
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateQ(State s, Action a) {
        return qTable.getQ(s, a);
    }
}
