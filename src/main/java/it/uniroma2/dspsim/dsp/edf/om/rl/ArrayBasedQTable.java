package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;

public class ArrayBasedQTable implements QTable, Serializable {

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

	@Override
	public void dump(File f) {
		try {
			FileOutputStream fileOut = new FileOutputStream(f.getAbsolutePath());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this);
			out.close();
			fileOut.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void load(File f) {
		try {
			FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
			ObjectInputStream in = new ObjectInputStream(fileIn);
			ArrayBasedQTable loaded = (ArrayBasedQTable)  in.readObject();
			if (loaded.size != this.size || loaded.maxActionHash != this.maxActionHash) {
				throw new RuntimeException("Trying to load malformed QTable");
			}
			in.close();
			fileIn.close();

			this.arr = loaded.arr;
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
}
