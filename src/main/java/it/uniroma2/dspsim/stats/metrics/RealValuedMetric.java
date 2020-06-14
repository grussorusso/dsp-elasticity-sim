package it.uniroma2.dspsim.stats.metrics;

public class RealValuedMetric extends Metric {

	private double sum = 0.0;
	private long count = 0;

	public RealValuedMetric(String id) {
		super(id);
	}

	@Override
	public void update(Integer intValue) {
		update((double)intValue);
	}

	@Override
	public void update(Double realValue) {
		sum += realValue;
		count++;
	}

	public double getAvg() {
		return sum/count;
	}

	@Override
	public String dumpValue() {
		return Double.toString(getAvg());
	}
}
