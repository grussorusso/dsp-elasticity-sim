package it.uniroma2.dspsim.stats.metrics;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class RealValuedMetric extends Metric {

	private double sum = 0.0;
	private long count = 0;

	private PrintWriter valuesSampler = null;
	private PrintWriter avgSampler = null;

	public RealValuedMetric(String id) {
		this(id, false, false);
	}

	public RealValuedMetric(String id, boolean sampleAllValues, boolean sampleAvg) {
		super(id);

		final String outDir = Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY,".");
		final String normalizedId = id.replace(' ', '_');
		if (sampleAllValues) {
			final String filename = String.format("%s/%s-values.txt", outDir, normalizedId);

			try {
				this.valuesSampler = new PrintWriter(filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		if (sampleAvg) {
			final String filename = String.format("%s/%s-avg.txt", outDir, normalizedId);

			try {
				this.avgSampler = new PrintWriter(filename);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void update(Integer intValue) {
		update((double)intValue);
	}

	@Override
	public void update(Double realValue) {
		sum += realValue;
		count++;

		if (this.valuesSampler != null)
			this.valuesSampler.println(realValue);
		if (this.avgSampler != null)
			this.avgSampler.println(getAvg());
	}

	public double getAvg() {
		return sum/count;
	}

	@Override
	public String dumpValue() {
		return Double.toString(getAvg());
	}

	@Override
	public void close()
	{
		if (this.valuesSampler != null)
			this.valuesSampler.close();
		if (this.avgSampler != null)
			this.avgSampler.close();
	}
}
