package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.ReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.SplitQReconfigurationScore;
import it.uniroma2.dspsim.utils.JointActionIterator;
import it.uniroma2.dspsim.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
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
		/* Number of OMs. */
		final int N = omRequestMap.size();
		/* Count of reconf. requested by each OM. */
		int omRcfCount[] = new int[N];
		/* OMs */
		ArrayList<OperatorManager> managers = new ArrayList<>(N);
		ArrayList<Tuple2<Reconfiguration, SplitQReconfigurationScore>>[] scoredRcfs = new ArrayList[N];

		int i = 0;
		for (OperatorManager om : omRequestMap.keySet()) {
			omRcfCount[i] = 0;
			managers.add(om);
			scoredRcfs[i] = new ArrayList<>();

			RewardBasedOMRequest request = getRequest(omRequestMap.get(om));

			/* no reconfiguration is always the 0-th entry */
			scoredRcfs[i].add(new Tuple2<>(Reconfiguration.doNothing(), getScore(request.getNoReconfigurationScore())));

			for (Tuple2<Reconfiguration, ReconfigurationScore> scoredRcf : request.getScoredReconfigurations()) {
				Reconfiguration rcf = scoredRcf.getK();
				if (!rcf.isReconfiguration())
					continue;
				omRcfCount[i] += 1;
				SplitQReconfigurationScore score = getScore(scoredRcf.getV());
				logger.info("{} proposed {} w score {} ({}) ", om.getOperator(),
						rcf, score, request.getNoReconfigurationScore());
				scoredRcfs[i].add(new Tuple2<>(rcf, score));
			}
			i++;
		}

		/*
		 * Enumerate possible joint actions.
		 * a = <i_1, i_2, i_3, ..., i_N>, where i_j=0 means no reconfiguration
		 */
		int bestAction[];
		Double bestQ = null;
		JointActionIterator iterator = new JointActionIterator(N, omRcfCount);
		while (iterator.hasNext()) {
			int a[] = iterator.next();
			double q = evaluateGlobalQ(a, scoredRcfs);
			logger.info("Q({}) = {}", a , q);
			if (bestQ == null || q < bestQ) {
				bestAction = a.clone();
				bestQ = q;
			}
		}

		/*
		 * Accept reconfigurations based on best action.
		 */
		// TODO
		return acceptAll(omRequestMap); // TODO
	}

	private double evaluateGlobalQ(int[] a, ArrayList<Tuple2<Reconfiguration, SplitQReconfigurationScore>>[] scoredRcfs) {
		int N = a.length;

		/* Q_resources */
		double Qres = 0.0;
		for (int i=0; i<N; i++) {
			Qres += scoredRcfs[i].get(a[0]).getV().getqResources();
		}

		/* Q_rcf */
		double futureQrcf = 0.0;
		boolean isRcfJoint = false;
		for (int i=0; i<N; i++) {
			Reconfiguration rcf = scoredRcfs[i].get(a[0]).getK();
			double omFutureQRcf = scoredRcfs[i].get(a[0]).getV().getqReconfiguration();
			if (rcf.isReconfiguration()) {
				omFutureQRcf -= -1.0; // we must subtract immediate cost
				isRcfJoint = true;
			}
			futureQrcf = Math.max(futureQrcf, omFutureQRcf);
		}
		double immediateRcfCost = isRcfJoint? 1.0 : 0.0;
		double qRcf = futureQrcf + immediateRcfCost;

		/* Q_slo */
		// TODO: get gamma from conf
		final double gamma = 0.99;

		Collection<ArrayList<Operator>> paths = application.getAllPaths();
		// TODO: we need a mapping from Operator to its scores...

		boolean immediateSLOViolation = false; // TODO
		boolean futureSLOViolation = false; // TODO
		double Qslo = 0.0;
		if (immediateSLOViolation)
			Qslo += 1.0;
		if (futureSLOViolation)
			Qslo += gamma/(1.0-gamma);

		// TODO: get weights from conf
		final double w=0.33;
		return w*Qres + w*qRcf + w*Qslo;
	}


}
