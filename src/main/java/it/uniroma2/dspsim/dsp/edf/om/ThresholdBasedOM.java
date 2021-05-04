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

public class ThresholdBasedOM extends OperatorManager {

	private double scaleOutThreshold;
	private ThresholdPolicy thresholdPolicy;

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
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
		final double u = monitoringInfo.getCpuUtilization();
		final double p = operator.getInstances().size();

		Reconfiguration rcf = thresholdPolicy.applyThresholdPolicy(u, p, operator, this.scaleOutThreshold);
		return new BasicOMRequest(rcf);
	}
}
