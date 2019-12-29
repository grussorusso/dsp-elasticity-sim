package it.uniroma2.dspsim.dsp.edf.om.fa.features;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.ArrayList;
import java.util.List;

public abstract class Feature {
    protected List<Double> weights;

    public Feature() {
        this.weights = new ArrayList<>();
        this.initWeights(0.0);
    }

    public void addWeights(int number, double value) {
        for (int i = 0; i < number; i++) {
            this.weights.add(value);
        }
    }

    public void updateWeight(int position, double updateValue) {
        this.weights.set(position, Double.sum(this.weights.get(position), updateValue));
    }

    protected abstract int initWeights(double initValue);
    public abstract double evaluate(State state, Action action);
    public abstract int getWeightIndex(State state, Action action);
}
