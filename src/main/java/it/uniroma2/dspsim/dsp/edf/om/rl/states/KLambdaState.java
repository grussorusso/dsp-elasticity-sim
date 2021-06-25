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

        if (!(repr.reducedDeploymentRepresentation || repr.minimalRepresentation) && this.getIndex() < 0) {
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

        INDArray input = Nd4j.zeros(1, features);
        int offset = 0;
        if (!repr.reducedDeploymentRepresentation && !repr.minimalRepresentation) {
            kToOneHotVector(input);
            final int kFeatures = repr.oneHotForLambda ? (features - (this.getMaxLambda() + 1)) : (features - 1);
            offset += kFeatures;
        } else if (repr.minimalRepresentation) {
            kToNormalizedVector(input);
            offset += this.actualDeployment.length;
        } else {
            kToReducedOneHotVector(input, repr.useResourceSetInReducedRepr);
            offset = maxParallelism * actualDeployment.length;
            if (repr.useResourceSetInReducedRepr)
                offset += (1 << actualDeployment.length) - 1;
        }

        if (!repr.oneHotForLambda) {
            // append lambda level normalized value to input array
            input.put(0, offset, this.getNormalizedLambda());
        } else {
            input.put(0, offset + this.getLambda(), 1);
        }
        return input;
    }

    private void kToOneHotVector(INDArray input) {
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, ... , 0) state
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (2, 0, 0) state and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, ... , maxParallelism) state
        input.put(0, Math.floorDiv(this.getIndex(), this.getMaxLambda() + 1), 1);
    }

    private void kToNormalizedVector (INDArray input) {
        for (int i = 0; i < this.actualDeployment.length; i++)  {
            input.put(0, i, (double)this.actualDeployment[i]/(double)maxParallelism);
        }
    }
    private void kToReducedOneHotVector (INDArray input, boolean useResourceSet) {

        int setOfTypesIndex = 0;
        for (int i = 0; i < this.actualDeployment.length; i++)  {
            final int toSetIndex = i * maxParallelism + (this.actualDeployment[i] - 1);
            input.put(0, toSetIndex, 1);
            if (this.actualDeployment[i] > 0)
                setOfTypesIndex += (1 << i);
        }

        if (useResourceSet)
            input.put(0, maxParallelism * actualDeployment.length + setOfTypesIndex-1, 1);
    }
}
