package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;

public class DoNothingOM extends OperatorManager {

	public DoNothingOM(Operator operator) {
		super(operator);
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
		return new OMRequest(Reconfiguration.doNothing());
	}
}
