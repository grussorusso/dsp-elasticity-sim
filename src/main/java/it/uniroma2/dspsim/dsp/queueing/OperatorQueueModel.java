package it.uniroma2.dspsim.dsp.queueing;

import java.util.Random;

public interface OperatorQueueModel {

	double responseTime (double arrivalRate, double speedup);
	double utilization (double arrivalRate, double speedup);

	double getServiceTimeMean();

	OperatorQueueModel getApproximateModel (Random r);
}
