package it.uniroma2.dspsim.dsp.edf.om;

public class OMMonitoringInfo {

	private double inputRate = 0.0;
	private double cpuUtilization = 0.0;

	public double getCpuUtilization() {
		return cpuUtilization;
	}

	public void setCpuUtilization(double cpuUtilization) {
		this.cpuUtilization = cpuUtilization;
	}


	public double getInputRate() {
		return inputRate;
	}

	public void setInputRate(double inputRate) {
		this.inputRate = inputRate;
	}
}
