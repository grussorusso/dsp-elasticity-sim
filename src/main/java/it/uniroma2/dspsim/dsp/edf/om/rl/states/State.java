package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import it.uniroma2.dspsim.dsp.Operator;

import java.util.Arrays;

public abstract class State {

    private int [] actualDeployment;

    protected State(int[] k, int lambda, Operator operator) {
        this.actualDeployment = k;
    }

    public int overallParallelism() {
        int p = 0;
        for (int k : this.actualDeployment) {
            p += k;
        }

        return p;
    }

    public int[] getActualDeployment() {
        return this.actualDeployment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return Arrays.equals(getActualDeployment(), state.getActualDeployment());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getActualDeployment());
    }
}
