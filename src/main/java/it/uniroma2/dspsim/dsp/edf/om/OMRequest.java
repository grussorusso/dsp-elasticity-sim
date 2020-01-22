package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Reconfiguration;

import java.util.ArrayList;
import java.util.List;

public class OMRequest {


	/** Requested reconfiguration, each with a preference score/value. */
	private List<RequestedReconfiguration> scoredReconfigurations;

	/** Score given to the no-reconfiguration scenario. */
	private double noReconfigurationScore;

	/** Constructor used by default OM implementations,
	 * which always request a single reconfiguration.
	 */
	public OMRequest (Reconfiguration reconfiguration) {
		scoredReconfigurations = new ArrayList<>(1);
		scoredReconfigurations.add(new RequestedReconfiguration(reconfiguration));
		this.noReconfigurationScore = 0.0;
	}

	public OMRequest (Reconfiguration reconfiguration, double reconfScore, double noReconfScore) {
		scoredReconfigurations = new ArrayList<>(1);
		scoredReconfigurations.add(new RequestedReconfiguration(reconfiguration, reconfScore));
		this.noReconfigurationScore = noReconfScore;
	}

	public OMRequest () {
		this.scoredReconfigurations = new ArrayList<>();
		this.noReconfigurationScore = 0.0;
	}

	public boolean isEmpty() {
		return scoredReconfigurations == null || scoredReconfigurations.isEmpty();
	}

	public List<RequestedReconfiguration> getScoredReconfigurations()
	{
		return scoredReconfigurations;
	}

	public double getNoReconfigurationScore() { return noReconfigurationScore; }


	static public class RequestedReconfiguration {
		private Reconfiguration reconfiguration;
		private double score = 0.0;

		public RequestedReconfiguration (Reconfiguration reconfiguration) {
			this(reconfiguration, 0.0);
		}

		public RequestedReconfiguration (Reconfiguration reconfiguration, double score) {
			this.reconfiguration = reconfiguration;
			this.score = score;
		}

		public Reconfiguration getReconfiguration()
		{
			return reconfiguration;
		}

		public double getScore() {
			return score;
		}
	}
}
