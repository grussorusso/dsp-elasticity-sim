package it.uniroma2.dspsim.stats;

public class CountMetric extends Metric {

	private long count = 0;

	public CountMetric(String id, boolean semiLogSampling, int semiLogStep) {
		super(id, semiLogSampling, semiLogStep);
	}

	public CountMetric (String id) {
		this(id, false, 0);
	}

	@Override
	public void update(Integer intValue) {
		count += intValue;
	}

	@Override
	public void update(Double realValue) {
		throw new RuntimeException("CountMetric does not accept real values!");
	}

	@Override
	public Number getValue() {
		return count;
	}

	@Override
	public String dumpValue() {
		return Long.toString(count);
	}
}
