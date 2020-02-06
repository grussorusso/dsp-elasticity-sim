package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.io.Serializable;

public class JointQTable implements Serializable {


	private double arr[];
	private int maxActionHash;
	private int internalSize[];
	private int nOperators;

	private JointQTable(double initializationValue, int maxStateHash[], int maxActionHash) {
		this.nOperators = maxStateHash.length;
		this.internalSize = new int[nOperators];

		int size = 1;
		for (int i = 0; i<nOperators; i++) {
			internalSize[i] = Math.multiplyExact(1+maxActionHash, 1+maxStateHash[i]);
			size = Math.multiplyExact(size, internalSize[i]);
		}

		arr = new double[size];
		for (int i = 0; i<size; i++) {
			arr[i] = initializationValue;
		}

		this.maxActionHash = maxActionHash;
	}


	public static JointQTable createQTable (int nOperators, int maxParallelism[], int inputRateLevels)
	{
		int maxAHash = -1;
		int maxSHash[] = new int[nOperators];

		JointStateIterator it = new JointStateIterator(nOperators, maxParallelism, ComputingInfrastructure.getInfrastructure(), inputRateLevels);

		while (it.hasNext()) {
			JointState s = it.next();
			for (int i = 0; i < nOperators; i++) {
				maxSHash[i] = Math.max(maxSHash[i], s.getStates()[i].hashCode());
			}
		}

		JointActionIterator ait = new JointActionIterator(nOperators);
		while (ait.hasNext()) {
			JointAction a = ait.next();
			maxAHash = Math.max(maxAHash, a.getActions()[0].hashCode());
		}

		return new JointQTable(0.0, maxSHash, maxAHash);
	}

	private int computeIndex (JointState s, JointAction a) {
		int index[] = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			index[i] = (maxActionHash + 1)*s.states[i].hashCode() + a.actions[i].hashCode();
		}
		int jointIndex = index[0];
		int accumulatedSize = internalSize[0];

		for (int i = 1; i<nOperators; i++) {
			jointIndex = Math.multiplyExact(accumulatedSize, index[i]) + jointIndex;
			accumulatedSize *= internalSize[i];
		}

		return jointIndex;
	}

	public double getQ(JointState s, JointAction a) {
		return arr[computeIndex(s,a)];
	}

	public void setQ(JointState s, JointAction a, double value) {
		arr[computeIndex(s,a)]	= value;
	}
}
