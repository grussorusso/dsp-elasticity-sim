package it.uniroma2.dspsim.utils;

public class Tuple2<K, V> {

    private K k;
    private V v;

    public Tuple2(K k, V v) {
        this.k = k;
        this.v = v;
    }

    public K getK() {
        return k;
    }

    public V getV() {
        return v;
    }
}
