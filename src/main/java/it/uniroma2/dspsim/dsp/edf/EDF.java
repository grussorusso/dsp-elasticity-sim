package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.*;
import it.uniroma2.dspsim.dsp.edf.om.factory.OperatorManagerFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;

import java.util.*;

public class EDF {

	private Application application;

	private ApplicationManager applicationManager;
	private Map<Operator, OperatorManager> operatorManagers;

	public EDF (Application application)
	{
		this.application = application;

		/* Create OMs */
		final List<Operator> operators = application.getOperators();
		final int numOperators = operators.size();

		Configuration conf = Configuration.getInstance();

		operatorManagers = new HashMap<>(numOperators);
		for (Operator op : operators) {
			operatorManagers.put(op, newOperatorManager(op, conf));
		}
	}

	protected OperatorManager newOperatorManager (Operator op, Configuration configuration) {
		return OperatorManagerFactory.createOperatorManager(op, configuration);
	}

	public Map<Operator, Reconfiguration> pickReconfigurations (MonitoringInfo monitoringInfo) {
		Map<Operator, OMMonitoringInfo> omMonitoringInfo = new HashMap<>();
		for (Operator op : operatorManagers.keySet()) {
			omMonitoringInfo.put(op, new OMMonitoringInfo());
			omMonitoringInfo.get(op).setInputRate(0.0);
		}

		/* Compute operator input rate */
		boolean done = false;

		while (!done) {
			done = true;

			for (Operator src : application.getOperators()) {
				if (!src.isSource())
					continue;

				/* BFS visit */
				Set<Operator> checked = new HashSet<>();
				Deque<Operator> queue = new ArrayDeque<>();
				queue.push(src);

				while (!queue.isEmpty()) {
					Operator op = queue.pop();
					if (!checked.contains(op)) {
						double rate = 0.0;

						if (op.isSource())	{
							rate = monitoringInfo.getInputRate(); // TODO what if we have multiple sources?
						} else {
							for (Operator up : op.getUpstreamOperators()) {
								rate += omMonitoringInfo.get(up).getInputRate() * up.getSelectivity();
							}
						}

						double oldValue = omMonitoringInfo.get(op).getInputRate();
						done &= (oldValue == rate);

						omMonitoringInfo.get(op).setInputRate(rate);

						for (Operator down : op.getDownstreamOperators())  {
							queue.push(down);
						}

						checked.add(op);
					}
				}
			}
		}

		/* Add other monitoring information */
		for (Operator op : application.getOperators()) {
			OMMonitoringInfo opInfo = omMonitoringInfo.get(op);
			final double u = op.utilization(opInfo.getInputRate());
			opInfo.setCpuUtilization(u);
		}

		/* Let each OM make a decision. */
		Map<Operator, Reconfiguration> reconfigurations = new HashMap<>();
		for (Operator op : application.getOperators()) {
			Reconfiguration rcf = operatorManagers.get(op).pickReconfiguration(omMonitoringInfo.get(op));
			reconfigurations.put(op, rcf);
		}

		// TODO let the AM filter reconfigurations

		return reconfigurations;
	}

}
