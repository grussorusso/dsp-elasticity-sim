package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.*;

public class Operator {

	private String name;
	private int maxParallelism;

	private double selectivity = 1.0;

	private OperatorQueueModel queueModel;
	private ArrayList<NodeType> instances;

	private Collection<Operator> upstreamOperators = new ArrayList<>();
	private Collection<Operator> downstreamOperators = new ArrayList<>();

	public Operator(String name, OperatorQueueModel queueModel, int maxParallelism) {
		this.name = name;

		this.maxParallelism = maxParallelism;
		instances = new ArrayList<>(maxParallelism);

		this.queueModel = queueModel;

		/* Add initial replica */
		NodeType defaultType = ComputingInfrastructure.getInfrastructure().getNodeTypes()[0];
		instances.add(defaultType);
	}

	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}

	public double utilization(double inputRate) {
		return utilization(inputRate, instances.size(), new HashSet<NodeType>(instances));
	}

	public double responseTime(double inputRate) {
		return responseTime(inputRate, instances.size(), new HashSet<NodeType>(instances));
	}


	public double utilization(double inputRate, int parallelism, Set<NodeType> usedNodeTypes) {
		if (usedNodeTypes.size() > parallelism)
			throw new RuntimeException("Cannot use more resource types than replicas...");

		double currentSpeedup = Double.POSITIVE_INFINITY;
		for (NodeType nt : usedNodeTypes)
			currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

		return queueModel.utilization(inputRate, instances.size(), currentSpeedup);
	}

	public double responseTime(double inputRate, int parallelism, Set<NodeType> usedNodeTypes) {
		if (usedNodeTypes.size() > parallelism)
			throw new RuntimeException("Cannot use more resource types than replicas...");

		double currentSpeedup = Double.POSITIVE_INFINITY;
		for (NodeType nt : usedNodeTypes)
			currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

		return queueModel.responseTime(inputRate, instances.size(), currentSpeedup);
	}

	/**
	 * Applies a reconfiguration.
	 * @param reconfiguration Reconfiguration to apply.
	 */
	public void reconfigure (Reconfiguration reconfiguration) {
		int parallelism = instances.size();

		if (reconfiguration.getInstancesToAdd() != null) {
			for (NodeType nt : reconfiguration.getInstancesToAdd()) {
				instances.add(nt);
				parallelism++;
			}
		}

		if (reconfiguration.getInstancesToRemove() != null) {
			for (NodeType nt : reconfiguration.getInstancesToRemove()) {
				instances.remove(nt);
				parallelism--;
			}
		}

		if (parallelism != instances.size())
			throw new RuntimeException("something gone wrong in the reconfiguration");
	}

	public void addUpstream(Operator op) {
		upstreamOperators.add(op);
	}

	public void addDownstream(Operator op) {
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

	public String getName() {
		return name;
	}

	public int getMaxParallelism() {
		return maxParallelism;
	}

	public double getSelectivity() {
		return selectivity;
	}

	public OperatorQueueModel getQueueModel() {
		return queueModel;
	}

	public ArrayList<NodeType> getInstances() {
		return instances;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Operator operator = (Operator) o;
		return name.equals(operator.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public double computeDeploymentCost() {
		double c = 0.0;

		for (NodeType nt : instances)
			c += nt.getCost();

		return c;
	}

	public double computeMaxDeploymentCost() {
		return maxParallelism * ComputingInfrastructure.getInfrastructure().getMostExpensiveResType().getCost();
	}

	public double computeNormalizedDeploymentCost() {
		return computeDeploymentCost() / computeMaxDeploymentCost();
	}
}
