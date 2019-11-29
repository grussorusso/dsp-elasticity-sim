package it.uniroma2.dspsim.utils.matrix;

public interface MatrixOps<X, Y, V extends Number> {
    V getValue(X x, Y y);
    void setValue(X x, Y y, V v);
}
