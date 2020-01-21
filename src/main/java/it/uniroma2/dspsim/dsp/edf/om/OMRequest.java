package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Reconfiguration;

public class OMRequest {


	private RequestedReconfiguration scoredReconfigurations[];

	/** Constructor used by default OM implementations,
	 * which always request a single reconfiguration.
	 */
	public OMRequest (Reconfiguration reconfiguration) {
		scoredReconfigurations = new RequestedReconfiguration[1];
		scoredReconfigurations[0] = new RequestedReconfiguration(reconfiguration);
	}

	public boolean isEmpty() {
		return scoredReconfigurations == null || scoredReconfigurations.length < 1;
	}

	public RequestedReconfiguration[] getScoredReconfigurations()
	{
		return scoredReconfigurations;
	}

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
