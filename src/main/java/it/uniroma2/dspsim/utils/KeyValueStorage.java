package it.uniroma2.dspsim.utils;

import java.util.*;

public class KeyValueStorage<K, V> {
    private HashMap<K, V> map;

    public KeyValueStorage() {
        this.map = new HashMap<>();
    }

    public void addKeyValue(K key, V value) {
        if (this.map.containsKey(key)) {
            throw new IllegalArgumentException(
                    String.format("key '%s' already used in metadata", key));
        }

        this.map.put(key, value);
    }

    public V getValue(K key) {
        return this.map.get(key);
    }

    public Collection<V> getAll() {
        return this.map.values();
    }

    // update value in metadata removing old value and adding new value
    // WARNING: class consistency is left to the developer
    public void updateValue(K key, V newValue) {
        this.removeKeyValue(key);
        this.addKeyValue(key, newValue);
    }

    public void removeKeyValue(K key) {
        this.map.remove(key);
    }

    public boolean containsKey(K key) {
        return this.map.containsKey(key);
    }
}
