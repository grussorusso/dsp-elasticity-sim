package it.uniroma2.dspsim.dsp.edf.om.request;

public class QBasedReconfigurationScore extends ReconfigurationScore {

	private double value;

	public QBasedReconfigurationScore(double v)
	{
		this.value = v;
	}

	public double getScore()
	{
		return value;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}
}
