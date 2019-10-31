package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection;

import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.EpsilonGreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.GreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.RandomActionSelectionPolicy;

import java.util.HashMap;

public class ActionSelectionPolicyFactory {

    // avoid instantiation
    private ActionSelectionPolicyFactory() {
    }

    public static ActionSelectionPolicy getPolicy(ActionSelectionPolicyType type,
                                                  HashMap<String, Object> metadata,
                                                  ActionSelectionPolicyCallback aSCallback) {
        switch (type) {
            case RANDOM:
                return new RandomActionSelectionPolicy(metadata, aSCallback);
            case GREEDY:
                return new GreedyActionSelectionPolicy(metadata, aSCallback);
            case EPSILON_GREEDY:
                return new EpsilonGreedyActionSelectionPolicy(metadata, aSCallback);
            default:
                throw new IllegalArgumentException("Invalid action selection policy type");
        }
    }
}
