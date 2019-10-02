package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class Operator {

	private String name;
	private int maxParallelism;

	private double selectivity = 1.0;
	private double serviceTimeMean = 1.0;
	private double serviceTimeVariance = 0.0;

	private ArrayList<NodeType> instances;

	private Collection<Operator> upstreamOperators = new ArrayList<>();
	private Collection<Operator> downstreamOperators = new ArrayList<>();

	public Operator (String name, double serviceTimeMean, double serviceTimeVariance,
					 int maxParallelism) {
		this.name = name;

		this.maxParallelism = maxParallelism;
		instances = new ArrayList<>(maxParallelism);

		this.serviceTimeMean = serviceTimeMean;
		this.serviceTimeVariance = serviceTimeVariance;

		/* Add initial replica */
		NodeType defaultType = ComputingInfrastructure.getInfrastructure().getNodeTypes()[0];
		instances.add(defaultType);
	}

	public void setSelectivity (double selectivity) {
		this.selectivity = selectivity;
	}

	public double utilization (double inputRate) {
		return utilization(inputRate, instances.size(), new HashSet<NodeType>(instances));
	}

	public double responseTime (double inputRate) {
		return responseTime(inputRate, instances.size(), new HashSet<NodeType>(instances));
	}


	public double utilization (double inputRate, int parallelism, Set<NodeType> usedNodeTypes) {
		if (usedNodeTypes.size() > parallelism)
			throw new RuntimeException("Cannot use more resource types than replicas...");

		// TODO we assume uniform stream repartition
		final double ratePerReplica = inputRate / instances.size();

		double currentSpeedup = Double.POSITIVE_INFINITY;
		for (NodeType nt : usedNodeTypes)
			currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

		final double st_mean = serviceTimeMean / currentSpeedup;
		double rho = ratePerReplica * st_mean;
		return rho;
	}

	public double responseTime (double inputRate, int parallelism, Set<NodeType> usedNodeTypes) {
		if (usedNodeTypes.size() > parallelism)
			throw new RuntimeException("Cannot use more resource types than replicas...");

		// TODO we assume uniform stream repartition
		final double ratePerReplica = inputRate / instances.size();

		double currentSpeedup = Double.POSITIVE_INFINITY;
		for (NodeType nt : usedNodeTypes)
			currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

		final double rho = utilization(inputRate, parallelism, usedNodeTypes);
		if (rho >= 1.0)	 {
			return Double.POSITIVE_INFINITY;
		}

		final double st_mean = serviceTimeMean / currentSpeedup;
		final double st_var = serviceTimeVariance / (currentSpeedup * currentSpeedup);
		final double es2 = st_var + st_mean * st_mean;
		final double r = st_mean + ratePerReplica / 2.0 * es2 / (1.0 - rho);

		return r;
	}

	public void addUpstream (Operator op) {
		upstreamOperators.add(op);
	}

	public void addDownstream (Operator op) {
		downstreamOperators.add(op);
	}

	public boolean isSource() {
		return upstreamOperators.isEmpty();
	}

	public boolean isSink() {
		return downstreamOperators.isEmpty();
	}

	public Collection<Operator> getUpstreamOperators() {
		return upstreamOperators;
	}

	public Collection<Operator> getDownstreamOperators() {
		return downstreamOperators;
	}

	@Override
	public String toString() {
		return "Operator{" +
				"name='" + name + '\'' +
				'}';
	}
}
