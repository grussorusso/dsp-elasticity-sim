package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;

public class FAHybridRewardBasedOM extends HybridRewardBasedOM {

    public FAHybridRewardBasedOM(Operator operator) {
        super(operator);
    }

    @Override
    protected BaseTBValueIterationOM buildOffLineOM(Operator operator) {
        return new FaTBValueIterationOM(operator);
    }

    @Override
    protected ReinforcementLearningOM buildOnLineOM(Operator operator) {
        return new FAQLearningOM(operator);
    }

    @Override
    protected void transferKnowledge() {
        ((FAQLearningOM) this.getOnLineOM()).functionApproximationManager =
                ((FaTBValueIterationOM) this.getOffLineOM()).functionApproximationManager;
    }
}
