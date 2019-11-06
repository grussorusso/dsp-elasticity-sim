package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyComposition;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.HashMap;
import java.util.Random;

public class EpsilonGreedyActionSelectionPolicy extends ActionSelectionPolicyComposition {

    private double epsilon;
    private double epsilonDecay;
    private Random rng;

    public EpsilonGreedyActionSelectionPolicy(ActionSelectionPolicyCallback aspCallback) {
        super(aspCallback);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // add greedy action selection policy
        this.policies.add(ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                aspCallback
        ));

        // add random action selection policy
        this.policies.add(ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.RANDOM,
                aspCallback
        ));

        // TODO epsilon should change over time
        // init epsilon
        this.epsilon = configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_KEY, 0.05);

        // init epsilon decay
        this.epsilonDecay = configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_DECAY_KEY, 0.9);

        // init random number generator
        this.rng = new Random(configuration.getLong(ConfigurationKeys.ASP_EG_RANDOM_SEED_KEY, 1234L));
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
