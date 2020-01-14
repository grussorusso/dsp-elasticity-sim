package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class HybridRewardBasedOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    private DeepTBValueIterationOM deepTBValueIterationOM;
    private DeepVLearningOM deepVLearningOM;

    public HybridRewardBasedOM(Operator operator) {
        super(operator);

        this.deepTBValueIterationOM = new DeepTBValueIterationOM(operator);
        this.deepVLearningOM = new DeepVLearningOM(operator);

        // overwrite deep-v-learning neural network with deep-tb-vi one
        this.deepVLearningOM.networkConf = this.deepTBValueIterationOM.network.getLayerWiseConfigurations();
        this.deepVLearningOM.network = this.deepTBValueIterationOM.network;

        // recall init action selection policy
        // to overwrite initial action selection policy with deep-v-learning configured one
        initActionSelectionPolicy();
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        if (this.deepVLearningOM == null) {
            return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        } else {
            return this.deepVLearningOM.getActionSelectionPolicy();
        }
    }

    @Override
    protected void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {
        // delegate reward collection to deep-v-learning om
        this.deepVLearningOM.useReward(reward, lastState, lastChosenAction, currentState, monitoringInfo);
    }

    @Override
    public boolean validateAction(State s, Action a) {
        if (this.deepVLearningOM == null) {
            return s.validateAction(a);
        } else {
            return this.deepVLearningOM.validateAction(s, a);
        }
    }

    @Override
    public double evaluateAction(State s, Action a) {
        if (this.deepVLearningOM == null) {
            if (this.deepTBValueIterationOM == null ) {
                return 0.0;
            } else {
                return this.deepTBValueIterationOM.evaluateAction(s, a);
            }
        } else {
            return this.deepVLearningOM.evaluateAction(s, a);
        }
    }
}
