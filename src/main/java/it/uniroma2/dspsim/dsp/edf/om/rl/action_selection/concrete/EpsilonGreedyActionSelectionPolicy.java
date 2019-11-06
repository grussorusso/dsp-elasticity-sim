package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyComposition;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.HashMap;
import java.util.Random;

public class EpsilonGreedyActionSelectionPolicy extends ActionSelectionPolicyComposition {

    private double epsilon; // TODO should change over time
    private double epsilonDecay;
    private Random rng;

    public EpsilonGreedyActionSelectionPolicy(ActionSelectionPolicyCallback aSCallback) {
        super(aSCallback);
        this.init(null, aSCallback);
    }

    public EpsilonGreedyActionSelectionPolicy(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        super(metadata, aSCallback);
        this.init(metadata, aSCallback);
    }

    private void init(HashMap<String, Object> metadata, ActionSelectionPolicyCallback aSCallback) {
        // add greedy action selection policy
        this.policies.add(ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                metadata,
                aSCallback
        ));
        // add random action selection policy
        this.policies.add(ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.RANDOM,
                metadata,
                aSCallback
        ));

        // init epsilon
        this.epsilon = this.getMetadata(ConfigurationKeys.ASP_EG_EPSILON_KEY, Double.class.getName());

        // init epsilon decay
        this.epsilonDecay = this.getMetadata(ConfigurationKeys.ASP_EG_EPSILON_DECAY_KEY, Double.class.getName());

        // init random number generator
        this.rng = new Random(this.getMetadata(ConfigurationKeys.ASP_EG_RANDOM_SEED_KEY, Long.class.getName()));
    }

    @Override
    public Action selectAction(State s) {
        if (this.rng.nextDouble() <= this.epsilon)
            // select random action
            return this.policies.get(1).selectAction(s);
        else
            // select greedy action
            return this.policies.get(0).selectAction(s);
    }
}
