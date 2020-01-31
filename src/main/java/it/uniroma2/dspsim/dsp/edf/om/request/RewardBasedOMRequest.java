package it.uniroma2.dspsim.dsp.edf.om.request;

import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.LinkedList;
import java.util.List;

public class RewardBasedOMRequest extends OMRequest {

	/** Requested reconfiguration, each with a preference score/value. */
	protected List<Tuple2<Reconfiguration,ReconfigurationScore>> scoredReconfigurations;

	/** Score given to the no-reconfiguration scenario. */
	protected ReconfigurationScore noReconfigurationScore;

	public RewardBasedOMRequest(Reconfiguration reconfiguration, ReconfigurationScore reconfScore, ReconfigurationScore noReconfScore) {
		scoredReconfigurations = new LinkedList<>();
		scoredReconfigurations.add(new Tuple2<>(reconfiguration, reconfScore));
		this.noReconfigurationScore = noReconfScore;
	}

	public void addRequestedReconfiguration (Reconfiguration reconfiguration, ReconfigurationScore reconfigurationScore)
	{
		scoredReconfigurations.add(new Tuple2<>(reconfiguration, reconfigurationScore));
	}

	public boolean isEmpty() {
		return scoredReconfigurations == null || scoredReconfigurations.isEmpty();
	}

	public List<Tuple2<Reconfiguration,ReconfigurationScore>> getScoredReconfigurations()
	{
		return scoredReconfigurations;
	}

	public ReconfigurationScore getNoReconfigurationScore() { return noReconfigurationScore; }


	@Override
	public Reconfiguration getRequestedReconfiguration() {
		/* base implementation */
		return scoredReconfigurations.get(0).getK();
	}
}
