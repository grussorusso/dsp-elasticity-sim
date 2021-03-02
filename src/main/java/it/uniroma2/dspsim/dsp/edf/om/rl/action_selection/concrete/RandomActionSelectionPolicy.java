package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.ArrayList;
import java.util.Random;

public class RandomActionSelectionPolicy extends ActionSelectionPolicy {

    private Random rng;

    public RandomActionSelectionPolicy(ActionSelectionPolicyCallback aspCallback) {
        super(aspCallback);

        this.rng = new Random();
    }

    @Override
    public Action selectAction(State s) {
        ArrayList<Action> actions = new ArrayList<>();
        ActionIterator ait = new ActionIterator();
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!aspCallback.validateAction(s, a))
                continue;
            actions.add(a);
        }

        return actions.get(this.rng.nextInt(actions.size()));
    }

    public void setSeed(long seed) {
        this.rng.setSeed(seed);
    }
}
