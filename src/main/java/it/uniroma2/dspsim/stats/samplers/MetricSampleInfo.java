package it.uniroma2.dspsim.stats.samplers;

import it.uniroma2.dspsim.utils.KeyValueStorage;

public class MetricSampleInfo {
    private String metricID;
    private String filename;
    private KeyValueStorage<String, Object> metadata;


    public MetricSampleInfo(String metricID, String filename) {
        this.metricID = metricID;
        this.filename = filename;
        this.metadata = new KeyValueStorage<>();
    }

    public String getMetricID() {
        return metricID;
    }

    public String getFilename() {
        return filename;
    }

    public KeyValueStorage<String, Object> getMetadata() {
        return metadata;
    }
}
