package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerFactory;
import it.uniroma2.dspsim.dsp.edf.am.ApplicationManagerType;
import it.uniroma2.dspsim.dsp.edf.am.centralized.CentralizedAM;
import it.uniroma2.dspsim.dsp.edf.om.*;
import it.uniroma2.dspsim.dsp.edf.om.factory.OperatorManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class EDF {

	private static Logger logger = LoggerFactory.getLogger(EDF.class);

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

		applicationManager = newApplicationManager(conf, sloLatency);

		if (!(applicationManager instanceof CentralizedAM)) {
			operatorManagers = new HashMap<>(numOperators);
			for (Operator op : operators) {
				operatorManagers.put(op, newOperatorManager(op, conf));
			}
		}
	}

	private ApplicationManager newApplicationManager(Configuration conf, double sloLatency) {
		// get application manager type from configurations
		final String amType = conf.getString(ConfigurationKeys.AM_TYPE_KEY, "do-nothing");
		ApplicationManagerType appManagerType = ApplicationManagerType.fromString(amType);

		return ApplicationManagerFactory.createApplicationManager(appManagerType, application, sloLatency);
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
		Map<Operator, Double> opInputRate = application.computePerOperatorInputRate(monitoringInfo.getInputRate());

		for (Operator op : application.getOperators()) {
			final double rate = opInputRate.get(op);
			final double u = op.utilization(rate);

			omMonitoringInfo.put(op, new OMMonitoringInfo());
			omMonitoringInfo.get(op).setInputRate(rate);
			omMonitoringInfo.get(op).setCpuUtilization(u);
		}

		return applicationManager.planReconfigurations(omMonitoringInfo, operatorManagers);
	}

}
