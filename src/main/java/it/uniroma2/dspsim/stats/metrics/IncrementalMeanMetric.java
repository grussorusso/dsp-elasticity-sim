package it.uniroma2.dspsim.stats.metrics;

public class IncrementalMeanMetric extends Metric {
    private long count;
    private double mean;

    public IncrementalMeanMetric(String id) {
        super(id);
        this.count = 0L;
        this.mean = 0.0;
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        this.count++;
        this.mean += ((realValue - this.mean) / this.count);
    }

    @Override
    public String dumpValue() {
        return Double.toString((double) this.getValue());
    }

    @Override
    public Number getValue() {
        return this.mean;
    }
}
