package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.HashMap;

public class GreedyActionSelectionPolicy extends ActionSelectionPolicy {

    public GreedyActionSelectionPolicy(ActionSelectionPolicyCallback aSCallback) {
        super(aSCallback);
    }

    public GreedyActionSelectionPolicy(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        super(metadata, aSCallback);
    }

    @Override
    public Action selectAction(State s) {
        ActionIterator ait = new ActionIterator();
        Action newAction = null;
        double bestQ = 0.0;
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!this.aSCallback.actionValidation(s, a))
                continue;
            final double q = this.aSCallback.evaluateQ(s, a);

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
