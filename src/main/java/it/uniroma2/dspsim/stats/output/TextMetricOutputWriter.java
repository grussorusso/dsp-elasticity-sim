package it.uniroma2.dspsim.stats.output;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.MetricSampleInfo;

public class TextMetricOutputWriter extends FileMetricOutputWriter {

    public TextMetricOutputWriter(boolean appendMode) {
        super(appendMode);
    }

    @Override
    protected String buildLine(Object... args) {
        // concatenate args by space
        StringBuilder line = new StringBuilder();
        for (int i = 0; i <  args.length - 1; i++)
            line.append(args[i]).append(" ");
        line.append(args[args.length - 1]);
        return line.toString();
    }

    @Override
    protected boolean shouldWrite(Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {
        return true;
    }

    @Override
    protected void fileCreationCallback(String filename, Metric metric, MetricSampleInfo metricSampleInfo, Object... args) {

    }
}
