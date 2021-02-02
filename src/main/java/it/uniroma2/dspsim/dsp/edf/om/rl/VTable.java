package it.uniroma2.dspsim.dsp.edf.om.rl;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public interface VTable {

	double getV (State s);
	void setV (State s, double value);

}
