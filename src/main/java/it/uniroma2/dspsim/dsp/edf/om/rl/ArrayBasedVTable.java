package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;

public class ArrayBasedVTable implements VTable, Serializable {

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
			ArrayBasedVTable loaded = (ArrayBasedVTable)  in.readObject();
			if (loaded.size != this.size) {
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
