package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.threshold.MaxSpeedupThresholdPolicy;
import it.uniroma2.dspsim.dsp.edf.om.threshold.MinCostThresholdPolicy;
import it.uniroma2.dspsim.dsp.edf.om.threshold.ThresholdPolicy;

public class ThresholdBasedOM extends OperatorManager {

	private double scaleOutThreshold;
	private ThresholdPolicy thresholdPolicy;

	public ThresholdBasedOM(Operator operator) {
		super(operator);

		this.thresholdPolicy = new MaxSpeedupThresholdPolicy();

		this.scaleOutThreshold = Configuration.getInstance().getDouble(ConfigurationKeys.OM_THRESHOLD_KEY, 0.7);
	}

	@Override
	public Reconfiguration pickReconfiguration(OMMonitoringInfo monitoringInfo) {
		final double u = monitoringInfo.getCpuUtilization();
		final double p = operator.getInstances().size();

		return thresholdPolicy.applyThresholdPolicy(u, p, operator, this.scaleOutThreshold);
	}
}
