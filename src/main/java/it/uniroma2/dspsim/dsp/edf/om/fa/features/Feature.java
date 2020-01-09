package it.uniroma2.dspsim.dsp.edf.om.fa.features;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {

    public Feature() {
        initWeights();
    }

    public void update(double updateValue, State state, Action action, RewardBasedOM om) {
        if (isActive(state, action, om))
            updateWeight(updateValue, state, action, om);
    }

    protected abstract void initWeights();
    protected abstract void updateWeight(double updateValue, State state, Action action, RewardBasedOM om);
    public abstract boolean isActive(State state, Action action, RewardBasedOM om);
    public abstract double evaluate(State state, Action action, RewardBasedOM om);
    public abstract List<Tuple2<Object, Double>> getWeights();
}
