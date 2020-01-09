package it.uniroma2.dspsim.utils.matrix.cube;

import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.Matrix;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public abstract class Cube<Z, X, Y, V extends Number> {
    private HashMap<Z, Matrix<X, Y, V>> cube;
    private V initValue;

    public Cube(V initValue) {
        this.initValue = initValue;
        this.cube = new HashMap<>();
    }

    public void setValue(X x, Y y, Z z, V v) {
        Matrix<X, Y, V> matrix = this.cube.get(z);
        if (matrix == null) {
            matrix = initMatrix(initValue);
            this.cube.put(z, matrix);
        }
        matrix.setValue(x, y, v);
    }

    public V getValue(X x, Y y, Z z) {
        Matrix<X, Y, V> matrix = this.cube.get(z);
        if (matrix == null) {
            return initValue;
        } else {
            return matrix.getValue(x, y);
        }
    }

    public void add(X x, Y y, Z z, V v) {
        Matrix<X, Y, V> matrix = this.cube.get(z);
        if (matrix == null) {
            matrix = initMatrix(initValue);
            this.cube.put(z, matrix);
        }
        matrix.add(x, y, v);
    }

    public void multiply(X x, Y y, Z z, V v) {
        Matrix<X, Y, V> matrix = this.cube.get(z);
        if (matrix == null) {
            matrix = initMatrix(initValue);
            this.cube.put(z, matrix);
        }
        matrix.multiply(x, y, v);
    }

    public List<Tuple2<Z, Matrix<X, Y, V>>> get2DSections() {
        List<Tuple2<Z, Matrix<X, Y, V>>> sections = new ArrayList<>();
        for (Z z : this.cube.keySet()) {
            sections.add(new Tuple2<>(z, this.cube.get(z)));
        }
        return sections;
    }


    protected abstract Matrix<X, Y, V> initMatrix(V initValue);
}
