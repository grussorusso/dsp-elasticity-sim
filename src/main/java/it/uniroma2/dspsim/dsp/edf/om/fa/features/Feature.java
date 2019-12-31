package it.uniroma2.dspsim.dsp.edf.om.fa.features;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {
    protected Object weights;

    public Feature() {
        initWeights();
    }

    protected abstract void initWeights();
    public abstract void updateWeight(double updateValue, Number... coordinate);
    public abstract double evaluate(State state, Action action);
    public abstract int getWeightIndex(State state, Action action);
}
