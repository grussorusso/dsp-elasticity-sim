package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.EpsilonGreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public abstract class ReinforcementLearningOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    private boolean enabledLearning;

    protected ReinforcementLearningOM(Operator operator) {
        super(operator);
        this.enabledLearning = Configuration.getInstance().getBoolean(ConfigurationKeys.OM_ENABLE_LEARNING, true);
    }


    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        // action selection policy
        ActionSelectionPolicyType aspType = ActionSelectionPolicyType.fromString(
        Configuration.getInstance().getString(ConfigurationKeys.ASP_TYPE_KEY, "e-greedy"));


        ActionSelectionPolicy asp =  ActionSelectionPolicyFactory.getPolicy(aspType, this);
        if (asp instanceof EpsilonGreedyActionSelectionPolicy)  {
            EpsilonGreedyActionSelectionPolicy egp = (EpsilonGreedyActionSelectionPolicy) asp;
            int baseSeed = Configuration.getInstance().getInteger(ConfigurationKeys.EPSGREEDY_SEED, 5234);
            egp.setSeed(baseSeed + this.operator.getName().hashCode());
        }
        return asp;
    }

    @Override
    protected void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {
        // learning step
        if (enabledLearning)
            learningStep(lastState, lastChosenAction, currentState, reward);
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public boolean validateAction(State s, Action a) {
        return s.validateAction(a);
    }

    /**
     * ABSTRACT METHODS
     */
    protected abstract void learningStep(State oldState, Action action, State currentState, double reward);
}
