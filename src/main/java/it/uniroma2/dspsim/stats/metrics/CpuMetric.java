package it.uniroma2.dspsim.stats.metrics;


import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;


public class CpuMetric extends Metric {
    private double cpu;

    private double cpuAvg;
    private long count;

    public CpuMetric(String id) {
        super(id);
        this.cpu = 0L;
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
        this.cpu = osInfo.getProcessCpuLoad();

        this.count++;

        this.cpuAvg += ((this.cpu - this.cpuAvg) / this.count);
    }

    @Override
    public String dumpValue() {
        return String.format("%s %s", Double.toString(cpu) , Double.toString(cpuAvg));
    }

    @Override
    public Number getValue() {
        return cpu;
    }
}
