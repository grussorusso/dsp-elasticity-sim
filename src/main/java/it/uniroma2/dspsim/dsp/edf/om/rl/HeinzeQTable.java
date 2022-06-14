package it.uniroma2.dspsim.dsp.edf.om.rl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.HeinzeState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.*;

public class HeinzeQTable {

	private Table<Integer, Integer, Double> table = HashBasedTable.create();
	private final double initializationValue;

	public HeinzeQTable(double initializationValue) {
		this.initializationValue = initializationValue;
	}

	public double getQ(HeinzeState s, AbstractAction a) {
		Double q = table.get(s.hashCode(),a.hashCode());
		if (q == null) {
			table.put(s.hashCode(), a.hashCode(), initializationValue);
			return initializationValue; // TODO put() before returning?
		}

		return q;
	}

	public void setQ(HeinzeState s, AbstractAction a, double value) {
		table.put(s.hashCode(), a.hashCode(), value);
	}

}
