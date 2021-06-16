package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class MapBasedVTable implements VTable {

	private Map<Integer, Double> table = new HashMap<>();
	private final double initializationValue;

	public MapBasedVTable(double initializationValue) {
		this.initializationValue = initializationValue;
	}

	@Override
	public double getV(State s) {
		Double q = table.get(s.hashCode());
		if (q == null) {
			table.put(s.hashCode(), initializationValue);
			return initializationValue;
		}

		return q;
	}

	@Override
	public void setV(State s, double value) {
		table.put(s.hashCode(), value);
	}

	@Override
	public void dump(File f) {
		try {
			FileOutputStream fileOut = new FileOutputStream(f.getAbsolutePath());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(this.table);
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
			this.table = (HashMap<Integer, Double>)in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
}
