package it.uniroma2.dspsim.dsp.queueing;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * NOTE: Experimental.
 * Reads utilization and response time values from a pre-computed CSV file.
 * Format:
 * Col 0: <speedup 1>, opStMean, opStScv, <speedup 2> opStMean opStScv ....
 * Col i: i==rate, util w speedup 1 and rate i, RT w speedup 1 and rate i, i, util w speedup 2, ...
 */
@Deprecated
public class FromFileQueueModel implements OperatorQueueModel {

	private Map<Double,PerformanceTable> tables = new HashMap<>();
	private double serviceTimeMean;

	public FromFileQueueModel(String filename) throws FileNotFoundException {
		parseFile(new File(filename));
	}

	private void parseFile(File file) throws FileNotFoundException {
		Scanner myReader = new Scanner(file);
		while (myReader.hasNextLine()) {
			/* Speedup date */
			String data1 = myReader.nextLine();
			String data2 = myReader.nextLine();
			String data3 = myReader.nextLine();
			double speedup = Double.parseDouble(data1.split(",")[0]);
			this.serviceTimeMean = Double.parseDouble(data2.split(",")[0]);

			double rates[] = Arrays.stream(data1.split(",")).skip(1).mapToDouble(Double::parseDouble).toArray();
			double utils[] = Arrays.stream(data2.split(",")).skip(1).mapToDouble(Double::parseDouble).toArray();
			double respTimes[] = Arrays.stream(data3.split(",")).skip(1).mapToDouble(Double::parseDouble).toArray();
			PerformanceTable table = new PerformanceTable(rates, utils, respTimes);
			tables.put(speedup, table);
		}
		myReader.close();
	}

	public double responseTime(double arrivalRate, double speedup)
	{
		PerformanceTable table = tables.get(speedup);
		double r = table.getRespTime(arrivalRate);
		//System.out.printf("R(%.1f, %.1f) = %.3f\n", arrivalRate, speedup, r);
		return r;
	}

	public double utilization(double arrivalRate, double speedup)
	{
		PerformanceTable table = tables.get(speedup);
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
		return new MG1OperatorQueueModel(newMean, newVar);
	}

	static class PerformanceTable {
		private double utilizations[];
		private double responseTimes[];

		public PerformanceTable(double[] rates, double[] utils, double[] respTimes) {
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

	public static void main (String args[]) throws FileNotFoundException {
		String filename = "/tmp/prova.txt";
		FromFileQueueModel model = new FromFileQueueModel(filename);
		System.out.println(model.responseTime(100.0, 2.0));
	}
}


