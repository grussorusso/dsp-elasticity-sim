package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;

import java.util.Map;

public class DoNothingAM extends ApplicationManager {

	public DoNothingAM(Application application, double sloLatency) {
		super(application, sloLatency);
	}

	protected Map<Operator, Reconfiguration> plan (Map<OperatorManager, OMRequest> omRequestMap,
												   Map<Operator, OMMonitoringInfo> omMonitoringInfoMap)
	{
		return acceptAll(omRequestMap);
	}

}
