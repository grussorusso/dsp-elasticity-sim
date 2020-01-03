package it.uniroma2.dspsim.dsp.edf.om.fa.features.simple;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class ScalingFeature extends Feature {
    private int delta;

    public ScalingFeature(int delta) {
        super();
        this.delta = delta;
    }

    @Override
    protected void initWeights() {
        this.weights = new Double[] {0.0};
    }

    @Override
    public void updateWeight(double updateValue, Number... coordinate) {

    }

    @Override
    public double evaluate(State state, Action action, RewardBasedOM om) {
        if (action.getDelta() == this.delta)
            return 1.0;
        else
            return 0.0;
    }
}
