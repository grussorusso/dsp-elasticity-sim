package it.uniroma2.dspsim.dsp.edf.om.fa.features;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {

    public Feature() {
        initWeights();
    }

    protected abstract void initWeights();
    public abstract void updateWeight(double updateValue, State state, Action action, RewardBasedOM om);
    public abstract boolean isActive(State state, Action action, RewardBasedOM om);
    public abstract double evaluate(State state, Action action, RewardBasedOM om);
}
