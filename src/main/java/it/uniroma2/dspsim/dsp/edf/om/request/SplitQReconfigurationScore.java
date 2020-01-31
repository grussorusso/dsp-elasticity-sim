package it.uniroma2.dspsim.dsp.edf.om.request;

public class SplitQReconfigurationScore extends ReconfigurationScore {

	private double qResources;
	private double qReconfiguration;
	private double qRespTime;

	public SplitQReconfigurationScore(double qRes, double qRcf, double qRespTime)
	{
		this.qResources = qRes;
		this.qReconfiguration = qRcf;
		this.qRespTime = qRespTime;
	}

	public double getqResources() {
		return qResources;
	}

	public double getqReconfiguration() {
		return qReconfiguration;
	}

	public double getqRespTime() {
		return qRespTime;
	}

	@Override
	public String toString() {
		return String.format("%.3f-%.3f-%.3f", qResources,qReconfiguration,qRespTime);
	}
}
