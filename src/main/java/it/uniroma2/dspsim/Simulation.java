package it.uniroma2.dspsim;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.edf.ApplicationManager;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.io.IOException;

public class Simulation {

	/** Simulated time */
	private long time = 0l;

	private InputRateFileReader inputRateFileReader;
	private ApplicationManager applicationManager;

	public Simulation (InputRateFileReader inputRateFileReader, ApplicationManager applicationManager) {
		this.inputRateFileReader = inputRateFileReader;
		this.applicationManager = applicationManager;
	}

	public void run() throws IOException {
		run(-1l);
	}

	public void run (long stopTime) throws IOException {
		Application app = applicationManager.getApplication();

		while (inputRateFileReader.hasNext() && (stopTime <= 0 || time <= stopTime)) {
			double inputRate = inputRateFileReader.next();
			double responseTime = app.endToEndLatency(inputRate);

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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
