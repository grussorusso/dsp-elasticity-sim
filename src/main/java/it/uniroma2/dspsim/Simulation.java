package it.uniroma2.dspsim;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.edf.ApplicationManager;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.CountMetric;
import it.uniroma2.dspsim.stats.Statistics;

import java.io.IOException;

public class Simulation {

	/** Simulated time */
	private long time = 0l;

	// TODO should be configurable
	private static final double LATENCY_SLO = 0.100;

	/* Statistics */
	private static final String STAT_LATENCY_VIOLATIONS = "LatencyViolations";

	private InputRateFileReader inputRateFileReader;
	private ApplicationManager applicationManager;

	private Statistics statistics = Statistics.getInstance();

	public Simulation (InputRateFileReader inputRateFileReader, ApplicationManager applicationManager) {
		this.inputRateFileReader = inputRateFileReader;
		this.applicationManager = applicationManager;
	}

	public void run() throws IOException {
		run(-1l);
	}

	private void registerMetrics () {
		statistics.registerMetric(new CountMetric(STAT_LATENCY_VIOLATIONS));
	}

	public void run (long stopTime) throws IOException {
		registerMetrics();


		Application app = applicationManager.getApplication();

		while (inputRateFileReader.hasNext() && (stopTime <= 0 || time <= stopTime)) {
			double inputRate = inputRateFileReader.next();
			double responseTime = app.endToEndLatency(inputRate);

			if (responseTime > LATENCY_SLO) {
				statistics.updateMetric(STAT_LATENCY_VIOLATIONS, 1);
			}

			System.out.println(inputRate + "\t" + responseTime);
		}
	}

	public static void main (String args[]) {
		ComputingInfrastructure.initDefaultInfrastructure(3);

		try {
			final String inputFile = "/home/gabriele/profile.dat";
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Application app = Application.buildDefaultApplication();
			ApplicationManager am = new ApplicationManager(app);

			Simulation simulation = new Simulation(inputRateFileReader, am);
			simulation.run();

			/* Dump statistics to standard output. */
			Statistics.getInstance().dumpAll();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
