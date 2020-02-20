package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;

import java.util.HashMap;
import java.util.Map;

public abstract class ApplicationManager {

	protected Application application;
	protected double sloLatency;

	public ApplicationManager(Application application, double sloLatency)  {
		this.application = application;
		this.sloLatency = sloLatency;
	}

	public Application getApplication() {
		return application;
	}


	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {

		Map<OperatorManager, OMRequest> omRequests = pickOMRequests(omMonitoringInfo, operatorManagers);
		return plan(omRequests, omMonitoringInfo);
	}

	protected Map<OperatorManager, OMRequest> pickOMRequests (Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															  Map<Operator, OperatorManager> operatorManagers) {
		Map<OperatorManager, OMRequest> omRequests = new HashMap<>();
		for (Operator op : application.getOperators()) {
			OMMonitoringInfo operatorMonitoringInfo = omMonitoringInfo.get(op);
			OperatorManager om = operatorManagers.get(op);
			OMRequest req = om.pickReconfigurationRequest(operatorMonitoringInfo);
			omRequests.put(om, req);
		}

		return omRequests;
	}

	abstract protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														   Map<Operator, OMMonitoringInfo> omMonitoringInfo);

	protected Map<Operator, Reconfiguration> acceptAll(Map<OperatorManager, OMRequest> omRequestMap) {
		Map<Operator, Reconfiguration> reconfigurations = new HashMap<>(omRequestMap.size());

		for (OperatorManager om : omRequestMap.keySet()) {
			OMRequest req = omRequestMap.get(om);
			Reconfiguration rcf = req.getRequestedReconfiguration();
			if (rcf.isReconfiguration()) {
				reconfigurations.put(om.getOperator(), rcf);
			}
		}

		return reconfigurations;
	}

	protected boolean isAppSLOViolated(Map<Operator, Double> opResponseTime) {
		return application.endToEndLatency(opResponseTime)  > sloLatency;
	}

}
