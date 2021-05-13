package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.BasicOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.threshold.MaxSpeedupThresholdPolicy;
import it.uniroma2.dspsim.dsp.edf.om.threshold.MinCostThresholdPolicy;
import it.uniroma2.dspsim.dsp.edf.om.threshold.RandomSelectionThresholdPolicy;
import it.uniroma2.dspsim.dsp.edf.om.threshold.ThresholdPolicy;

import java.util.Random;

public class ThresholdBasedOM extends OperatorManager {

	private double scaleOutThreshold;
	private ThresholdPolicy thresholdPolicy;

	private boolean noisyUtilizationMonitoring;
	private Random utilNoiseRng;

	public ThresholdBasedOM(Operator operator) {
		super(operator);

		String resSelectionPolicy = Configuration.getInstance().getString(ConfigurationKeys.OM_THRESHOLD_RESOURCE_SELECTION, "cost");
		if (resSelectionPolicy.equalsIgnoreCase("speedup")) {
			this.thresholdPolicy = new MaxSpeedupThresholdPolicy();
		} else if (resSelectionPolicy.equalsIgnoreCase("random")) {
			this.thresholdPolicy = new RandomSelectionThresholdPolicy();
		} else {
			this.thresholdPolicy = new MinCostThresholdPolicy();
		}

		this.scaleOutThreshold = Configuration.getInstance().getDouble(ConfigurationKeys.OM_THRESHOLD_KEY, 0.7);
		this.noisyUtilizationMonitoring = Configuration.getInstance().getBoolean(ConfigurationKeys.OM_THRESHOLD_UTIL_NOISE, false);
		if (this.noisyUtilizationMonitoring) {
			this.utilNoiseRng = new Random(this.operator.getName().hashCode());
		}
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
		double u = monitoringInfo.getCpuUtilization();

		if (this.noisyUtilizationMonitoring) {
			final double MAX_REL_ERR = 0.1;
			final double err = -MAX_REL_ERR/2.0 + this.utilNoiseRng.nextDouble() * MAX_REL_ERR;
			u = Math.max(0.0, u * (1.0 + err));
		}

		final double p = operator.getInstances().size();

		Reconfiguration rcf = thresholdPolicy.applyThresholdPolicy(u, p, operator, this.scaleOutThreshold);
		return new BasicOMRequest(rcf);
	}
}
