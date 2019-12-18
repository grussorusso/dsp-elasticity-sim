package it.uniroma2.dspsim.stats.tracker;

import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.Metric;
import it.uniroma2.dspsim.stats.samplers.Sampler;
import it.uniroma2.dspsim.utils.KeyValueStorage;

import java.util.ArrayList;
import java.util.List;

public abstract class Tracker implements ITracker {
    private String id;
    private List<Sampler> samplers;
    private KeyValueStorage<String, Metric> metrics;

    // if tracker.startTrack() has not been called this flag is false and tracker will not track values
    private boolean started;

    public Tracker(String id) {
        this.id = id;
        this.samplers = new ArrayList<>();
        this.metrics = new KeyValueStorage<>();

        this.started = false;
    }

    public void addSampler(Sampler sampler) {
        if (!samplers.contains(sampler)) {
            samplers.add(sampler);
            for (Metric m : metrics.getAll()) {
                m.addSampler(sampler);
            }
        }
    }

    protected void addMetric(Metric metric) {
        if (metrics.getValue(metric.getId()) == null) {
            for (Sampler s : samplers) {
                metric.addSampler(s);
            }
            this.metrics.addKeyValue(metric.getId(), metric);
        }
    }

    protected void updateMetric(String metricID, Number value) {
        if (value instanceof Double)
            metrics.getValue(metricID).update((double) value);
        else if (value instanceof Integer)
            metrics.getValue(metricID).update((int) value);
        else
            throw new IllegalArgumentException("Value number type not supported");
    }

    protected String buildMetricID(String metricID) {
        return this.id + "_" + metricID;
    }

    @Override
    public void startTracking() {
        this.initTracker();
        this.started = true;
    }

    /**
     * File tracking, this protected method must be called from Tracker generalization to exec tracking dump
     */
    protected void track(Metric metric) {
        metric.sample();
    }

    /**
     * ABSTRACT METHODS
     */
    protected abstract void initTracker();

    /**
     * GETTERS
     */

    public String getId() {
        return id;
    }

    public boolean isStarted() {
        return started;
    }

    protected Metric getMetric(String metricID) {
        return this.metrics.getValue(metricID);
    }
}
