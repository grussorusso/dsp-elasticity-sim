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

	private Random random = null;

	private Operator[] operators;

	public LohrmannAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();
		this.operators = application.getOperators().toArray(new Operator[]{});

		Configuration configuration = Configuration.getInstance();

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
		} else if (resSelectionPolicy.equalsIgnoreCase("zero")) {
			nodeToUse = ComputingInfrastructure.getInfrastructure().getNodeTypes()[0];
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
				for (int i = 0; i<newInstances.length; i++) {
					newInstances[i] = op.getInstances().get(i);
				}
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
			}
			rebalance(path, omMonitoringInfo, newParallelism);
		}

		for (Operator op : newParallelism.keySet()) {
			if (newParallelism.get(op) < 1 ) {
				throw new RuntimeException();
			}
		}

		return newParallelism;
	}

	private void rebalance(List<Operator> path, Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> newParallelism) {
		// Check with max parallelism
		Map<Operator, Integer> maxPar = new HashMap<>();
		for (Operator op: path) {
			maxPar.put(op, op.getMaxParallelism());
		}
		if (evaluateLatency(path, omMonitoringInfo, maxPar) >= sloLatency) {
			newParallelism.putAll(maxPar);
			return;
		}

		while (evaluateLatency(path, omMonitoringInfo, newParallelism) > sloLatency) {
			Set<Operator> scalableOperators = new HashSet<>();
			for (Operator op : path) {
				if (newParallelism.get(op) < op.getMaxParallelism())
					scalableOperators.add(op);
			}

			Map<Operator,Double> latencyGains = new HashMap<>();
			for (Operator op: scalableOperators) {
				double gain = evaluateLatencyOp(op, omMonitoringInfo.get(op), newParallelism.get(op)) -
						evaluateLatencyOp(op, omMonitoringInfo.get(op), newParallelism.get(op) + 1);
				latencyGains.put(op, gain);
			}

			// pick op with maximum gain
			Map.Entry<Operator, Double> maxEntry = null;
			for (Map.Entry<Operator, Double> entry : latencyGains.entrySet()) {
				if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0) {
					maxEntry = entry;
				}
			}
			final Operator maxOp = maxEntry.getKey();
			double serviceVar = maxOp.getQueueModel().getServiceTimeVariance();
			double serviceSCV = serviceVar / Math.pow(maxOp.getQueueModel().getServiceTimeMean(),2);
			final double b = omMonitoringInfo.get(maxOp).getInputRate() *
					maxOp.getQueueModel().getServiceTimeMean() * newParallelism.get(maxOp);
			double a = b * maxOp.getQueueModel().getServiceTimeMean();
			a *= (maxOp.getQueueModel().getArrivalSCV() + serviceSCV)/2.0;

			if (scalableOperators.size() > 1) {
				// we have more than a scalable op
				Map.Entry<Operator, Double> secondEntry = null;
				for (Map.Entry<Operator, Double> entry : latencyGains.entrySet()) {
					if (entry.equals(maxEntry))
						continue;
					if (secondEntry == null || entry.getValue().compareTo(secondEntry.getValue()) > 0) {
						secondEntry = entry;
					}
				}

				double pc = (2*b-1)/2.0;
				pc += Math.sqrt(Math.pow((1-2*b)/2.0, 2) -
								1.0/secondEntry.getValue()*(a + secondEntry.getValue()*(b*b-b))
						);
				int newP = (int)Math.ceil(pc);
				if (newP < 1) {
					throw new RuntimeException();
				}
				newParallelism.put(maxOp, Math.min(maxOp.getMaxParallelism(), newP));
			} else {
				double w = sloLatency - evaluateLatency(path, omMonitoringInfo, newParallelism) +
						evaluateLatencyOp(maxOp, omMonitoringInfo.get(maxOp),
								newParallelism.get(maxOp));
				int newP = (int)Math.ceil(a/w + b);
				if (newP < 1) {
					System.out.printf("a=%f, b=%f, w=%f, newp=%d", a, b, w, newP);
					throw new RuntimeException();
				}
				newParallelism.put(maxEntry.getKey(), newP);
			}
		}
	}

	private double evaluateLatencyOp(Operator op, OMMonitoringInfo omMonitoringInfo, int p) {
		double serviceVar = op.getQueueModel().getServiceTimeVariance();
		double serviceSCV = serviceVar / Math.pow(op.getQueueModel().getServiceTimeMean(),2);

		double rho = omMonitoringInfo.getInputRate()/p * op.getQueueModel().getServiceTimeMean();
		if (rho >= 1.0) {
			return 999;
		}
		double r = rho * op.getQueueModel().getServiceTimeMean()/ (1.0 - rho);
		r *= (op.getQueueModel().getArrivalSCV() + serviceSCV)/2.0;

		if (Double.isNaN(r) || Double.isInfinite(r)) {
			System.out.printf("rho=%f, r=%f", rho, r);
			throw new RuntimeException();
		}
		return r;
	}

	private double evaluateLatency(List<Operator> path, Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> par) {
		double r = 0.0;
		for (Operator op : path) {
			r += evaluateLatencyOp(op, omMonitoringInfo.get(op), par.get(op));
		}

		return r;
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
			if (monitoringInfo.get(op).getInputRate() * op.getQueueModel().getServiceTimeMean() > 0.99) {
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
