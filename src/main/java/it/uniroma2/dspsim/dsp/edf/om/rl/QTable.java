package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.io.File;

public interface QTable {

	double getQ (State s, AbstractAction a);
	void setQ (State s, AbstractAction a, double value);
	void dump (File f);
	void load (File f);

}
