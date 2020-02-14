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

	// computing case of values like response time and utilization
	private enum ValuesComputingCase {
		AVG,
		WORST;

		public static ValuesComputingCase fromString(String str) {
			if (str.equalsIgnoreCase("avg")) {
				return AVG;
			} else if (str.equalsIgnoreCase("worst")) {
				return WORST;
			} else {
				throw new IllegalArgumentException("Not valid response time computing type " + str);
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
	private ValuesComputingCase valuesComputingCase;

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
		this.valuesComputingCase = ValuesComputingCase.fromString(Configuration.getInstance().getString(
				ConfigurationKeys.OPERATOR_VALUES_COMPUTING_CASE_KEY, "worst"));

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
		return utilization(inputRate, this.instances);
	}

	public double responseTime(double inputRate) {
		return responseTime(inputRate, this.instances);
	}

	public double utilization(double inputRate, List<NodeType> operatorInstances) {
		List<Tuple2<NodeType, Double>> inputRates = loadBalancer.balance(inputRate, operatorInstances);

		double[] perReplicaInputRate = new double[inputRates.size()];
		double[] perReplicaUtilization = new double[inputRates.size()];
		for (int i = 0; i < inputRates.size(); i++) {
			Tuple2<NodeType, Double> perReplicaRate = inputRates.get(i);
			perReplicaInputRate[i] = perReplicaRate.getV();
			perReplicaUtilization[i] = queueModel.utilization(perReplicaRate.getV(), perReplicaRate.getK().getCpuSpeedup());
		}

		return computeValueConsideringCase(inputRate, perReplicaInputRate, perReplicaUtilization);
	}

	public double responseTime(double inputRate, List<NodeType> operatorInstances) {
		List<Tuple2<NodeType, Double>> inputRates = loadBalancer.balance(inputRate, operatorInstances);

		double[] perReplicaInputRate = new double[inputRates.size()];
		double[] perReplicaRespTime = new double[inputRates.size()];
		for (int i = 0; i < inputRates.size(); i++) {
			Tuple2<NodeType, Double> perReplicaRate = inputRates.get(i);
			perReplicaInputRate[i] = perReplicaRate.getV();
			perReplicaRespTime[i] = queueModel.responseTime(perReplicaRate.getV(), perReplicaRate.getK().getCpuSpeedup());
		}

		return computeValueConsideringCase(inputRate, perReplicaInputRate, perReplicaRespTime);
	}

	private double computeValueConsideringCase(double totalIR, double[] partialIR, double[] values) {
		switch (this.valuesComputingCase) {
			case AVG:
				double rt = 0.0;
				for (int i = 0; i < values.length; i++) {
					rt += (partialIR[i] / totalIR) * values[i];
				}
				return rt;
			default:
				OptionalDouble max = Arrays.stream(values).max();
				if (max.isPresent()) {
					return max.getAsDouble();
				} else {
					throw new RuntimeException("Error while computing response time");
				}
		}
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

	public int[] getCurrentDeployment()
	{
		int resTypes = ComputingInfrastructure.getInfrastructure().getNodeTypes().length;
		int deployment[] = new int[resTypes];

		for (NodeType nt : instances) {
			deployment[nt.getIndex()]++;
		}

		return deployment;
	}

	public double getCurrentMaxThroughput () {
		double thr = 0.0;
		for (NodeType nt : instances) {
			thr += 1.0 / queueModel.getServiceTimeMean() * nt.getCpuSpeedup();
		}
		return thr;
	}

	public double getMaxThroughput (int[] deployment) {
		ComputingInfrastructure infrastructure = ComputingInfrastructure.getInfrastructure();

		double thr = 0.0;
		for (int i = 0; i<deployment.length; i++) {
			thr += deployment[i] * 1.0 / queueModel.getServiceTimeMean() * infrastructure.getNodeTypes()[i].getCpuSpeedup();

			if (infrastructure.getNodeTypes()[i].getIndex() != i)
				throw new RuntimeException("We are doing something wrong here!");
		}
		return thr;
	}
}
