package it.uniroma2.dspsim;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.io.IOException;

public class Simulation {

	/** Simulated time */
	private long time = 0l;

	private InputRateFileReader inputRateFileReader;

	public Simulation (InputRateFileReader inputRateFileReader) {
		this.inputRateFileReader = inputRateFileReader;
	}

	public void run() throws IOException {
		run(-1l);
	}

	public void run (long stopTime) throws IOException {
		while (inputRateFileReader.hasNext() && (stopTime <= 0 || time <= stopTime)) {
			double inputRate = inputRateFileReader.next();
			System.out.println(inputRate);
		}
	}

	public static void main (String args[]) {
		ComputingInfrastructure.initDefaultInfrastructure(3);

		try {
			final String inputFile = "/home/gabriele/profile.dat";
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Simulation simulation = new Simulation(inputRateFileReader);
			simulation.run();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
