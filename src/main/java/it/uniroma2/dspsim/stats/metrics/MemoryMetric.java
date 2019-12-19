package it.uniroma2.dspsim.stats.metrics;

public class MemoryMetric extends Metric {

    private double memory;

    private double memoryAvg;
    private long count;

    public MemoryMetric(String id) {
        super(id);
        this.memory = 0L;
        this.memoryAvg = 0.0;
        this.count = 0L;
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        this.memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1.0E6;

        this.count++;

        this.memoryAvg += ((this.memory - this.memoryAvg) / this.count);
    }

    @Override
    public String dumpValue() {
        return String.format("%s %s", Double.toString(memory) , Double.toString(memoryAvg));
    }

    @Override
    public Number getValue() {
        return memory;
    }
}
