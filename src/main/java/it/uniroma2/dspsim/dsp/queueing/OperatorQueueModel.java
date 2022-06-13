package it.uniroma2.dspsim.dsp.queueing;

import java.util.Random;

public interface OperatorQueueModel {

	double responseTime (double arrivalRate, double speedup);
	double utilization (double arrivalRate, double speedup);

	double getServiceTimeMean();
	double getServiceTimeVariance();

	double getArrivalSCV();

	OperatorQueueModel getApproximateModel (Random r, double minPercErr, double maxPercErr);
}
