package it.uniroma2.dspsim.stats;

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
		if (metrics.containsKey(m.id))
			throw new MetricExistsException(m.id);

		metrics.put(m.id, m);
	}

	public void updateMetric (String id, Integer intValue) {
		metrics.get(id).update(intValue);
	}

	public void updateMetric (String id, Double realValue) {
		metrics.get(id).update(realValue);
	}

	public void dumpAll()
	{
		for (Metric m : metrics.values())
			System.out.println(m.toString());
	}

	 public class MetricExistsException extends RuntimeException {
		public MetricExistsException(String msg) {
			super(msg);
		}
	}
}
