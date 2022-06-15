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


public class DrsAM extends ApplicationManager {

	static private final Logger logger = LoggerFactory.getLogger(DrsAM.class);

	private int nOperators;
	private int maxParallelism[];

	private Random random = null;

	private Operator[] operators;
	private int Kmax;

	public DrsAM(Application application, double sloLatency) {
		super(application, sloLatency);
		this.nOperators = application.getOperators().size();
		this.operators = application.getOperators().toArray(new Operator[]{});

		Configuration configuration = Configuration.getInstance();

		this.maxParallelism = new int[nOperators];
		for (int i = 0; i<nOperators; i++) {
			maxParallelism[i] = application.getOperators().get(i).getMaxParallelism();
			this.Kmax += maxParallelism[i];
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
		Map<Operator, Integer> newParallelism = new HashMap<>();

		for (Operator op : operators)	 {
			newParallelism.put(op, minParallelism.get(op));
		}

		for (List<Operator> path : application.getAllPaths()) {
			scale(path, omMonitoringInfo, newParallelism);
		}

		for (Operator op : newParallelism.keySet()) {
			if (newParallelism.get(op) < 1 ) {
				throw new RuntimeException();
			}
		}

		return newParallelism;
	}

	private void scale(List<Operator> path, Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> newParallelism) {
		int K = 0;
		for (Operator op: path) {
			int p = (int)Math.floor(omMonitoringInfo.get(op).getInputRate()*op.getQueueModel().getServiceTimeMean()) + 1;
			p = Math.max(newParallelism.get(op), p);
			K += p;
			newParallelism.put(op, p);
		}
		if (evaluateLatency(path, omMonitoringInfo, newParallelism) >= sloLatency) {
			// unfeasible requirement
			return;
		}

		while (evaluateLatency(path, omMonitoringInfo, newParallelism) > sloLatency) {
			K++;
			assignProcessors(path, omMonitoringInfo,  newParallelism);
		}
	}

	private void assignProcessors(List<Operator> path, Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> newParallelism) {
		Map<Operator,Double> latencyGains = new HashMap<>();
		for (Operator op: path) {
			if (newParallelism.get(op) == op.getMaxParallelism())
				continue;
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
		newParallelism.put(maxOp, newParallelism.get(maxOp) + 1);
	}

	private double evaluateLatencyOp(Operator op, OMMonitoringInfo omMonitoringInfo, int p) {
		double serviceVar = op.getQueueModel().getServiceTimeVariance();
		double serviceSCV = serviceVar / Math.pow(op.getQueueModel().getServiceTimeMean(),2);

		double rho = omMonitoringInfo.getInputRate()/p * op.getQueueModel().getServiceTimeMean();
		if (rho >= 1.0) {
			return 999;
		}
		double p0 = 0;
		for (int l = 0; l<=p-1; l++) {
			p0 +=Math.pow(p*rho, l)/fact(l);
		}
		p0 += Math.pow(p*rho, p)/fact(p)/(1-rho);
		p0 = 1.0/p0;
		double qtime = p0*Math.pow(p*rho,p)/(fact(p)*(1-rho)*(1-rho)/op.getQueueModel().getServiceTimeMean()*p);
		double r = op.getQueueModel().getServiceTimeMean() + qtime*(op.getQueueModel().getArrivalSCV() + serviceSCV)/2.0;

		if (Double.isNaN(r) || Double.isInfinite(r)) {
			System.out.printf("rho=%f, r=%f", rho, r);
			throw new RuntimeException();
		}
		return r;
	}

	private static long fact(int p) {
		long f = 1;
		while (p >= 1) {
			f = f*p;
			p--;
		}
		return f;
	}

	private double evaluateLatency(List<Operator> path, Map<Operator, OMMonitoringInfo> omMonitoringInfo, Map<Operator, Integer> par) {
		double r = 0.0;
		for (Operator op : path) {
			r += evaluateLatencyOp(op, omMonitoringInfo.get(op), par.get(op));
		}

		return r;
	}


	@Override
	final protected Map<Operator, Reconfiguration> plan(Map<OperatorManager, OMRequest> omRequestMap,
														Map<Operator, OMMonitoringInfo> omMonitoringInfoMap) {
		throw new RuntimeException("This method should never be called!");
	}



}
