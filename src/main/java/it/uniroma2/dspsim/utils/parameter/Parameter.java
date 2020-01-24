package it.uniroma2.dspsim.utils.parameter;

public class Parameter {

    protected double value;

    public Parameter(double value) {
        this.value = value;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}
