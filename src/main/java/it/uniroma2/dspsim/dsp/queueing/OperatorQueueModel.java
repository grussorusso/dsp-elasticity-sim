package it.uniroma2.dspsim.dsp.queueing;

public interface OperatorQueueModel {

	double responseTime (double arrivalRate, double speedup);
	double utilization (double arrivalRate, double speedup);

}
