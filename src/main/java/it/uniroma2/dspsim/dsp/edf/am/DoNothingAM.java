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
