package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


public class LohrmannAM extends ApplicationManager {

	static private final Logger logger = LoggerFactory.getLogger(LohrmannAM.class);

	private int nOperators;
	private int maxParallelism[];

	private int maxInputRate;


	private Operator[] operators;

	public LohrmannAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();
		this.operators = application.getOperators().toArray(new Operator[]{});

		Configuration configuration = Configuration.getInstance();

		// input rate discretization
		this.maxInputRate = configuration.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);


		for (Operator op : application.getOperators()) {
			if (op.getSelectivity() > 1.01 || op.getSelectivity() < 0.99) {
				throw new RuntimeException("CentralizedAM currently does not support selectivity != 1");
			}
		}

		this.maxParallelism = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
		}

	}


	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {

		// Build map op->reconf based on global action
		Map<Operator, Reconfiguration> opReconfs = new HashMap<>(nOperators);
		for (int i = 0; i<nOperators; i++) {
			Reconfiguration rcf = Reconfiguration.doNothing(); // TODO
			opReconfs.put(application.getOperators().get(i), rcf);
		}

		return opReconfs;
	}

	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														Map<Operator, OMMonitoringInfo> omMonitoringInfoMap) {
		throw new RuntimeException("This method should never be called!");
	}



}
