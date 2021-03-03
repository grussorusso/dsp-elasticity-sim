package it.uniroma2.dspsim.dsp.edf.om.rl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;

public class GuavaBasedQTable implements QTable {

	private Table<Integer, Integer, Double> table = HashBasedTable.create();
	private double initializationValue;

	public GuavaBasedQTable(double initializationValue) {
		this.initializationValue = initializationValue;
	}

	@Override
	public double getQ(State s, AbstractAction a) {
		Double q = table.get(s.hashCode(),a.hashCode());
		if (q == null)
			return initializationValue; // TODO put() before returning?

		return q;
	}

	@Override
	public void setQ(State s, AbstractAction a, double value) {
		table.put(s.hashCode(), a.hashCode(), value);
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
			this.table = (Table<Integer, Integer, Double>)in.readObject();
			in.close();
			fileIn.close();
		} catch (IOException i) {
			i.printStackTrace();
		} catch (ClassNotFoundException c) {
			c.printStackTrace();
		}
	}
}
