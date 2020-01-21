package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;

public abstract class OperatorManager {

	protected Operator operator;

	public OperatorManager (Operator operator)  {
		this.operator = operator;
	}


	abstract protected Reconfiguration pickReconfiguration (OMMonitoringInfo monitoringInfo);

	public OMRequest newReconfigurationRequest (OMMonitoringInfo monitoringInfo) {
			return new OMRequest(pickReconfiguration(monitoringInfo));
	}

	public Operator getOperator() {
		return operator;
	}
}
