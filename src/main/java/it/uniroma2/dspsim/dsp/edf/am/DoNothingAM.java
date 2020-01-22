package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;

import java.util.HashMap;
import java.util.Map;

public class DoNothingAM extends ApplicationManager {

	public DoNothingAM(Application application) {
		super(application);
	}

	public Map<Operator, Reconfiguration> planReconfigurations(Map<OperatorManager, OMRequest> omRequestMap)
	{
		return acceptAll(omRequestMap);
	}
}