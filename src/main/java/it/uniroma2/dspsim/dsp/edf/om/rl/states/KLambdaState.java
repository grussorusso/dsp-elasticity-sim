package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

// TODO: the current class hierarchy should be revised
// There is no reason to keep this class distinct from State any more
public class KLambdaState extends State {

    public KLambdaState(int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        super(index, k, lambda, maxLambda, maxParallelism);
    }



    @Override
    public INDArray arrayRepresentation(NeuralStateRepresentation repr) throws IllegalArgumentException {
        final int features = repr.getRepresentationLength();

        if (!repr.reducedDeploymentRepresentation && this.getIndex() < 0) {
            StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, this.maxParallelism,
                    ComputingInfrastructure.getInfrastructure(), this.getMaxLambda() + 1);
            while (stateIterator.hasNext()) {
                State state = stateIterator.next();
                if (this.equals(state)) {
                    this.index = state.getIndex();
                    break;
                }
            }

            if (this.index < 0)
                throw new IllegalArgumentException("State must be indexed to extract it as array");
        }

        INDArray input;
        if (!repr.reducedDeploymentRepresentation) {
            final int kFeatures = repr.oneHotForLambda ? (features - (this.getMaxLambda() + 1)) : (features - 1);
            input = kToOneHotVector(kFeatures);
        } else {
            input = kToReducedOneHotVector(repr.useResourceSetInReducedRepr);
        }

        if (!repr.oneHotForLambda) {
            // append lambda level normalized value to input array
            input = Nd4j.append(input, 1, this.getNormalizedLambda(), 1);
        } else {
            input = Nd4j.concat(1, input, lambda2OneHotVector());
        }
        return input;
    }

    private INDArray lambda2OneHotVector() {
        INDArray oneHotVector = Nd4j.zeros(this.getMaxLambda()+1);
        oneHotVector.put(0, this.getLambda(), 1);
        return oneHotVector;
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

    private INDArray kToReducedOneHotVector (boolean useResourceSet) {
        int size = maxParallelism * actualDeployment.length;
        if (useResourceSet)
            size += (1 << actualDeployment.length) - 1;

        INDArray oneHotVector = Nd4j.zeros(size);

        int setOfTypesIndex = 0;
        for (int i = 0; i < this.actualDeployment.length; i++)  {
            final int toSetIndex = i * maxParallelism + (this.actualDeployment[i] - 1);
            oneHotVector.put(0, toSetIndex, 1);
            if (this.actualDeployment[i] > 0)
                setOfTypesIndex += (1 << i);
        }

        if (useResourceSet)
            oneHotVector.put(0, maxParallelism * actualDeployment.length + setOfTypesIndex-1, 1);


        return oneHotVector;
    }
}
