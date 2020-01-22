package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PDSBasedAM extends ApplicationManager {

	private Logger logger = LoggerFactory.getLogger(PDSBasedAM.class);

	public PDSBasedAM(Application application) {
		super(application);
	}

	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<OperatorManager, OMRequest> omRequestMap) {
		for (OperatorManager om : omRequestMap.keySet()) {
			OMRequest req = omRequestMap.get(om);
			if (req.isEmpty())
				continue;
			for (OMRequest.RequestedReconfiguration rcf : req.getScoredReconfigurations()) {
				if (!rcf.getReconfiguration().isReconfiguration())
					continue;
				logger.info("{} proposed {} w score {} ({}) ", om.getOperator(),
						rcf.getReconfiguration(), rcf.getScore(), req.getNoReconfigurationScore());
			}
		}

		return acceptAll(omRequestMap); // TODO
	}
}
