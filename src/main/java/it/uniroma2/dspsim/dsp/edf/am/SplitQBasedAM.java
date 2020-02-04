package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.ReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.SplitQReconfigurationScore;
import it.uniroma2.dspsim.utils.JointReconfigurationIterator;
import it.uniroma2.dspsim.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SplitQBasedAM extends ApplicationManager {

	private Logger logger = LoggerFactory.getLogger(SplitQBasedAM.class);

	public SplitQBasedAM(Application application, double sloLatency) {
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
		int bestAction[] = new int[0]; /* just to initialize it... */
		Double bestQ = null;
		JointReconfigurationIterator iterator = new JointReconfigurationIterator(N, omRcfCount);
		while (iterator.hasNext()) {
			int a[] = iterator.next();
			double q = evaluateGlobalQ(a, managers, scoredRcfs);
			logger.info("Q({}) = {}", a , q);
			if (bestQ == null || q < bestQ) {
				bestAction = a.clone();
				bestQ = q;
			}
		}

		logger.info("Accepted: {}", bestAction);

		/*
		 * Accept reconfigurations based on best action.
		 */
		Map<Operator, Reconfiguration> acceptedReconfs = new HashMap<>();
		for (i=0; i<N; i++) {
			Reconfiguration rcf = scoredRcfs[i].get(bestAction[i]).getK();
			if (!rcf.isReconfiguration()) {
				continue;
			}
			acceptedReconfs.put(managers.get(i).getOperator(), rcf);
		}

		return acceptedReconfs;
	}

	private double evaluateGlobalQ(int[] a, ArrayList<OperatorManager> oms,
								   ArrayList<Tuple2<Reconfiguration, SplitQReconfigurationScore>>[] scoredRcfs) {
		int N = a.length;

		/* Q_resources */
		double Qres = 0.0;
		for (int i=0; i<N; i++) {
			Qres += scoredRcfs[i].get(a[i]).getV().getqResources();
		}
		Qres /= N; /* normalized based on number of operator */

		/* Q_rcf */
		double futureQrcf = 0.0;
		boolean isRcfJoint = false;
		for (int i=0; i<N; i++) {
			Reconfiguration rcf = scoredRcfs[i].get(a[i]).getK();
			double omFutureQRcf = scoredRcfs[i].get(a[i]).getV().getqReconfiguration();
			if (rcf.isReconfiguration()) {
				omFutureQRcf -= 1.0; // we must subtract immediate cost
				isRcfJoint = true;
			}
			futureQrcf = Math.max(futureQrcf, omFutureQRcf);
		}
		double immediateRcfCost = isRcfJoint? 1.0 : 0.0;
		//double qRcf = futureQrcf + immediateRcfCost;
		double qRcf = immediateRcfCost; // ignore future Rcf

		/* Q_slo */
		Map<Operator,Double> opResponseTime = new HashMap<>();
		Map<Operator,Double> opFutureResponseTime = new HashMap<>();
		for (int i=0; i<N; i++) {
			double immediateRespTime = scoredRcfs[i].get(a[i]).getV().getImmediateRespTime();
			double futureAvgRespTime = scoredRcfs[i].get(a[i]).getV().getAvgFutureRespTime();
			Operator op = oms.get(i).getOperator();
			opResponseTime.put(op, immediateRespTime);
			opFutureResponseTime.put(op, futureAvgRespTime);
		}

		boolean immediateSLOViolation = isAppSLOViolated(opResponseTime);
		boolean futureSLOViolation = isAppSLOViolated(opFutureResponseTime);

		final double gamma = 0.99;// TODO: get gamma from conf
		double Qslo = 0.0;
		if (immediateSLOViolation)
			Qslo += 1.0;
		if (futureSLOViolation)
			Qslo += gamma/(1.0-gamma);

		// TODO: get weights from conf
		final double w=0.33;
		logger.info("Q({}): res={}, rcf={}, slo={}", a, Qres, qRcf, Qslo);
		return w*Qres + w*qRcf + w*Qslo;
	}


}
