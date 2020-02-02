package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerFactory;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerType;
import it.uniroma2.dspsim.dsp.edf.om.*;
import it.uniroma2.dspsim.dsp.edf.om.factory.OperatorManagerFactory;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;

import java.util.*;

public class EDF {

	private Application application;

	private ApplicationManager applicationManager;
	private Map<Operator, OperatorManager> operatorManagers;

	public EDF (Application application, double sloLatency)
	{
		this.application = application;

		/* Create OMs */
		final List<Operator> operators = application.getOperators();
		final int numOperators = operators.size();

		Configuration conf = Configuration.getInstance();

		applicationManager = newApplicationManager(conf);
		applicationManager.setSloLatency(sloLatency);

		operatorManagers = new HashMap<>(numOperators);
		for (Operator op : operators) {
			operatorManagers.put(op, newOperatorManager(op, conf));
		}
	}

	private ApplicationManager newApplicationManager(Configuration conf) {
		// get application manager type from configurations
		final String amType = conf.getString(ConfigurationKeys.AM_TYPE_KEY, "do-nothing");
		ApplicationManagerType appManagerType = ApplicationManagerType.fromString(amType);

		return ApplicationManagerFactory.createApplicationManager(appManagerType, application);
	}

	protected OperatorManager newOperatorManager (Operator op, Configuration configuration) {
		// get operator manager type from configurations
		final String omType = configuration.getString(ConfigurationKeys.OM_TYPE_KEY, "qlearning");

		// decode operator manager type
		// WARNING : could throw IllegalArgumentException
		OperatorManagerType operatorManagerType = OperatorManagerType.fromString(omType);

		return OperatorManagerFactory.createOperatorManager(operatorManagerType, op);
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
		Map<OperatorManager, OMRequest> omRequests = new HashMap<>();
		for (Operator op : application.getOperators()) {
			OMMonitoringInfo operatorMonitoringInfo = omMonitoringInfo.get(op);
			OperatorManager om = operatorManagers.get(op);
			OMRequest req = om.pickReconfigurationRequest(operatorMonitoringInfo);
			omRequests.put(om, req);
		}

		// Let the AM filter reconfigurations
		Map<Operator, Reconfiguration> reconfigurations = applicationManager.planReconfigurations(omRequests);

		return reconfigurations;
	}

}
