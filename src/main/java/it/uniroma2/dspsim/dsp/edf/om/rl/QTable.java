package it.uniroma2.dspsim.dsp.edf.om.rl;

public interface QTable {

	double getQ (AbstractState s, AbstractAction a);
	void setQ (AbstractState s, AbstractAction a, double value);

}
