package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.output.CSVMetricOutputWriter;
import it.uniroma2.dspsim.stats.output.ConsoleMetricOutputWriter;
import it.uniroma2.dspsim.stats.output.MetricOutputWriter;
import it.uniroma2.dspsim.utils.KeyValueStorage;

import java.util.List;

public abstract class Sampler {
    private String id;

    private KeyValueStorage<String, MetricSampleInfo> metricSampleInfo;

    private KeyValueStorage<String, MetricOutputWriter> writers;

    public Sampler(String id) {
        this.id = id;
        this.metricSampleInfo = new KeyValueStorage<>();

        //TODO configure it
        this.writers = new KeyValueStorage<>();

        // TODO sampler builder
        //this.writers.addKeyValue("console", new ConsoleMetricOutputWriter());
    }

    protected void dump(double step, Metric m) {
        for (Object writer : this.writers.getAll()) {
            if (writer instanceof MetricOutputWriter) {
                ((MetricOutputWriter) writer).write(m, this.metricSampleInfo.getValue(m.getId()), step, m.dumpValue());
            }
        }
    }

    public abstract void addMetricSampleInfo(Metric metric);

    public abstract void sample(Metric metric, long simulationTime);

    public String getId() {
        return id;
    }

    public KeyValueStorage<String, MetricSampleInfo> getMetricSampleInfo() {
        return metricSampleInfo;
    }

    public KeyValueStorage<String, MetricOutputWriter> getWriters() {
        return writers;
    }
}
