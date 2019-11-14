package it.uniroma2.dspsim.stats;

public abstract class Metric {

	protected String id;

	public Metric (String id) {
		this.id = id;
	}

	abstract public void update (Integer intValue);

	abstract public void update (Double realValue);

	abstract public String dumpValue();

	abstract public Number getValue();

	@Override
	public String toString() {
		return String.format("%s = %s", id, dumpValue());
	}
}
