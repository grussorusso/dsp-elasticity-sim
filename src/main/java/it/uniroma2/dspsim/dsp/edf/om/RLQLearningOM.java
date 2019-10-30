package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.ArrayList;
import java.util.Random;

public class RLQLearningOM extends ReinforcementLearningOM {
    private QTable qTable;

    private Random actionSelectionRng = new Random();

    private double epsilon;
    private double alpha;

    public RLQLearningOM(Operator operator) {
        super(operator);

        this.qTable = new GuavaBasedQTable(0.0);

        this.epsilon = 0.05; // TODO should change over time
        this.alpha = 0.2; // TODO should change over time
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha) * oldQ +
                alpha * (reward + qTable.getQ(currentState, greedyActionSelection(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);
        System.out.println(bellmanError); // TODO create a statistic

        qTable.setQ(oldState, action, newQ);
    }

    @Override
    protected Action pickNewAction(State state) {
        return this.epsilonGreedyActionSelection(state, this.epsilon);
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
            if (!a.isValidInState(s, this.operator))
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
            if (!a.isValidInState(s, this.operator))
                continue;
            final double q = qTable.getQ(s, a);

            if (newAction == null || q < bestQ) {
                bestQ = q;
                newAction = a;
            }
        }

        return newAction;
    }
}
