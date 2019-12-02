package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import com.google.common.base.Objects;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;

public class KLambdaState extends State {
    // k usage
    private int[] k;

    public KLambdaState(int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        super(index, k, lambda, maxLambda, maxParallelism);

        this.k = k;
    }

    @Override
    public int getArrayRepresentationLength() {
        return (this.getTotalStates() / (this.getMaxLambda() + 1)) + 1;
    }

    private int getTotalStates() {
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, this.maxParallelism,
                ComputingInfrastructure.getInfrastructure(), this.getMaxLambda() + 1);
        int count = 0;
        while (stateIterator.hasNext()) {
            stateIterator.next();
            count++;
        }
        return count;
    }

    @Override
    public INDArray arrayRepresentation(int features) throws IllegalArgumentException {
        if (this.getIndex() < 0) {
            StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, this.maxParallelism,
                    ComputingInfrastructure.getInfrastructure(), this.getMaxLambda() + 1);
            while (stateIterator.hasNext()) {
                State state = stateIterator.next();
                if (this.equals(state)) {
                    this.index = state.getIndex();
                    break;
                }
            }
        }

        if (this.index < 0)
            throw new IllegalArgumentException("State must be indexed to extract it as array");

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

    public int[] getK() {
        return k;
    }
}
