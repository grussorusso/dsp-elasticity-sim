package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;

public abstract class OperatorManager {

	protected Operator operator;

	public OperatorManager (Operator operator)  {
		this.operator = operator;
	}


	abstract public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo);

	public Operator getOperator() {
		return operator;
	}
}
