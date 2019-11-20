package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.Arrays;
import java.util.Objects;

public class KLambdaState extends State {
    // k usage
    private int[] k;

    // lambda level
    private int lambda;

    public KLambdaState(int[] k, int lambda, Operator operator) {
        super(k, lambda, operator);

        this.k = k;
        this.lambda = lambda;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KLambdaState)) return false;
        if (!super.equals(o)) return false;
        KLambdaState that = (KLambdaState) o;
        return lambda == that.lambda &&
                Arrays.equals(k, that.k);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), lambda);
        result = 31 * result + Arrays.hashCode(k);
        return result;
    }
}
