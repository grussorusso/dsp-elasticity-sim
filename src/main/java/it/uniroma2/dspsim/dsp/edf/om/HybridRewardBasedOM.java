package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public abstract class HybridRewardBasedOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    private BaseTBValueIterationOM offLineOM;
    private ReinforcementLearningOM onLineOM;

    public HybridRewardBasedOM(Operator operator) {
        super(operator);

        this.offLineOM = buildOffLineOM(operator);
        this.onLineOM = buildOnLineOM(operator);

        transferKnowledge();

        // recall init action selection policy
        // to overwrite initial action selection policy with deep-v-learning configured one
        initActionSelectionPolicy();
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        if (this.onLineOM == null) {
            return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        } else {
            return this.onLineOM.getActionSelectionPolicy();
        }
    }

    @Override
    protected void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {
        // delegate reward collection to deep-v-learning om
        this.onLineOM.useReward(reward, lastState, lastChosenAction, currentState, monitoringInfo);
    }

    @Override
    public boolean validateAction(State s, Action a) {
        if (this.onLineOM == null) {
            return s.validateAction(a);
        } else {
            return this.onLineOM.validateAction(s, a);
        }
    }

    @Override
    public double evaluateAction(State s, Action a) {
        if (this.onLineOM == null) {
            if (this.offLineOM == null ) {
                return 0.0;
            } else {
                return this.offLineOM.evaluateAction(s, a);
            }
        } else {
            return this.onLineOM.evaluateAction(s, a);
        }
    }

    /**
     * ABSTRACT METHODS
     */

    protected abstract BaseTBValueIterationOM buildOffLineOM(Operator operator);
    protected abstract ReinforcementLearningOM buildOnLineOM(Operator operator);

    /**
     * Implement this method to transfer knowledge from off-line om to on-line om
     */
    protected abstract void transferKnowledge();

    /**
     * GETTERS
     */
    public BaseTBValueIterationOM getOffLineOM() {
        return offLineOM;
    }

    public ReinforcementLearningOM getOnLineOM() {
        return onLineOM;
    }
}
