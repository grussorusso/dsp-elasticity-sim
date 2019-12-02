package it.uniroma2.dspsim.utils.matrix;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.Set;

public abstract class Matrix<X, Y, V extends Number> implements MatrixOps<X, Y, V> {
    protected Table<X, Y, V> table;

    protected V initValue;

    public Matrix(V initValue) {
        this.initValue = initValue;
        this.table = HashBasedTable.create();
    }

    @Override
    public V getValue(X x, Y y) {
        V value = table.get(x, y);
        if (value == null)
            return this.initValue;

        return value;
    }

    @Override
    public void setValue(X x, Y y, V v) {
        table.put(x, y, v);
    }

    public void add(X x, Y y, V v) {
        V value = this.getValue(x, y);
        this.setValue(x, y, this.sum(value, v));
    }

    public void multiply(X x, Y y, V v) {
        V value = this.getValue(x, y);
        this.setValue(x, y,this.multiplyValues(value, v));
    }

    public Set<X> getRowLabels() {
        return this.table.rowKeySet();
    }

    public Set<X> getRowLabels(Y y) {
        return this.table.column(y).keySet();
    }

    public Set<Y> getColLabels() {
        return this.table.columnKeySet();
    }

    public Set<Y> getColLabels(X x) {
        return this.table.row(x).keySet();
    }

    public V rowSum(X x) {
        V s = null;
        Map<Y, V> rowValues = this.table.row(x);
        for (V value : rowValues.values()) {
            if (s == null)
                s = value;
            else
                s = this.sum(s, value);
        }

        return s;
    }

    public V colSum(Y y) {
        V s = null;
        Map<X, V> colValues = this.table.column(y);
        for (V value : colValues.values()) {
            if (s == null)
                s = value;
            else
                s = this.sum(s, value);
        }

        return s;
    }

    protected abstract V sum(V v1, V v2);
    protected abstract V multiplyValues(V v1, V v2);

    public void print() {
        Set<X> xLabels = this.getRowLabels();
        for (X x : xLabels) {
            System.out.println("Row: " + x.toString());
            Set<Y> yLabels = this.getColLabels(x);
            StringBuilder row = new StringBuilder();
            for (Y y : yLabels) {
                row.append(this.getValue(x, y));
                row.append("\t");
            }
            System.out.println(row);
        }
    }
}
