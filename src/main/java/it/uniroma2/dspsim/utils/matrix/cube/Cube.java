package it.uniroma2.dspsim.utils.matrix.cube;

import it.uniroma2.dspsim.utils.matrix.Matrix;

import java.util.HashMap;

public abstract class Cube<Z, X, Y, V extends Number> {
    private HashMap<Z, Matrix<X, Y, V>> cube;
    private V initValue;

    public Cube(V initValue) {
        this.initValue = initValue;
        this.cube = new HashMap<>();
    }

    public void setValue(X x, Y y, Z z, V v) {
        getMatrix(z).setValue(x, y, v);
    }

    public void getValue(X x, Y y, Z z) {
        getMatrix(z).getValue(x, y);
    }

    public void add(X x, Y y, Z z, V v) {
        getMatrix(z).add(x, y, v);
    }

    public void multiply(X x, Y y, Z z, V v) {
        getMatrix(z).multiply(x, y, v);
    }

    private Matrix<X, Y, V> getMatrix(Z z) {
        Matrix matrix = cube.get(z);
        if (matrix == null) {
            matrix = initMatrix(initValue);
        }
        return matrix;
    }


    protected abstract Matrix<X, Y, V> initMatrix(V initValue);
}
