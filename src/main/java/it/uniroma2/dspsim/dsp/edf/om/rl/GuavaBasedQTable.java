package it.uniroma2.dspsim.dsp.edf.om.rl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class GuavaBasedQTable implements QTable {

	private Table<State, AbstractAction, Double> table = HashBasedTable.create();
	private double initializationValue;

	public GuavaBasedQTable(double initializationValue) {
		this.initializationValue = initializationValue;
	}

	@Override
	public double getQ(State s, AbstractAction a) {
		Double q = table.get(s,a);
		if (q == null)
			return initializationValue; // TODO put() before returning?

		return q;
	}

	@Override
	public void setQ(State s, AbstractAction a, double value) {
		table.put(s,a,value);
	}
}
