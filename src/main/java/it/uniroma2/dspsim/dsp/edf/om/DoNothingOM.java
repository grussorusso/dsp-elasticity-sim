package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.BasicOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;

public class DoNothingOM extends OperatorManager {

	public DoNothingOM(Operator operator) {
		super(operator);
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
		return new BasicOMRequest(Reconfiguration.doNothing());
	}
}
