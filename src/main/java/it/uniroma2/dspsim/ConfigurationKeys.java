package it.uniroma2.dspsim;

public abstract class ConfigurationKeys {

	/** Current SLO in terms of application latency (seconds). */
	public static final String SLO_LATENCY_KEY = "dsp.slolatency";

	/** Type of OperatorManager to use. */
	public static final String OM_TYPE_KEY = "edf.om.type";

	/** For ThresholdBasedOM, scaling threshold in (0,1). */
	public static final String OM_THRESHOLD_KEY = "edf.om.threshold";

}
