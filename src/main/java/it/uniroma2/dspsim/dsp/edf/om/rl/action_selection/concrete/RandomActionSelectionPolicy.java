package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class RandomActionSelectionPolicy extends ActionSelectionPolicy {

    private Random rng;

    public RandomActionSelectionPolicy(ActionSelectionPolicyCallback aSCallback) {
        super(aSCallback);

        this.rng = new Random();
    }

    public RandomActionSelectionPolicy(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        super(metadata, aSCallback);

        this.rng = new Random(this.getMetadata(ConfigurationKeys.ASP_R_RANDOM_SEED_KEY, Long.class.getName()));
    }

    @Override
    public Action selectAction(State s) {
        ArrayList<Action> actions = new ArrayList<>();
        ActionIterator ait = new ActionIterator();
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!aSCallback.actionValidation(s, a))
                continue;
            actions.add(a);
        }

        return actions.get(this.rng.nextInt(actions.size()));
    }
}
