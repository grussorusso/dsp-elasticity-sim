package it.uniroma2.dspsim.dsp.queueing;

public interface OperatorQueueModel {

	double responseTime (double arrivalRate, int parallelism, double speedup);
	double utilization (double arrivalRate, int parallelism, double speedup);

}
