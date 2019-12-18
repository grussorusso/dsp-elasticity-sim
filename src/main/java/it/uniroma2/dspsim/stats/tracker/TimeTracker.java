package it.uniroma2.dspsim.stats.tracker;

import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.IncrementalAvgMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;

import java.time.Duration;
import java.time.Instant;

public class TimeTracker extends Tracker {

    private static final String AVG_TIME_METRIC = "Avg Time";
    private static final String MAX_TIME_METRIC = "Max Time";
    private static final String MIN_TIME_METRIC = "Min Time";

    private Instant start;
    private Instant prev;

    public TimeTracker(String id) {
        super(id);
        this.addMetric(new IncrementalAvgMetric(buildMetricID(AVG_TIME_METRIC)));
        this.addMetric(new RealValuedCountMetric(buildMetricID(MAX_TIME_METRIC)));
        this.addMetric(new RealValuedCountMetric(buildMetricID(MIN_TIME_METRIC)));
    }

    @Override
    protected void initTracker() {
        this.start = now();
        this.prev = this.start;
    }

    @Override
    public void track() {
        if (isStarted()) {
            Instant now = now();
            Duration elapsedTime = Duration.between(prev, now);

            // update avg time
            this.updateMetric(buildMetricID(AVG_TIME_METRIC), elapsedTime.getNano() / 1.0E6);

            // update max time and min time if necessary
            double maxTime = (double) this.getMetric(buildMetricID(MAX_TIME_METRIC)).getValue();
            if (elapsedTime.getNano() > maxTime) {
                this.updateMetric(buildMetricID(MAX_TIME_METRIC), elapsedTime.getNano() / 1.0E6);
            }

            double minTime = (double) this.getMetric(buildMetricID(MIN_TIME_METRIC)).getValue();
            if (elapsedTime.getNano() < minTime) {
                this.updateMetric(buildMetricID(MIN_TIME_METRIC), elapsedTime.getNano() / 1.0E6);
            }

            // track metrics
            this.track(this.getMetric(buildMetricID(AVG_TIME_METRIC)));
            this.track(this.getMetric(buildMetricID(MAX_TIME_METRIC)));
            this.track(this.getMetric(buildMetricID(MIN_TIME_METRIC)));

            // update last time to now in order to delete statistic updating time from metric sampling
            this.prev = now();
        }
    }

    private Instant now() {
        return Instant.now();
    }
}
