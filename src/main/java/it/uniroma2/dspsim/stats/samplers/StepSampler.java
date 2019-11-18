package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.stats.metrics.Metric;

public class StepSampler extends Sampler {
    private final String STEP_COUNTER_KEY = "counter";

    private int step;

    public StepSampler(String id, int step) {
        super(id);
        this.step = step;
    }

    @Override
    public void addMetricSampleInfo(Metric metric) {
        String filename = String.format("%s_step_%d_sampling.csv", metric.getId(), step);
        MetricSampleInfo metricSampleInfo = new MetricSampleInfo(metric.getId(), filename);
        metricSampleInfo.getMetadata().addKeyValue(STEP_COUNTER_KEY, -1);
        this.getMetricSampleInfo().addKeyValue(metric.getId(), metricSampleInfo);
    }

    @Override
    public void sample(Metric metric, long simulationTime) {
        int counter = (int) this.getMetricSampleInfo().getValue(metric.getId()).getMetadata().getValue(STEP_COUNTER_KEY);
        counter += 1;
        if (counter % step == 0)
            this.dump((double) simulationTime / (24.0 * 60.0), metric);

        this.getMetricSampleInfo().getValue(metric.getId()).getMetadata().updateValue(STEP_COUNTER_KEY, counter);
    }
}
