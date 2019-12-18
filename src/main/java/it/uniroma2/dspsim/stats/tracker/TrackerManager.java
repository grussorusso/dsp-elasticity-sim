package it.uniroma2.dspsim.stats.tracker;

import it.uniroma2.dspsim.stats.samplers.Sampler;
import it.uniroma2.dspsim.utils.KeyValueStorage;

public class TrackerManager {

    private static final String MEMORY_TRACKER = "Memory";
    private static final String CPU_TRACKER = "CPU";
    private static final String TIME_TRACKER = "Time";

    private String id;
    private KeyValueStorage<String, Tracker> trackers;

    private TrackerManager(String id) {
        this.id = id;
        this.trackers = new KeyValueStorage<>();
    }

    public void startTracking() {
        for (Tracker tracker : this.trackers.getAll()) {
            tracker.startTracking();
        }
    }

    public void track() {
        for (Tracker tracker : this.trackers.getAll()) {
            tracker.track();
        }
    }

    public static class Builder {

        private TrackerManager trackerManager;

        public Builder(String id) {
            this.trackerManager = new TrackerManager(id);
        }

        /**
         * BUILD TRACKERS
         */

        public Builder trackMemory() {
            return this;
        }

        public Builder trackCPU() {
            return this;
        }

        public Builder trackExecTime() {
            this.trackerManager.trackers.addKeyValue(TIME_TRACKER, new TimeTracker(this.trackerManager.id));
            return this;
        }

        public Builder customTracker(String trackerID, Tracker tracker) {
            this.trackerManager.trackers.addKeyValue(trackerID, tracker);
            return this;
        }

        /**
         * ADD SAMPLERS
         */
        public Builder addSampler(Sampler sampler) {
            for (Tracker t : this.trackerManager.trackers.getAll()) {
                t.addSampler(sampler);
            }
            return this;
        }

        /**
         * BUILD TRUCKER MANAGER
         */
        public TrackerManager build() {
            return this.trackerManager;
        }
    }

}
