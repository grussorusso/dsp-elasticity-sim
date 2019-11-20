package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.HashMap;

public class GreedyActionSelectionPolicy extends ActionSelectionPolicy {

    public GreedyActionSelectionPolicy(ActionSelectionPolicyCallback aspCallback) {
        super(aspCallback);
    }

    @Override
    public Action selectAction(State s) {
        ActionIterator ait = new ActionIterator();
        Action newAction = null;
        double bestQ = 0.0;
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!this.aspCallback.actionValidation(s, a))
                continue;
            final double q = this.aspCallback.evaluateQ(s, a);

            if (newAction == null || q < bestQ) {
                bestQ = q;
                newAction = a;
            }
        }

        if (newAction != null)
            System.out.println(newAction.getIndex());
        return newAction;
    }
}
