package it.uniroma2.dspsim.infrastructure;

/**
 * Type of computing node.
 */
public class NodeType implements Comparable<NodeType> {

	private String name;
	private double cost;
	private double cpuSpeedup;
	private int index;

	public NodeType (int index, String name, double cost, double cpuSpeedup) {
		this.index = index;
		this.name = name;
		this.cost = cost;
		this.cpuSpeedup = cpuSpeedup;
	}

	public int getIndex() {
		return this.index;
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

	public void setCpuSpeedup(double cpuSpeedup) {
		this.cpuSpeedup = cpuSpeedup;
	}

	public void setCost (double cost) {
		this.cost = cost;
	}

	@Override
	public int compareTo(NodeType o) {
		return Double.compare(this.cpuSpeedup, o.cpuSpeedup);
	}
}
