package it.uniroma2.dspsim.stats.metrics;

public class MeanMetric extends Metric {

    private Metric sum;
    private Metric count;

    public MeanMetric(String id, Metric sum, Metric count) {
        super(id);
        if (sum instanceof CountMetric || sum instanceof RealValuedCountMetric) {
            this.sum = sum;
            this.count = count;
        } else {
            throw new IllegalArgumentException("Metric sum must be CountMetric or RealValuedCountMetric");
        }
    }

    @Override
    public void update(Integer intValue) {
        this.sum.update(intValue);
        this.count.update(1);
    }

    @Override
    public void update(Double realValue) {
        this.sum.update(realValue);
        this.count.update(1);
    }

    @Override
    public String dumpValue() {
        if (this.sum instanceof RealValuedCountMetric)
            return Double.toString((double) this.getValue());
        else
            return Long.toString((long) this.getValue());
    }

    @Override
    public Number getValue() {
        if (this.sum instanceof RealValuedCountMetric)
            return (double) this.sum.getValue() / this.count.getValue().doubleValue();
        else
            return (long) this.sum.getValue() / (long) this.count.getValue();
    }

    /**
     * GETTER
     */

    public Metric getSum() {
        return sum;
    }

    public Metric getCount() {
        return count;
    }
}
