package it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyComposition;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.HashMap;
import java.util.Random;

public class EpsilonGreedyActionSelectionPolicy extends ActionSelectionPolicyComposition {

    private double epsilon;
    private double epsilonDecay;
    private double epsilonMinValue;
    private int epsilonDecaySteps;
    private int epsilonDecayStepsCounter;

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

        // init epsilon
        this.epsilon = configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_KEY, 1.0);

        // init epsilon decay
        this.epsilonDecay = configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_DECAY_KEY, 0.9);

        // init epsilon min value
        this.epsilonMinValue = configuration.getDouble(ConfigurationKeys.ASP_EG_EPSILON_MIN_VALUE_KEY, 0.01);

        // init epsilon decay steps numbers
        this.epsilonDecaySteps = configuration.getInteger(ConfigurationKeys.ASP_EG_EPSILON_DECAY_STEPS_KEY, -1);

        // init epsilon decay steps counter
        this.epsilonDecayStepsCounter = 0;

        // init random number generator
        this.rng = new Random();
        this.setSeed(configuration.getLong(ConfigurationKeys.EPSGREEDY_SEED, 5234L));
    }

    @Override
    public Action selectAction(State s) {
        Action chosenAction;
        if (this.rng.nextDouble() <= this.epsilon) {
            // select random action
            chosenAction = this.policies.get(1).selectAction(s);
        } else {
            // select greedy action
            chosenAction = this.policies.get(0).selectAction(s);
        }

        // decrement epsilon if necessary
        decrementEpsilon();


        return chosenAction;
    }

    private void decrementEpsilon() {
        if (this.epsilonDecaySteps > 0) {
            this.epsilonDecayStepsCounter++;
            if (this.epsilonDecayStepsCounter >= this.epsilonDecaySteps) {
                if (this.epsilon >= this.epsilonMinValue) {
                    this.epsilonDecayStepsCounter = 0;
                    this.epsilon = this.epsilonDecay * this.epsilon;
                    if (this.epsilon < this.epsilonMinValue) {
                        this.epsilon = this.epsilonMinValue;
                    }
                }
            }
        }
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public void setEpsilonDecay(double epsilonDecay) {
        this.epsilonDecay = epsilonDecay;
    }

    public void setEpsilonDecaySteps(int epsilonDecaySteps) {
        this.epsilonDecaySteps = epsilonDecaySteps;
    }

    public void setEpsilonDecayStepsCounter(int epsilonDecayStepsCounter) {
        this.epsilonDecayStepsCounter = epsilonDecayStepsCounter;
    }

    public void setSeed (long seed) {
        this.rng.setSeed(seed);

        int i = 1;
        for (ActionSelectionPolicy asp : this.policies) {
            if (asp instanceof RandomActionSelectionPolicy) {
                RandomActionSelectionPolicy rp = (RandomActionSelectionPolicy) asp;
                ((RandomActionSelectionPolicy) asp).setSeed(seed+i);
                i++;
            }
        }
    }
}
