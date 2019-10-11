package it.uniroma2.dspsim.dsp.queueing;

import it.uniroma2.dspsim.infrastructure.NodeType;

public class MG1OperatorQueueModel implements OperatorQueueModel {

	private double serviceTimeMean = 1.0;
	private double serviceTimeVariance = 0.0;

	public MG1OperatorQueueModel(double serviceTimeMean, double serviceTimeVariance) {
		this.serviceTimeMean = serviceTimeMean;
		this.serviceTimeVariance = serviceTimeVariance;
	}

	public double responseTime(double arrivalRate, int parallelism, double speedup)
	{
		final double rho = utilization(arrivalRate, parallelism, speedup);
		if (rho >= 1.0) {
			return Double.POSITIVE_INFINITY;
		}

		// TODO we assume uniform stream repartition
		final double ratePerReplica = arrivalRate / parallelism;

		final double st_mean = serviceTimeMean / speedup;
		final double st_var = serviceTimeVariance / (speedup * speedup);
		final double es2 = st_var + st_mean * st_mean;
		final double r = st_mean + ratePerReplica / 2.0 * es2 / (1.0 - rho);

		return r;

	}

	public double utilization(double arrivalRate, int parallelism, double speedup)
	{
		// TODO we assume uniform stream repartition
		final double ratePerReplica = arrivalRate / parallelism;

		final double st_mean = serviceTimeMean / speedup;
		double rho = ratePerReplica * st_mean;
		return rho;
	}

}
