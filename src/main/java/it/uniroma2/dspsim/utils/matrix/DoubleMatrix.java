package it.uniroma2.dspsim.utils.matrix;

public class DoubleMatrix<X, Y> extends Matrix<X, Y, Double> {

    public DoubleMatrix(Double initValue) {
        super(initValue);
    }

    @Override
    protected Double sum(Double v1, Double v2) {
        return v1 + v2;
    }

    @Override
    protected Double multiplyValues(Double v1, Double v2) {
        return v1 * v2;
    }
}