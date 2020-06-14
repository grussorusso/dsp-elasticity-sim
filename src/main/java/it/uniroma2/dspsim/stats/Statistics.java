package it.uniroma2.dspsim.stats;

import it.uniroma2.dspsim.stats.metrics.Metric;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

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

	public void updateMetric (String id, Integer intValue) {
		metrics.get(id).update(intValue);
	}


	public void updateMetric (String id, Double realValue) {
		metrics.get(id).update(realValue);
	}

	public Metric getMetric (String id) {
		return this.metrics.get(id);
	}

	public void dumpSorted()
	{
		List<String> lines = new ArrayList<>(metrics.size());
		for (Metric m : metrics.values())
			lines.add(m.toString());

		Collections.sort(lines);

		for (String s : lines)
			System.out.println(s);
	}

	public void dumpAll()
	{
		dumpAll(System.out);
	}

	public void dumpAll(OutputStream out) {
		List<String> lines = new ArrayList<>(metrics.size());
		for (Metric m : metrics.values())
			lines.add(m.toString());

		Collections.sort(lines);

		try {
			for (String l : lines) {
				byte[] bytes = l.getBytes();
				out.write(bytes);
				out.write("\n".getBytes());
			}
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public class MetricExistsException extends RuntimeException {
		public MetricExistsException(String msg) {
			super(msg);
		}
	}
}
