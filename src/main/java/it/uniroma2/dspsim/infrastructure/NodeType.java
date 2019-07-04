package it.uniroma2.dspsim.infrastructure;

/**
 * Type of computing node.
 */
public class NodeType {

	private String name;
	private double cost;
	private double cpuSpeedup;

	public NodeType (String name, double cost, double cpuSpeedup) {
		this.name = name;
		this.cost = cost;
		this.cpuSpeedup = cpuSpeedup;
	}

	public String getName() {
		return name;
	}

	public double getCost() {
		return cost;
	}

	public double getCpuSpeedup() {
		return cpuSpeedup;
	}

}
