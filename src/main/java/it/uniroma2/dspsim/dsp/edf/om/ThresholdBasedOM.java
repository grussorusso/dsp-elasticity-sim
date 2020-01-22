package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

public class ThresholdBasedOM extends OperatorManager {

	private double scaleOutThreshold;
	private NodeType defaultNodeType = null;

	public ThresholdBasedOM(Operator operator) {
		super(operator);

		/* Default resource type (cheapest). */
		// TODO use more complex criterion
		for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
			if (defaultNodeType == null || defaultNodeType.getCost() > nt.getCost())	{
				defaultNodeType = nt;
			}
		}

		this.scaleOutThreshold = Configuration.getInstance().getDouble(ConfigurationKeys.OM_THRESHOLD_KEY, 0.7);
	}

	public ThresholdBasedOM(Operator operator, double scaleOutThreshold) {
		this(operator);
		this.scaleOutThreshold = scaleOutThreshold;
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
		final double u = monitoringInfo.getCpuUtilization();
		final double p = operator.getInstances().size();

		Reconfiguration rcf;

		if (u > scaleOutThreshold && p < operator.getMaxParallelism()) {
			/* scale-out */
			rcf = Reconfiguration.scaleOut(defaultNodeType);
		} else if (p > 1 && u*p/(p-1) < 0.75 * scaleOutThreshold) {
			/* scale-in */
			rcf = Reconfiguration.scaleIn(defaultNodeType);
		} else {
			rcf = Reconfiguration.doNothing();
		}


		return new OMRequest(rcf);
	}
}
