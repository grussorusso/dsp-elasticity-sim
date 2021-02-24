package it.uniroma2.dspsim.utils;

import java.util.LinkedHashMap;
import java.util.Map;

public class HashCache<K,V> extends LinkedHashMap {

	private final int maxSize;

	public HashCache(int maxSize) {
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry eldest) {
		return size() > maxSize;
	}

	@Override
	public boolean equals(Object obj){
		return obj==this;
	}

	@Override
	public int hashCode(){
		return System.identityHashCode(this);
	}

}
