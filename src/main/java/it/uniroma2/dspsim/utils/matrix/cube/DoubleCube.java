package it.uniroma2.dspsim.utils.matrix.cube;

import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.Matrix;

public class DoubleCube<X, Y, Z> extends Cube<X, Y, Z, Double> {
    public DoubleCube(Double initValue) {
        super(initValue);
    }

    @Override
    protected Matrix<Y, Z, Double> initMatrix(Double initValue) {
        return new DoubleMatrix<>(initValue);
    }
}
