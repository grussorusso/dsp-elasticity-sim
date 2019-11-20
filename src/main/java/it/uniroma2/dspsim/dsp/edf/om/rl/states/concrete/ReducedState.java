package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.Arrays;
import java.util.Objects;

public class ReducedState extends State {

    // sum of k usage not normalized, e.g. (1, 2, 0) -> 3
    private int kLevel;

    // max k level
    private int maxKLevel;

    // lambda level not normalized
    private int lambdaLevel;

    // binary k mask, e.g. (1, 2, 0) -> (1, 1, 0)
    private int[] kMask;

    public ReducedState(int[] k, int lambda, Operator operator) {
        super(k, lambda, operator);

        this.kLevel = Arrays.stream(k).sum();

        this.maxKLevel = operator.getMaxParallelism();

        this.lambdaLevel = lambda;

        this.kMask = Arrays.stream(k).map(value -> value > 0 ? 1 : 0).toArray();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReducedState)) return false;
        if (!super.equals(o)) return false;
        ReducedState that = (ReducedState) o;
        return kLevel == that.kLevel &&
                maxKLevel == that.maxKLevel &&
                lambdaLevel == that.lambdaLevel &&
                Arrays.equals(kMask, that.kMask);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), kLevel, maxKLevel, lambdaLevel);
        result = 31 * result + Arrays.hashCode(kMask);
        return result;
    }
}
