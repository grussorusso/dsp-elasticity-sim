package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class DeepHybridRewardBasedOM extends HybridRewardBasedOM {

    public DeepHybridRewardBasedOM(Operator operator) {
        super(operator);
    }

    @Override
    protected BaseTBValueIterationOM buildOffLineOM(Operator operator) {
        return new DeepTBValueIterationOM(operator);
    }

    @Override
    protected ReinforcementLearningOM buildOnLineOM(Operator operator) {
        return new DeepVLearningOM(operator);
    }

    @Override
    protected void transferKnowledge() {
        // overwrite deep-v-learning neural network with deep-tb-vi one
        ((DeepVLearningOM) this.getOnLineOM()).networkConf = ((DeepTBValueIterationOM) this.getOffLineOM()).network.getLayerWiseConfigurations();
        ((DeepVLearningOM) this.getOnLineOM()).network = ((DeepTBValueIterationOM) this.getOffLineOM()).network;
    }
}
