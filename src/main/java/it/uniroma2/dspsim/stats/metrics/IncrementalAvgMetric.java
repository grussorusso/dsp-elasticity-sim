package it.uniroma2.dspsim.stats.metrics;

public class IncrementalAvgMetric extends Metric {
    private long count;
    private double avg;

    public IncrementalAvgMetric(String id) {
        super(id);
        this.count = 0L;
        this.avg = 0.0;
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        this.count++;
        this.avg += ((realValue - this.avg) / this.count);
    }

    @Override
    public String dumpValue() {
        return Double.toString((double) this.getValue());
    }

    @Override
    public Number getValue() {
        return this.avg;
    }
}
