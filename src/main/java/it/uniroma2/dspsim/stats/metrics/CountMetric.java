package it.uniroma2.dspsim.stats.metrics;

public class CountMetric extends Metric {

	private long count = 0;

	public CountMetric (String id) {
		super(id);
	}

	@Override
	public void update(Integer intValue) {
		count += intValue;
	}

	@Override
	public void update(Double realValue) {
		throw new RuntimeException("CountMetric does not accept real values!");
	}

	public Number getCount() {
		return count;
	}

	@Override
	public String dumpValue() {
		return Long.toString(count);
	}
}
