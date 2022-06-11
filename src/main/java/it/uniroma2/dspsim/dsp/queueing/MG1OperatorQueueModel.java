package it.uniroma2.dspsim.dsp.queueing;

import java.util.Random;

public class MG1OperatorQueueModel implements OperatorQueueModel {

	private double serviceTimeMean = 1.0;

	public double getServiceTimeVariance() {
		return serviceTimeVariance;
	}

	private double serviceTimeVariance = 0.0;

	public MG1OperatorQueueModel(double serviceTimeMean, double serviceTimeVariance) {
		this.serviceTimeMean = serviceTimeMean;
		this.serviceTimeVariance = serviceTimeVariance;
	}

	public double responseTime(double arrivalRate, double speedup)
	{
		final double rho = utilization(arrivalRate, speedup);
		if (rho >= 1.0) {
			return Double.POSITIVE_INFINITY;
		}

		final double st_mean = serviceTimeMean / speedup;
		final double st_var = serviceTimeVariance / (speedup * speedup);
		final double es2 = st_var + st_mean * st_mean;
		final double r = st_mean + arrivalRate / 2.0 * es2 / (1.0 - rho);

		return r;

	}

	public double utilization(double arrivalRate, double speedup)
	{
		final double st_mean = serviceTimeMean / speedup;
		double rho = arrivalRate * st_mean;
		return rho;
	}

	@Override
	public double getServiceTimeMean() {
		return this.serviceTimeMean;
	}


	public OperatorQueueModel getApproximateModel (Random r, double minPercErr, double maxPercErr) {
		double sampledValue = r.nextDouble();
		double sign;
		if (sampledValue > 0.5) {
			sign = 1.0;
			sampledValue = (sampledValue - 0.5)	 * 2.0;
		} else {
			sign = -1.0;
			sampledValue = sampledValue * 2.0;
		}

		double error = sign * (minPercErr + (maxPercErr - minPercErr) * sampledValue);
		double newMean = this.serviceTimeMean + this.serviceTimeMean*error;
		double newVar = newMean*newMean;
		return new MG1OperatorQueueModel(newMean, newVar);
	}
}
