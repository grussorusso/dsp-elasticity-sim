package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.ReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.SplitQReconfigurationScore;
import it.uniroma2.dspsim.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SecondOpportunityAM extends ApplicationManager {

	private Logger logger = LoggerFactory.getLogger(SecondOpportunityAM.class);


	public SecondOpportunityAM(Application application, double sloLatency) {
		super(application, sloLatency);
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
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {
		Map<OperatorManager, OMRequest> omRequestMap = pickOMRequests(omMonitoringInfo, operatorManagers);

		Map<OperatorManager, Reconfiguration[]> requestedReconfs = new HashMap<>();

		/* OMs */
		ArrayList<OperatorManager> managers = new ArrayList<>();

		int i = 0;
		for (OperatorManager om : omRequestMap.keySet()) {
			managers.add(om);

			RewardBasedOMRequest request = getRequest(omRequestMap.get(om));
			Reconfiguration reconfs[] = new Reconfiguration[2];

			int count = 0;
			for (Tuple2<Reconfiguration, ReconfigurationScore> scoredRcf : request.getScoredReconfigurations()) {
				Reconfiguration rcf = scoredRcf.getK();
				if (rcf.isReconfiguration())
					logger.info("{} proposed {} ", om.getOperator(), rcf);
				if (count <= 1) {
					reconfs[count] = scoredRcf.getK();
					++count;
				} else {
					break;
				}
			}


			if (count > 0)
				requestedReconfs.put(om, reconfs);
			i++;
		}

		boolean reconfiguring = false;

		for (OperatorManager om : requestedReconfs.keySet()) {
			Reconfiguration rcf = requestedReconfs.get(om)[0];
			if (rcf != null && rcf.isReconfiguration())
				reconfiguring = true;
		}

		Map<Operator, Reconfiguration> acceptedReconfs = new HashMap<>();
		if (!reconfiguring) {
			/* nothing to accept */
		} else {
			/* accept possibly secondary actions */
			for (OperatorManager om : requestedReconfs.keySet()) {
				Reconfiguration rcf = requestedReconfs.get(om)[0];
				if (rcf == null )
					continue;
				if (rcf.isReconfiguration()) {
					acceptedReconfs.put(om.getOperator(), rcf);
					continue;
				}

				rcf = requestedReconfs.get(om)[1]; /* secondary request */
				if (rcf != null && rcf.isReconfiguration())
					acceptedReconfs.put(om.getOperator(), rcf);
			}
		}

		if (!acceptedReconfs.isEmpty())
			logger.info("Accepted: {}", acceptedReconfs);
		return acceptedReconfs;
	}


	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														Map<Operator, OMMonitoringInfo> omMonitoringInfoMap) {
		throw new RuntimeException("This method should never be called!");
	}
}
