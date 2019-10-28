package it.uniroma2.dspsim.dsp.edf.om.rl;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public class GuavaBasedQTable implements QTable {

	private Table<AbstractState, AbstractAction, Double> table = HashBasedTable.create();
	private double initializationValue;

	public GuavaBasedQTable(double initializationValue) {
		this.initializationValue = initializationValue;
	}

	@Override
	public double getQ(AbstractState s, AbstractAction a) {
		Double q = table.get(s,a);
		if (q == null)
			return initializationValue; // TODO put() before returning?

		return q;
	}

	@Override
	public void setQ(AbstractState s, AbstractAction a, double value) {
		table.put(s,a,value);
	}
}
