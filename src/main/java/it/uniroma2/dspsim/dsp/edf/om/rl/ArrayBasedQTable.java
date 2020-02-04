package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class ArrayBasedQTable implements QTable {

	private double arr[];
	private int size;
	private int maxActionHash;

	public ArrayBasedQTable(double initializationValue, int maxStateHash, int maxActionHash) {
		this.size = (1+maxActionHash)*(1+maxStateHash);
		arr = new double[size];
		for (int i = 0; i<size; i++) {
			arr[i] = initializationValue;
		}

		this.maxActionHash = maxActionHash;
	}

	@Override
	public double getQ(State s, AbstractAction a) {
		int index = (maxActionHash + 1)*s.hashCode() + a.hashCode();
		return arr[index];
	}

	@Override
	public void setQ(State s, AbstractAction a, double value) {
		int index = (maxActionHash + 1)*s.hashCode() + a.hashCode();
		arr[index]	= value;
	}

}
