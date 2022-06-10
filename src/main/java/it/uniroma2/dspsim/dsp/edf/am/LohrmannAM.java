package it.uniroma2.dspsim.dsp.edf.am;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


public class LohrmannAM extends ApplicationManager {

	static private final Logger logger = LoggerFactory.getLogger(LohrmannAM.class);

	private int nOperators;
	private int maxParallelism[];

	private final int maxInputRate;
	private Random random = null;


	private Operator[] operators;

	public LohrmannAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();
		this.operators = application.getOperators().toArray(new Operator[]{});

		Configuration configuration = Configuration.getInstance();

		// input rate discretization
		this.maxInputRate = configuration.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);

		this.maxParallelism = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
		}

		String resSelectionPolicy = Configuration.getInstance().getString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "cost");
		if (resSelectionPolicy.equalsIgnoreCase("random")) {
			this.random = new Random(129);
		}
	}


	@Override
	public Map<Operator, Reconfiguration> planReconfigurations(Map<Operator, OMMonitoringInfo> omMonitoringInfo,
															   Map<Operator, OperatorManager> operatorManagers) {

		Map<Operator, Integer> minParallelism = new HashMap<>();
		for (Operator op : operators) {
			minParallelism.put(op, 1);
		}

		Map<Operator, Integer> newParallelism = scalingPolicy(omMonitoringInfo, minParallelism);

		String resSelectionPolicy = Configuration.getInstance().getString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "cost");
		NodeType nodeToUse = null;
		if (resSelectionPolicy.equalsIgnoreCase("speedup")) {
			for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
				if (nodeToUse == null || nodeToUse.getCpuSpeedup() < nt.getCpuSpeedup())	{
					nodeToUse = nt;
				}
			}
		} else if (resSelectionPolicy.equalsIgnoreCase("random")) {
			nodeToUse = ComputingInfrastructure.getInfrastructure().getNodeTypes()[this.random.nextInt(ComputingInfrastructure.getInfrastructure().getNodeTypes().length)];
		} else {
			for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
				if (nodeToUse == null || nodeToUse.getCost() > nt.getCost())	{
					nodeToUse = nt;
				}
			}
		}

		Map<Operator, Reconfiguration> opReconfs = new HashMap<>(nOperators);
		for (Operator op : operators) {
			Reconfiguration rcf;
			int newP = newParallelism.get(op);
			if (newP == op.getCurrentParallelism()) {
				rcf = Reconfiguration.doNothing();
			} else if (newP > op.getCurrentParallelism()) {
				NodeType[] newInstances = new NodeType[newP - op.getCurrentParallelism()];
				Arrays.fill(newInstances, nodeToUse);
				rcf = Reconfiguration.scaleOut(newInstances);
			} else {
				NodeType[] newInstances = new NodeType[op.getCurrentParallelism() - newP];
				Arrays.fill(newInstances, nodeToUse);
				rcf = Reconfiguration.scaleIn(newInstances);
			}

			opReconfs.put(op, rcf);
		}

		return opReconfs;
	}

	protected Map<Operator, Integer> scalingPolicy(Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> minParallelism) {
		Map<Operator, Integer> newParallelism = new HashMap<>(minParallelism);

		for (Operator op : operators)	 {
			newParallelism.put(op, minParallelism.get(op));
		}

		for (List<Operator> path : application.getAllPaths()) {
			if (hasBottleneck(path, omMonitoringInfo)) {
				resolveBottlenecks(path, omMonitoringInfo, newParallelism);
			} else {
				rebalance(newParallelism);
			}
		}

		return newParallelism;
	}

	private void rebalance(Map<Operator, Integer> newParallelism) {
		// TODO
	}

	private void resolveBottlenecks(List<Operator> path, Map<Operator, OMMonitoringInfo> monitoringInfo, Map<Operator, Integer> newParallelism) {
		for (Operator op : path) {
			double rho = monitoringInfo.get(op).getCpuUtilization();
			if (rho > 0.99) {
				int newP = (int)Math.ceil(
						Math.min(op.getMaxParallelism(),
						Math.max(2*op.getCurrentParallelism(),
								2*op.getCurrentParallelism()*rho)));
				//System.out.println(String.format("Scaling bottleneck %s to %d", op.getName(), newP));
				newParallelism.put(op, newP);
			}
		}
	}

	private boolean hasBottleneck(List<Operator> path, Map<Operator, OMMonitoringInfo> monitoringInfo) {
		for (Operator op : path) {
			if (monitoringInfo.get(op).getCpuUtilization() > 0.99) {
				return true;
			}
		}

		return false;
	}

	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														Map<Operator, OMMonitoringInfo> omMonitoringInfoMap) {
		throw new RuntimeException("This method should never be called!");
	}



}
