package it.uniroma2.dspsim.dsp.edf;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EDF {

	private Application application;
	private ComputingInfrastructure infrastructure;

	private ApplicationManager applicationManager;
	private List<OperatorManager> operatorManagers;

	public EDF (Application application, ComputingInfrastructure infrastructure)
	{
		this.application = application;
		this.infrastructure = infrastructure;

		/* Create OMs */
		final List<Operator> operators = application.getOperators();
		final int numOperators = operators.size();

		operatorManagers = new ArrayList<>(numOperators);
		for (Operator op : operators) {
			OperatorManager om = new DoNothingOM(op); // TODO configurable type of OM
			operatorManagers.add(om);
		}
	}

	public void pickReconfigurations (MonitoringInfo monitoringInfo) {
		// TODO create OMMonitoringInfo

		// TODO let each OM pick a reconfiguration

		// TODO let the AM filter reconfigurations
	}

}
