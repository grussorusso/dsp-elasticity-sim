package it.uniroma2.dspsim.stats.metrics;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

public class AvgMemoryMetric extends Metric {
    private static final long MEGABYTE = 1024L * 1024L;

    private double maxMemoryHeapUsageMB;
    private double avgMemoryHeapUsageMB;

    private double maxMemoryNoHeapUsageMB;
    private double avgMemoryNoHeapUsageMB;

    private long count;

    public AvgMemoryMetric(String id) {
        super(id);

        this.maxMemoryHeapUsageMB = 0.0;
        this.avgMemoryHeapUsageMB = 0.0;

        this.maxMemoryNoHeapUsageMB = 0.0;
        this.avgMemoryNoHeapUsageMB = 0.0;

        this.count = 0L;
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        //double memory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (double) MEGABYTE;

        this.count++;

        MemoryMXBean mBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = mBean.getHeapMemoryUsage();

        double heapUsageMB = (double) heap.getUsed() / (double) MEGABYTE;

        this.maxMemoryHeapUsageMB = Math.max(this.maxMemoryHeapUsageMB, heapUsageMB);
        this.avgMemoryHeapUsageMB += ((heapUsageMB - this.avgMemoryHeapUsageMB) / (double) this.count);

        MemoryUsage noHeap = mBean.getNonHeapMemoryUsage();

        double noHeapUsageMB = (double) noHeap.getUsed() / (double) MEGABYTE;

        this.maxMemoryNoHeapUsageMB = Math.max(this.maxMemoryNoHeapUsageMB, noHeapUsageMB);
        this.avgMemoryNoHeapUsageMB += ((noHeapUsageMB - this.avgMemoryNoHeapUsageMB) / (double) this.count);

        //this.memoryAvg += ((memory - this.memoryAvg) / (double) this.count);
    }

    @Override
    public String dumpValue() {
        return String.format("Heap [MB] (max, avg): %s %s, No Heap [MB] (max, avg): %s %s",
                Double.toString(this.maxMemoryHeapUsageMB),  Double.toString(this.avgMemoryHeapUsageMB),
                Double.toString(this.maxMemoryNoHeapUsageMB), Double.toString(this.avgMemoryNoHeapUsageMB));
    }

    @Override
    public Number getValue() {
        return avgMemoryHeapUsageMB;
    }
}
