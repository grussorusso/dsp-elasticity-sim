package it.uniroma2.dspsim.stats.metrics;


import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;


public class CpuMetric extends Metric {
    private double cpuAvg;
    private long count;

    public CpuMetric(String id) {
        super(id);
        this.cpuAvg = 0.0;
        this.count = 0L;
    }

    @Override
    public void update(Integer intValue) {
        this.update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        OperatingSystemMXBean osInfo = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);

        this.count++;

        this.cpuAvg += ((osInfo.getProcessCpuLoad() - this.cpuAvg) / (double) this.count);
    }

    @Override
    public String dumpValue() {
        return String.format("%s", Double.toString((double) getValue()));
    }

    @Override
    public Number getValue() {
        return this.cpuAvg;
    }
}
