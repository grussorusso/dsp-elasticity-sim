package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class ArrayBasedVTable implements VTable {

	private double arr[];
	private int size;

	public ArrayBasedVTable(double initializationValue, int maxStateHash) {
		this.size = 1+maxStateHash;
		arr = new double[size];
		for (int i = 0; i<size; i++) {
			arr[i] = initializationValue;
		}
	}

	@Override
	public double getV(State s) {
		return arr[s.hashCode()];
	}

	@Override
	public void setV(State s, double value) {
		arr[s.hashCode()]	= value;
	}

}
