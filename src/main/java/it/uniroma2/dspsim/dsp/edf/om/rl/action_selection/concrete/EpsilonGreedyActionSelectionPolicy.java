package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyComposition;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.HashMap;
import java.util.Random;

public class EpsilonGreedyActionSelectionPolicy extends ActionSelectionPolicyComposition {

    //TODO configuration
    private static final String EPSILON = "epsilon";
    private static final String RANDOM_SEED = "eg_rng_seed";

    private double epsilon; // TODO should change over time
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
        if (metadata != null && metadata.containsKey(EPSILON))
            if (metadata.get(EPSILON) instanceof Double)
                this.epsilon = (double) metadata.get(EPSILON);
            else
                throw new IllegalArgumentException("epsilon value must be double");
        else
            // TODO parametrize default epsilon
            this.epsilon = 0.05;

        // init random number generator
        if (metadata != null && metadata.containsKey(RANDOM_SEED))
            if (metadata.get(RANDOM_SEED) instanceof Integer)
                this.rng = new Random((int) metadata.get(RANDOM_SEED));
            else
                throw new IllegalArgumentException("random seed must be integer");
        else
            this.rng = new Random();
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
