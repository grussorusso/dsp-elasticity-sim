package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;

import java.util.HashMap;
import java.util.Map;

public abstract class ApplicationManager {

	protected Application application;

	public ApplicationManager (Application application) {
		this.application = application;
	}

	public Application getApplication() {
		return application;
	}


	public abstract Map<Operator, Reconfiguration> planReconfigurations(Map<OperatorManager, OMRequest> omRequestMap);


	protected Map<Operator, Reconfiguration> acceptAll (Map<OperatorManager, OMRequest> omRequestMap) {
		Map<Operator, Reconfiguration> reconfigurations = new HashMap<>(omRequestMap.size());

		for (OperatorManager om : omRequestMap.keySet()) {
			OMRequest req = omRequestMap.get(om);
			if (req.isEmpty())
				continue;
			Reconfiguration rcf = req.getScoredReconfigurations().get(0).getReconfiguration();
			reconfigurations.put(om.getOperator(), rcf);
		}

		return reconfigurations;
	}
}
