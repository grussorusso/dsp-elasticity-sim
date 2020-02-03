package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;

import java.util.Map;

/**
 * Experimental AM.
 * Adaptation is controlled in a centralized way,
 * requests from any OM are ignored.
 */
public class CentralizedAM extends ApplicationManager {



	public CentralizedAM(Application application) {
		super(application);
		
		computePolicy();
	}

	private void computePolicy() {
	}

	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {

		// TODO: build current state

		// TODO: pick best global action

		// TODO: build map op->reconf based on global action

		return null;
	}

	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap) {
		throw new RuntimeException("This method should never be called!");
	}
}
