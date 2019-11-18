package it.uniroma2.dspsim.stats.metrics;

import it.uniroma2.dspsim.stats.samplers.Sampler;

import java.util.HashMap;

public abstract class Metric {

	protected String id;

	// metric samplers map
	protected HashMap<String, Sampler> samplers;

	public Metric(String id) {
		this.id = id;
		samplers = new HashMap<>();
	}

	// add sampler to sampler' map
	public void addSampler(Sampler sampler) {
		if (this.samplers.containsKey(sampler.getId()))
			throw new IllegalArgumentException(
					String.format("%s -> id already used (%s)", this.getClass().getName(), sampler.getId()));

		this.samplers.put(sampler.getId(), sampler);
		// add this metric to sampler
		sampler.addMetricSampleInfo(this);
	}

	// remove sampler to sampler's map
	public void removeSampler(String id) {
		if (this.samplers.containsKey(id)) {
			this.samplers.get(id).getMetricSampleInfo().removeKeyValue(this.getId());
			this.samplers.remove(id);
		}
	}

	// for each sampler, sample metric in according to sampler's policy
	public void sample(long simulationTime) {
		for (String key : this.samplers.keySet()) {
			this.samplers.get(key).sample(this, simulationTime);
		}
	}

	abstract public void update (Integer intValue);

	abstract public void update (Double realValue);

	abstract public String dumpValue();

	abstract public Number getValue();

	@Override
	public String toString() {
		return String.format("%s = %s", id, dumpValue());
	}

	/**
	 * GETTER
	 */

	public String getId() {
		return id;
	}
}
