package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.dsp.Application;

public class ApplicationManager {

	protected Application application;

	public ApplicationManager (Application application) {
		this.application = application;
	}

	public Application getApplication() {
		return application;
	}
}
