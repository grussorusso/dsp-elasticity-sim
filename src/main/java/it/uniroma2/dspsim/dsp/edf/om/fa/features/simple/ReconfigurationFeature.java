package it.uniroma2.dspsim.dsp.edf.om.fa.features.simple;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ReconfigurationFeature extends Feature {
    private int delta;
    private double[] weights;

    public ReconfigurationFeature(int delta) {
        super();
        this.delta = delta;
    }

    @Override
    protected void initWeights() {
        this.weights = new double[] {0.0};
    }

    @Override
    public boolean isActive(State state, Action action, RewardBasedOM om) {
        return action.getDelta() == this.delta;
    }

    @Override
    public double evaluate(State state, Action action, RewardBasedOM om) {
        if (isActive(state, action, om))
            return this.weights[0];
        else
            return 0.0;
    }

    @Override
    protected void updateWeight(double updateValue, State state, Action action, RewardBasedOM om) {
        this.weights[0] += updateValue;
    }

    @Override
    public List<Tuple2<Object, Double>> getWeights() {
        return Collections.singletonList(new Tuple2<>(0, this.weights[0]));
    }
}
