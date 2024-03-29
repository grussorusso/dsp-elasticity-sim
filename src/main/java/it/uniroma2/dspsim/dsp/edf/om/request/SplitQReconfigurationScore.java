package it.uniroma2.dspsim.dsp.edf.om.request;

public class SplitQReconfigurationScore extends ReconfigurationScore {

	private double qResources;
	private double qReconfiguration;
	private double avgFutureRespTime;
	private double immediateRespTime;

	public SplitQReconfigurationScore(double qRes, double qRcf, double immediateRespTime, double avgFutureRespTime)
	{
		this.qResources = qRes;
		this.qReconfiguration = qRcf;
		this.avgFutureRespTime = avgFutureRespTime;
		this.immediateRespTime = immediateRespTime;
	}

	public double getqResources() {
		return qResources;
	}

	public double getqReconfiguration() {
		return qReconfiguration;
	}

	public double getAvgFutureRespTime() {
		return avgFutureRespTime;
	}

	public double getImmediateRespTime() {
		return immediateRespTime;
	}

	@Override
	public String toString() {
		return String.format("res=%.2f,rcf=%.2f,r=%.3f,avgR=%.3f", qResources,qReconfiguration, immediateRespTime, avgFutureRespTime);
	}
}
