package it.uniroma2.dspsim.utils.parameter;

public class VariableParameter extends Parameter {
    protected double minValue;
    protected double maxValue;
    protected double multiplier;

    public VariableParameter(double value, double minValue, double maxValue, double multiplier) {
        super(value);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.multiplier = multiplier;
    }

    public void update() {
        if (multiplier > 1) {
            increment();
        } else {
            decrement();
        }
    }

    private void increment() {
        if (this.value == this.maxValue) return;

        this.value *= this.multiplier;
        if (this.value >= this.maxValue) this.value = this.maxValue;
    }

    private void decrement() {
        if (this.value == this.minValue) return;

        this.value *= this.multiplier;
        if (this.value <= this.minValue) this.value = this.minValue;
    }

    public double getMinValue() {
        return minValue;
    }

    public void setMinValue(double minValue) {
        this.minValue = minValue;
    }

    public double getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(double maxValue) {
        this.maxValue = maxValue;
    }

    public double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(double multiplier) {
        this.multiplier = multiplier;
    }
}
