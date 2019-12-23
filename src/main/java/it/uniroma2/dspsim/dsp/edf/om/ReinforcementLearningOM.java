package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.stats.Statistics;

public abstract class ReinforcementLearningOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    protected ReinforcementLearningOM(Operator operator) {
        super(operator);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        // action selection policy
        ActionSelectionPolicyType aspType = ActionSelectionPolicyType.fromString(
        Configuration.getInstance().getString(ConfigurationKeys.ASP_TYPE_KEY, "e-greedy"));
        return  ActionSelectionPolicyFactory.getPolicy(aspType, this);
    }

    @Override
    protected void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {
        // learning step
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
