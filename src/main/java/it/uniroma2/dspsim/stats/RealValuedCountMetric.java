package it.uniroma2.dspsim.stats;

public class RealValuedCountMetric extends Metric {

	private double count = 0.0;

	public RealValuedCountMetric(String id, boolean semiLogSampling, int semiLogStep) {
		super(id, semiLogSampling, semiLogStep);
	}

	public RealValuedCountMetric(String id) {
		this(id, false, 0);
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
