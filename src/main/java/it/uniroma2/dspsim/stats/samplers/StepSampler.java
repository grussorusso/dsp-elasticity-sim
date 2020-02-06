package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.output.TextMetricOutputWriter;

public class StepSampler extends Sampler {
    private final String STEP_COUNTER_KEY = "counter";

    private int step;

    public StepSampler(String id, int step) {
        super(id);
        this.step = step;

        //this.getWriters().addKeyValue("csv", new CSVMetricOutputWriter(true, "Day", "Value"));
        this.getWriters().addKeyValue("dat", new TextMetricOutputWriter(true));
    }

    @Override
    public void addMetricSampleInfo(Metric metric) {
        String filename = String.format("%s/%s_step_%d_sampling_%s.dat",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                metric.getId(), step,
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""));
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

    @Override
    public void sample(Metric metric) {
        int counter = (int) this.getMetricSampleInfo().getValue(metric.getId()).getMetadata().getValue(STEP_COUNTER_KEY);
        counter += 1;
        if (counter % step == 0)
            this.dump(metric);

        this.getMetricSampleInfo().getValue(metric.getId()).getMetadata().updateValue(STEP_COUNTER_KEY, counter);
    }
}
