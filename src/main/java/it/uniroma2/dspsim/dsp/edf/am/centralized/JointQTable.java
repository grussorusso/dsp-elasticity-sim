package it.uniroma2.dspsim.dsp.edf.am.centralized;

public class JointQTable {


	private double arr[];
	private int maxActionHash;
	private int size1;

	public JointQTable(double initializationValue, int maxStateHash1, int maxStateHash2, int maxActionHash) {
		this.size1 = (1+maxActionHash)*(1+maxStateHash1);
		int size2 = (1+maxActionHash)*(1+maxStateHash2);
		int size = Math.multiplyExact(size1,size2);
		arr = new double[size];
		for (int i = 0; i<size; i++) {
			arr[i] = initializationValue;
		}

		this.maxActionHash = maxActionHash;
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
