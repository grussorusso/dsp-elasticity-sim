package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.ReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.SplitQReconfigurationScore;
import it.uniroma2.dspsim.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SplitQBasedAM extends ApplicationManager {

	private Logger logger = LoggerFactory.getLogger(SplitQBasedAM.class);

	public SplitQBasedAM(Application application) {
		super(application);
	}

	private RewardBasedOMRequest getRequest (OMRequest request) {
		OMRequest req = request;
		if (!(req instanceof RewardBasedOMRequest)) {
			logger.error("Unsupported OMRequest: {}", req.getClass().toString());
			throw new RuntimeException("Unsupported OMRequest!");
		}
		return (RewardBasedOMRequest)req;
	}

	private SplitQReconfigurationScore getScore (ReconfigurationScore score) {
		if (!(score instanceof SplitQReconfigurationScore)) {
			logger.error("Unsupported ReconfigurationScore: {}", score.getClass().toString());
			throw new RuntimeException("Unsupported ReconfigurationScore!");
		}
		return (SplitQReconfigurationScore) score;
	}

	@Override
	protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap) {
		for (OperatorManager om : omRequestMap.keySet()) {
			RewardBasedOMRequest request = getRequest(omRequestMap.get(om));
			if (request.isEmpty())
				continue;

			for (Tuple2<Reconfiguration, ReconfigurationScore> scoredRcf : request.getScoredReconfigurations()) {
				Reconfiguration rcf = scoredRcf.getK();
				if (!rcf.isReconfiguration())
					continue;
				SplitQReconfigurationScore score = getScore(scoredRcf.getV());
				logger.info("{} proposed {} w score {} ({}) ", om.getOperator(),
						rcf, score, request.getNoReconfigurationScore());
			}
		}

		return acceptAll(omRequestMap); // TODO
	}

}
