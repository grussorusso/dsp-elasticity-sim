package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Arrays;
import java.util.Objects;

public abstract class State {

    private int index;
    private int [] actualDeployment;
    private int lambda;
    private int maxLambda;
    protected Operator operator;

    protected State(int index, int[] k, int lambda, int maxLambda, Operator operator) {
        this.index = index;
        this.actualDeployment = k;
        this.lambda = lambda;
        this.maxLambda = maxLambda;
        this.operator = operator;
    }

    public int overallParallelism() {
        int p = 0;
        for (int k : this.actualDeployment) {
            p += k;
        }

        return p;
    }

    public boolean validateAction(Action a) {
        int delta = a.getDelta();
        if (delta == 0)
            return true;
        if (delta < 0) {
            if (this.actualDeployment[a.getResTypeIndex()] + delta < 0)
                return false;
        }

        return this.overallParallelism() + delta >= 1 &&
                this.overallParallelism() + delta <= this.operator.getMaxParallelism();
    }

    public int getLambda() {
        return lambda;
    }

    public int getMaxLambda() {
        return maxLambda;
    }

    public int getIndex() {
        return index;
    }

    public double getNormalizedLambda() {
        return MathUtils.normalizeValue(this.lambda, this.maxLambda);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return getLambda() == state.getLambda() &&
                getMaxLambda() == state.getMaxLambda();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLambda(), getMaxLambda());
    }

    public INDArray arrayRepresentation() throws IllegalArgumentException {
        return arrayRepresentation(getArrayRepresentationLength());
    }

    public INDArray arrayRepresentation(int features) throws IllegalArgumentException {
        // TODO maybe index must be leq of features?
        if (this.index >= 0)
            return this.toArray(features);
        else
            throw new IllegalArgumentException("State must be indexed to extract it as array");
    }

    protected abstract INDArray toArray(int features);

    public abstract int getArrayRepresentationLength();
}
