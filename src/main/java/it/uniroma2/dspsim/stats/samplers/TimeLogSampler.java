package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.stats.metrics.Metric;

public class TimeLogSampler extends Sampler {
    private final String TIME_COUNTER_KEY = "counter";

    private int timeMultiplier;

    private int logBase;

    private int startTime;
    private long stopTime;

    public TimeLogSampler(String id, int timeMultiplier, int logBase) {
        super(id);
        this.timeMultiplier = timeMultiplier;
        this.logBase = logBase;

        // TODO configure it or create a simulation time mapper to make time period dynamic and changeable
        this.startTime = 0;
        this.stopTime = 525533 * timeMultiplier;
    }

    @Override
    public void addMetricSampleInfo(Metric metric) {
        String filename = String.format("%s_time_multiplier_%d_log_sampling.csv", metric.getId(), this.timeMultiplier);
        MetricSampleInfo metricSampleInfo = new MetricSampleInfo(metric.getId(), filename);
        // add time counter to metric sample info metadata
        // in order to keep track of time elapsed and compute logarithmic time
        metricSampleInfo.getMetadata().addKeyValue(TIME_COUNTER_KEY, 0.0);

        this.getMetricSampleInfo().addKeyValue(metric.getId(), metricSampleInfo);
    }

    @Override
    public void sample(Metric metric, long simulationTime) {
        double timeElapsed = ((double) (simulationTime * timeMultiplier) / (double) this.stopTime);

        double prevValue = (double) this.getMetricSampleInfo().getValue(metric.getId())
                .getMetadata().getValue(TIME_COUNTER_KEY);

        double value = Math.pow(timeElapsed, logBase) * ((stopTime - startTime) + startTime);

        if (Math.getExponent(value) - Math.getExponent(prevValue) >= 1) {
            // TODO print metric to file
            this.dump((double) (simulationTime * timeMultiplier) / (double) (24 * 60 * 60), metric);
            // update last time value
            this.getMetricSampleInfo().getValue(metric.getId())
                    .getMetadata().updateValue(TIME_COUNTER_KEY, value);
        }
    }
}
