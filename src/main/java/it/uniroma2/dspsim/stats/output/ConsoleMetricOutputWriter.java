package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;

public class ConsoleMetricOutputWriter implements MetricOutputWriter {
    @Override
    public void write(Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        System.out.println(String.format("%s\t->\t%s", metric.getId(), metric.dumpValue()));
    }
}
