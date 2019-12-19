package it.uniroma2.dspsim.stats.metrics;

import java.time.Duration;
import java.time.Instant;

public class TimeMetric extends Metric {
    private Instant prev;
    private long lastElapsedMillis;

    public TimeMetric(String id) {
        super(id);
        this.prev = Instant.now();
    }

    @Override
    public void update(Integer intValue) {
        update((double) intValue);
    }

    @Override
    public void update(Double realValue) {
        Instant now = Instant.now();
        Duration elapsedTime = Duration.between(prev, now);
        lastElapsedMillis = elapsedTime.toMillis();
        this.prev = Instant.now();
    }

    @Override
    public String dumpValue() {
        return Long.toString(lastElapsedMillis);
    }

    @Override
    public Number getValue() {
        return lastElapsedMillis;
    }
}
