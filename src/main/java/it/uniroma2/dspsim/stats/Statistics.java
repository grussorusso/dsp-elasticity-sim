package it.uniroma2.dspsim.stats;

import it.uniroma2.dspsim.stats.metrics.Metric;

import java.io.*;
import java.util.HashMap;

public class Statistics {

	static private Statistics instance = null;

	static synchronized public Statistics getInstance() {
		if (instance == null) {
			instance = new Statistics();
		}

		return instance;
	}


	/* Registry */
	private HashMap<String, Metric> metrics = new HashMap<>();

	public void registerMetric (Metric m) throws MetricExistsException {
		if (metrics.containsKey(m.getId()))
			throw new MetricExistsException(m.getId());

		metrics.put(m.getId(), m);
	}

	public void registerMetricIfNotExists(Metric m) {
		if (!metrics.containsKey(m.getId())) {
			metrics.put(m.getId(), m);
		}
	}

	public void updateMetric (String id, Integer intValue, String... inner_ids) {
		this.updateMetricThree(id, intValue, inner_ids);
	}


	public void updateMetric (String id, Double realValue, String... inner_ids) {
		this.updateMetricThree(id, realValue, inner_ids);
	}

	/**
	 * Update metric calling metric.update(value) of metric with id = inner_ids.length - 1
	 * With this method is possible to update metric in a metric's three
	 * If inner_ids is not passed to method it will be ignored,
	 * so metric with id 'id' will be updated with 'value'.
	 * WARNING : you have to know all metrics id path
	 * @param id : initial metric id
	 * @param value : updating value
	 * @param inner_ids : inner ids path
	 */
	private void updateMetricThree(String id, Number value, String... inner_ids) {
		if (inner_ids == null || inner_ids.length <= 0)
			if (value instanceof Double)
				metrics.get(id).update((double) value);
			else if (value instanceof Integer)
				metrics.get(id).update((int) value);
			else
				throw new IllegalArgumentException("Value number type not supported");
		else {
			String[] ids = new String[inner_ids.length - 1];
			System.arraycopy(inner_ids, 1, ids, 0, inner_ids.length - 1);
			updateMetricThree(inner_ids[0], value, ids);
		}
	}

	public Metric getMetric(String id) {
		if (this.metrics.containsKey(id))
			return this.metrics.get(id);
		else
			throw new IllegalArgumentException("metric id not known");
	}

	public void dumpAll()
	{
		for (Metric m : metrics.values())
			System.out.println(m.toString());
	}

    // get sample of all metrics in hash map that
	// have been configured to be sampled during simulation
	public void sampleAll(long simulationTime) {
		for (Metric m : metrics.values()) {
			m.sample(simulationTime);
		}
	}

	public class MetricExistsException extends RuntimeException {
		public MetricExistsException(String msg) {
			super(msg);
		}
	}
}
