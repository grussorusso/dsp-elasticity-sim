package it.uniroma2.dspsim.stats;

public class PercentageMetric extends Metric {

    private Metric sum;
    private Metric total;

    public PercentageMetric(String id, Metric sum, Metric total) {
        super(id);
        if ((sum instanceof CountMetric && total instanceof CountMetric) ||
                (sum instanceof RealValuedCountMetric && total instanceof RealValuedCountMetric)) {
            this.sum = sum;
            this.total = total;
        } else {
            throw new IllegalArgumentException("Metric sum and Metric total must be both CountMetric or RealValuedMetric");
        }
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        throw new IllegalArgumentException(String.format("This method can't be called.\n " +
                "You must specify which inner metric you want to update: sum or total.\n" +
                "e.g. updateMetric(%s, value, {%s}) to update sum metric", this.id, this.sum.id));
    }

    @Override
    public String dumpValue() {
        // in both cases get value return double because it's a percentage
        // so normally it is a number in [0, 1] (int division will return 0 in each case)
        // even if it could be grater than 1 if we are applying this metric to a growth, e.g 1.8 equals a growth of 180%
        return Double.toString((double) this.getValue());
    }

    @Override
    public Number getValue() {
        return this.sum.getValue().doubleValue() / this.total.getValue().doubleValue();
    }
}
