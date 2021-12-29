package it.uniroma2.dspsim.dsp.queueing;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.*;

/**
 * NOTE: Experimental.
 * Operator replicas modeled as MAP/MAP/1  queues.
 * We rely on an external Python script to pre-compute performance metrics.
 */
public class MAPMAP1OperatorModel implements OperatorQueueModel {

	private Map<Double,PerformanceTable> speedup2perf = new HashMap<>();
	private double serviceTimeMean;

	public MAPMAP1OperatorModel(double serviceTimeMean, double serviceTimeVariance) {
		this.serviceTimeMean = serviceTimeMean;

		for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
			final double speedup = nt.getCpuSpeedup();
			final double st_mean = serviceTimeMean / speedup;
			final double st_var = serviceTimeVariance / (speedup * speedup);
			speedup2perf.put(speedup, computePerf(st_mean, st_var));
		}
	}

	// TODO: invoke Python
	private static PerformanceTable computePerf(double stMean, double stVar) {

		final String TEMP_FILE = "/tmp/perf";

		// TODO: read python script from conf
		// TODO: execute script

		ArrayList<Double> rates = new ArrayList<>();
		ArrayList<Double> utils = new ArrayList<>();
		ArrayList<Double> respTimes = new ArrayList<>();

		// ROW: rate; util; respT
		Scanner myReader = new Scanner(TEMP_FILE);
		while (myReader.hasNextLine()) {
			/* Speedup date */
			String line = myReader.nextLine();
			double values[] = Arrays.stream(line.split(";")).mapToDouble(Double::parseDouble).toArray();
			rates.add(values[0]);
			utils.add(values[1]);
			respTimes.add(values[2]);

		}
		myReader.close();
		return new PerformanceTable(rates.toArray(new Double[0]), utils.toArray(new Double[0]), respTimes.toArray(new Double[0]));
	}

	public double responseTime(double arrivalRate, double speedup)
	{
		PerformanceTable table = speedup2perf.get(speedup);
		return table.getRespTime(arrivalRate);
	}

	public double utilization(double arrivalRate, double speedup)
	{
		PerformanceTable table = speedup2perf.get(speedup);
		return table.getUtil(arrivalRate);
	}

	@Override
	public double getServiceTimeMean() {
		return this.serviceTimeMean;
	}


	@Override
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
		return new MAPMAP1OperatorModel(this.serviceTimeMean, newVar);
	}

	static class PerformanceTable {
		private double utilizations[];
		private double responseTimes[];

		public PerformanceTable(Double[] rates, Double[] utils, Double[] respTimes) {
			int validRates = 0;
			for (int i = 0; i<rates.length; i++) {
				if (rates[i] < 1.0)	 {
					break;
				}
				++validRates;
			}

			this.utilizations = new double[validRates];
			this.responseTimes = new double[validRates];


			for (int i = 0; i<validRates; i++) {
				this.utilizations[i] = utils[i];
				this.responseTimes[i] = respTimes[i];
			}
		}

		public double getUtil (double rate) {
			int index = (int)(Math.ceil(rate)) - 1;
			if (index < this.utilizations.length)
				return this.utilizations[index];
			else
				return Double.POSITIVE_INFINITY;
		}

		public double getRespTime (double rate) {
			int index = (int)(Math.ceil(rate)) - 1;
			if (index < this.responseTimes.length)
				return this.responseTimes[index];
			else
				return Double.POSITIVE_INFINITY;
		}
	}

}


