package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

public class JointQTable {


	private double arr[];
	private int maxActionHash;
	private int size1;

	private JointQTable(double initializationValue, int maxStateHash1, int maxStateHash2, int maxActionHash) {
		this.size1 = (1+maxActionHash)*(1+maxStateHash1);
		int size2 = (1+maxActionHash)*(1+maxStateHash2);
		int size = Math.multiplyExact(size1,size2);
		arr = new double[size];
		for (int i = 0; i<size; i++) {
			arr[i] = initializationValue;
		}

		this.maxActionHash = maxActionHash;
	}


	public static JointQTable createQTable (int nOperators, int maxParallelism[], int inputRateLevels)
	{
		//TODO 3+ operators
		if (nOperators != 2) {
			throw new RuntimeException("Only 2 operators are supported.");
		}

		int maxAHash = -1;
		int maxSHash1 = -1;
		int maxSHash2 = -1;

		JointStateIterator it = new JointStateIterator(nOperators, maxParallelism, ComputingInfrastructure.getInfrastructure(), inputRateLevels);

		while (it.hasNext()) {
			JointState s = it.next();
			maxSHash1 = Math.max(maxSHash1, s.getStates()[0].hashCode());
			maxSHash2 = Math.max(maxSHash2, s.getStates()[1].hashCode());
		}
		JointActionIterator ait = new JointActionIterator(nOperators);
		while (ait.hasNext()) {
			JointAction a = ait.next();
			maxAHash = Math.max(maxAHash, a.getActions()[0].hashCode());
		}

		return new JointQTable(0.0, maxSHash1, maxSHash2, maxAHash);
	}

	public double getQ(JointState s, JointAction a) {
		int index1 = (maxActionHash + 1)*s.states[0].hashCode() + a.actions[0].hashCode();
		int index2 = (maxActionHash + 1)*s.states[1].hashCode() + a.actions[1].hashCode();
		int index = Math.multiplyExact(size1, index2) + index1;
		return arr[index];
	}

	public void setQ(JointState s, JointAction a, double value) {
		int index1 = (maxActionHash + 1)*s.states[0].hashCode() + a.actions[0].hashCode();
		int index2 = (maxActionHash + 1)*s.states[1].hashCode() + a.actions[1].hashCode();
		int index = Math.multiplyExact(size1, index2) + index1;
		arr[index]	= value;
	}
}
