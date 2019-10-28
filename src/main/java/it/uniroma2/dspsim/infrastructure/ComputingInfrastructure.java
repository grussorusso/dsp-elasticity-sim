package it.uniroma2.dspsim.infrastructure;

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

	public NodeType[] getNodeTypes() {
		return nodeTypes;
	}
}
