package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;

public abstract class OperatorManager {

	private Operator operator;

	public OperatorManager (Operator operator)  {
		this.operator = operator;
	}


	abstract public Reconfiguration pickReconfiguration (OMMonitoringInfo monitoringInfo);

}
