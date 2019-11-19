package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;

public interface MetricOutputWriter {
    void write(Metric metric, MetricSampleInfo metricSampleInfo, Object... args);
}
