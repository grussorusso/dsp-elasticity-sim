package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.util.Arrays;

public abstract class State {

    protected int index;
    protected int [] actualDeployment;
    private int lambda;
    private int maxLambda;

    public int getMaxParallelism() {
        return maxParallelism;
    }

    protected int maxParallelism;

    protected State(int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        this.index = index;
        this.actualDeployment = k;
        this.lambda = lambda;
        this.maxLambda = maxLambda;
        this.maxParallelism = maxParallelism;
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
                this.overallParallelism() + delta <= this.maxParallelism;
    }

    public int[] getActualDeployment() { return actualDeployment; }

    public int getLambda() {
        return lambda;
    }
    public void setLambda(int lambda) { this.lambda = lambda; }

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
    public String toString() {
        return dump();
    }

    /**
     * Dump state
     */
    public String dump() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < this.actualDeployment.length; i++) {
            str.append(String.format("%d", this.actualDeployment[i]));
            if (i < this.actualDeployment.length - 1)
                str.append(", ");
        }
        str.append(String.format("] %d/%d", this.lambda, this.maxLambda));
        return str.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State)) return false;
        State state = (State) o;
        return getLambda() == state.getLambda() &&
                getMaxLambda() == state.getMaxLambda() &&
                Arrays.equals(getActualDeployment(), state.getActualDeployment());
    }

    @Override
    public int hashCode() {
        int h = 0;
        for (int i = 0; i < this.actualDeployment.length; ++i){
            ++h;
            h *= 1 << (this.actualDeployment[i] + 1);
        }
        h >>= 1; //h/2

        h = h * (this.maxLambda + 1) + this.lambda;
        return h;
    }

    public abstract INDArray arrayRepresentation(NeuralStateRepresentation repr) throws IllegalArgumentException;
}
