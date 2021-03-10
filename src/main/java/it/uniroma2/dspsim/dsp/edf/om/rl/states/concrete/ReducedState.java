package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Arrays;
import java.util.Objects;

@Deprecated
public class ReducedState extends State {

    // sum of k usage not normalized, e.g. (1, 2, 0) -> 3
    private int kLevel;

    // max k level
    private int maxKLevel;

    // binary k mask, e.g. (1, 2, 0) -> (1, 1, 0)
    private int[] kMask;

    public ReducedState(int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        super(index, k, lambda, maxLambda, maxParallelism);

        this.kLevel = Arrays.stream(k).sum();

        this.maxKLevel = maxParallelism;

        this.kMask = Arrays.stream(k).map(value -> value > 0 ? 1 : 0).toArray();
    }

    @Override
    public int getArrayRepresentationLength() {
        // normalized k and normalized lambda
        int length = 2;
        // max k mask binary number to base 10
        int[] num = new int[this.kMask.length];
        for (int i = 0; i < this.kMask.length; i++)
            num[i] = 1;
        length += MathUtils.toBase10(num, 2);
        return length;
    }

    @Override
    public INDArray arrayRepresentation(int features) throws IllegalArgumentException {
        // k mask one hot vector array
        INDArray array = kToOneHotVector(features - 2);
        // normalized lambda
        array = Nd4j.append(array, 1, this.getNormalizedLambda(), 1);
        // normalize k level
        array = Nd4j.append(array, 1, MathUtils.normalizeValue(this.kLevel, this.maxKLevel), 1);

        return array;
    }

    private INDArray kToOneHotVector(int features) {
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, ... , 0) k mask
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (2, 0, 0, ... , 0) k mask and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, ... , 1) k mask
        INDArray oneHotVector = Nd4j.create(features);
        // revert k mask to obtain base 2 number
        int[] num = new int[this.kMask.length];
        for (int i = 0; i < this.kMask.length; i++)
            num[i] = this.kMask[this.kMask.length - 1 - i];
        int index = MathUtils.toBase10(num, 2);
        oneHotVector.put(0, index - 1, 1);
        return oneHotVector;
    }

    public int getkLevel() {
        return kLevel;
    }

    public int getMaxKLevel() {
        return maxKLevel;
    }

    public int[] getkMask() {
        return kMask;
    }

    @Override
    public String dump() {
        StringBuilder str = new StringBuilder(super.dump());
        str.append("\t[");
        for (int i = 0; i < this.kMask.length; i++) {
            str.append(String.format("%d", this.kMask[i]));
            if (i < this.kMask.length - 1)
                str.append(", ");
        }
        str.append(String.format("]\t%d\t%d", this.kLevel, this.maxKLevel));
        return str.toString();
    }
}
