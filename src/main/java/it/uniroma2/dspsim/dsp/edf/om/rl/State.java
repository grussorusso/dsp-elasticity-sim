package it.uniroma2.dspsim.dsp.edf.om.rl;

import java.util.Arrays;
import java.util.Objects;

public class State extends AbstractState {
    private int index = -1;
    private int k[];
    private int lambda;

    public State(int index, int[] k, int lambda) {
        this.index = index;
        this.k = k;
        this.lambda = lambda;
    }

    public State(int[] k, int lambda) {
        this.k = k;
        this.lambda = lambda;
    }

    public int overallParallelism() {
        int p = 0;
        for (int _k : k) {
            p += _k;
        }

        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return getLambda() == state.getLambda() &&
                Arrays.equals(getK(), state.getK());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getLambda());
        result = 31 * result + Arrays.hashCode(getK());
        return result;
    }

    public int[] getK() {
        return k;
    }

    public int getLambda() {
        return lambda;
    }

    public int getIndex() {
        return index;
    }
}
