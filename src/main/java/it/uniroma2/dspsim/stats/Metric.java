package it.uniroma2.dspsim.stats;

public abstract class Metric {

	protected String id;
	// semi logarithmic sampling flag
	protected boolean semiLogSampling;
	// if semiLogSampling is true, it indicates after how many time it must be sampled
	protected double semiLogStep;

	public Metric(String id, boolean semiLogSampling, double semiLogStep) {
		this.id = id;
		this.semiLogSampling = semiLogSampling;
		this.semiLogStep = semiLogStep;
	}

	abstract public void update (Integer intValue);

	abstract public void update (Double realValue);

	abstract public String dumpValue();

	abstract public Number getValue();

	@Override
	public String toString() {
		return String.format("%s = %s", id, dumpValue());
	}
}
