package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.Objects;

public class KLambdaState extends State {
    // k usage
    private int[] k;

    public KLambdaState(int index, int[] k, int lambda, int maxLambda, Operator operator) {
        super(index, k, lambda, maxLambda, operator);

        this.k = k;
    }

    @Override
    public int getArrayRepresentationLength() {
        return (this.getTotalStates() / (this.getMaxLambda() + 1)) + 1;
    }

    private int getTotalStates() {
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, this.operator,
                ComputingInfrastructure.getInfrastructure(), this.getMaxLambda());
        int count = 0;
        while (stateIterator.hasNext()) {
            stateIterator.next();
            count++;
        }
        return count;
    }

    @Override
    protected INDArray toArray(int features) {
        INDArray input = kToOneHotVector(features - 1);
        // append lambda level normalized value to input array
        input = Nd4j.append(input, 1, this.getNormalizedLambda(), 1);
        return input;
    }

    private INDArray kToOneHotVector(int features) {
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, ... , 0) state
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (2, 0, 0) state and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, ... , maxParallelism) state
        INDArray oneHotVector = Nd4j.create(features);
        oneHotVector.put(0, Math.floorDiv(this.getIndex(), this.getMaxLambda() + 1), 1);
        return oneHotVector;
    }

    private double lambdaLevelNormalized(int value, int maxValue) {
        return (double) value/ (double) maxValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KLambdaState)) return false;
        if (!super.equals(o)) return false;
        KLambdaState that = (KLambdaState) o;
        return Arrays.equals(k, that.k);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Arrays.hashCode(k);
        return result;
    }

    public int[] getK() {
        return k;
    }
}
