package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.EpsilonGreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.GreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.RandomActionSelectionPolicy;

import java.util.HashMap;

public class ActionSelectionPolicyFactory {

    // avoid instantiation
    private ActionSelectionPolicyFactory() {
    }

    public static ActionSelectionPolicy getPolicy(ActionSelectionPolicyType type,
                                                  ActionSelectionPolicyCallback aspCallback) {
        switch (type) {
            case RANDOM:
                return new RandomActionSelectionPolicy(aspCallback);
            case GREEDY:
                return new GreedyActionSelectionPolicy(aspCallback);
            case EPSILON_GREEDY:
                return new EpsilonGreedyActionSelectionPolicy(aspCallback);
            default:
                throw new IllegalArgumentException("Invalid action selection policy type");
        }
    }
}
