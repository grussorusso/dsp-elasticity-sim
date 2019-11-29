package it.uniroma2.dspsim.utils.matrix;

public class IntegerMatrix<X, Y> extends Matrix<X, Y, Integer> {

    public IntegerMatrix(Integer initValue) {
        super(initValue);
    }

    @Override
    protected Integer sum(Integer v1, Integer v2) {
        return v1 + v2;
    }

    @Override
    protected Integer multiplyValues(Integer v1, Integer v2) {
        return v1 * v2;
    }
}