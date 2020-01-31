package it.uniroma2.dspsim.dsp.edf.om.request;

import it.uniroma2.dspsim.dsp.Reconfiguration;

public class BasicOMRequest extends OMRequest {

	/** Requested reconfiguration. */
	private Reconfiguration requestedReconfiguration;

	/** Constructor used by default OM implementations,
	 * which always request a single reconfiguration.
	 */
	public BasicOMRequest(Reconfiguration reconfiguration) {
		this.requestedReconfiguration = reconfiguration;
	}

	public boolean isEmpty() {
		return requestedReconfiguration == null;
	}


	@Override
	public Reconfiguration getRequestedReconfiguration() {
		return requestedReconfiguration;
	}
}
