package it.uniroma2.dspsim.stats.metrics;

public class RealValuedCountMetric extends Metric {

	private double count = 0.0;

	public RealValuedCountMetric(String id) {
		super(id);
	}

	@Override
	public void update(Integer intValue) {
		count += intValue;
	}

	@Override
	public void update(Double realValue) {
		count += realValue;
	}

	@Override
	public Number getValue() {
		return count;
	}

	@Override
	public String dumpValue() {
		return Double.toString(count);
	}
}
