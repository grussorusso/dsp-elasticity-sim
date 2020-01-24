package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancer;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancerFactory;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancerType;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.*;

public class Operator {

	// case considered while
	// computing utilization or response time
	private enum SpeedupCase{
		AVG,
		WORST;

		public static SpeedupCase fromString(String str) {
			if (str.equalsIgnoreCase("avg")) {
				return AVG;
			} else if (str.equalsIgnoreCase("worst")) {
				return WORST;
			} else {
				throw new IllegalArgumentException("Not valid speedup case type " + str);
			}
		}
	}

	private String name;
	private int maxParallelism;

	private double selectivity = 1.0;

	private OperatorQueueModel queueModel;
	private ArrayList<NodeType> instances;

	private Collection<Operator> upstreamOperators = new ArrayList<>();
	private Collection<Operator> downstreamOperators = new ArrayList<>();

	private double sloRespTime;

	private LoadBalancer loadBalancer;
	private SpeedupCase speedupCase;

	public Operator(String name, OperatorQueueModel queueModel, int maxParallelism) {
		this.name = name;

		this.maxParallelism = maxParallelism;
		instances = new ArrayList<>(maxParallelism);

		this.queueModel = queueModel;

		// init operator load balancer policy
		this.loadBalancer = LoadBalancerFactory.getLoadBalancer(
				LoadBalancerType.fromString(Configuration.getInstance().getString(
						ConfigurationKeys.OPERATOR_LOAD_BALANCER_TYPE_KEY, "rr")));

		// get speedup case considered while computing utilization or response time
		this.speedupCase = SpeedupCase.fromString(Configuration.getInstance().getString(
				ConfigurationKeys.OPERATOR_SPEEDUP_CASE_CONSIDERED_KEY, "worst"));

		/* Add initial replica */
		NodeType defaultType = ComputingInfrastructure.getInfrastructure().getNodeTypes()[0];
		instances.add(defaultType);
	}

	public void setSelectivity(double selectivity) {
		this.selectivity = selectivity;
	}

	public void setSloRespTime(double sloRespTime) {
		this.sloRespTime = sloRespTime;
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

		double currentSpeedup = computeSpeedup(inputRate, usedNodeTypes);

		return queueModel.utilization(inputRate, instances.size(), currentSpeedup);
	}

	public double responseTime(double inputRate, int parallelism, Set<NodeType> usedNodeTypes) {
		if (usedNodeTypes.size() > parallelism)
			throw new RuntimeException("Cannot use more resource types than replicas...");

		double currentSpeedup = computeSpeedup(inputRate, usedNodeTypes);

		return queueModel.responseTime(inputRate, instances.size(), currentSpeedup);
	}

	private double computeSpeedup(double inputRate, Set<NodeType> usedNodeTypes) {
		double currentSpeedup = Double.POSITIVE_INFINITY;
		switch (this.speedupCase) {
			case AVG:
				double totSpeedup = 0.0;
				for (NodeType nt : usedNodeTypes) {
					totSpeedup += nt.getCpuSpeedup();
				}
				currentSpeedup = totSpeedup / (double) usedNodeTypes.size();
				break;
			default:
				for (NodeType nt : usedNodeTypes)
					currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());
				break;
		}
		return currentSpeedup;
	}

	/*
	private double computeSpeedup(double inputRate) {
		List<Tuple2<NodeType, Double>> balancedInputRates = this.loadBalancer.balance(inputRate, this.instances);
		double currentSpeedup = Double.POSITIVE_INFINITY;

		for (Tuple2<NodeType, Double> bir : balancedInputRates) {

		}
	}
	*/

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

	public double getSloRespTime() { return sloRespTime; }

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
