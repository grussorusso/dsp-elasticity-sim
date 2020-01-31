package it.uniroma2.dspsim.infrastructure;

import java.util.Arrays;

public class ComputingInfrastructure {

	private NodeType[] nodeTypes;

	private ComputingInfrastructure() {}

	static private ComputingInfrastructure infrastructure = null;

	static public ComputingInfrastructure getInfrastructure() {
		if (infrastructure != null)
			return infrastructure;

		throw new RuntimeException("Infrastructure has not been initialized!");
	}

	static public ComputingInfrastructure initDefaultInfrastructure (int numOfResTypes) {
		infrastructure = new ComputingInfrastructure();
		infrastructure.nodeTypes = new NodeType[numOfResTypes];

		for (int i = 0; i < numOfResTypes; i++) {
			final String name = String.format("Res-%d", i);
			final double cpuSpeedup = 1.0 + i*0.1;
			final double cost = cpuSpeedup;
			infrastructure.nodeTypes[i] = new NodeType(i, name,  cost, cpuSpeedup);
		}

		return infrastructure;
	}

	/**
	 * Gets first numOfResTypes in cpuSpeedups, sorts values,
	 * and creates an infrastructure using cpu speedups in sub-array.
	 * @param cpuSpeedups possible speedups
	 * @param numOfResTypes number of resources types in computing infrastructure
	 * @return ComputingInfrastructure
	 */
	static public ComputingInfrastructure initCustomInfrastructure(final double[] cpuSpeedups, int numOfResTypes) {
		infrastructure = new ComputingInfrastructure();
		infrastructure.nodeTypes = new NodeType[numOfResTypes];

		double[] cpuSpeedupsAvailable = Arrays.stream(cpuSpeedups, 0, numOfResTypes).sorted().toArray();
		System.out.println("Speedup: " + cpuSpeedupsAvailable[0]);


		for (int i = 0; i < cpuSpeedupsAvailable.length; i++) {
			infrastructure.nodeTypes[i] = new NodeType(i, String.format("Res-%d", i),
					cpuSpeedupsAvailable[i], cpuSpeedupsAvailable[i]);
		}

		return infrastructure;
	}

	public NodeType[] getNodeTypes() {
		return nodeTypes;
	}

	public NodeType getMostExpensiveResType () {
		Double cost = null;
		NodeType nodeType = null;

		for (NodeType nt : nodeTypes) {
			if (nodeType == null || nt.getCost() > cost) {
				cost = nt.getCost();
				nodeType = nt;
			}
		}

		return nodeType;
	}
}
