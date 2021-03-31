package it.uniroma2.dspsim.infrastructure;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;

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


	static private final double scenarioAspeedups[] = {1.0, 0.7, 1.3, 0.9, 1.7, 0.8, 1.8, 2.0, 1.65, 1.5};
	static private final double scenarioBspeedups[] = {1.0, 0.05, 30.0, 0.1, 0.2, 0.4, 0.8, 2.0, 5.0, 10.0};


	static public ComputingInfrastructure initDefaultInfrastructure (int numOfResTypes) {
		infrastructure = new ComputingInfrastructure();
		infrastructure.nodeTypes = new NodeType[numOfResTypes];

		String confScenario = Configuration.getInstance().getString(ConfigurationKeys.NODE_TYPES_SCENARIO, "A");

		for (int i = 0; i < numOfResTypes; i++) {
			final String name = String.format("Res-%d", i);
			double cpuSpeedup;
			if (confScenario.equalsIgnoreCase("A")) {
				cpuSpeedup = scenarioAspeedups[i];
			} else if (confScenario.equalsIgnoreCase("B")) {
				cpuSpeedup = scenarioBspeedups[i];
			} else {
				cpuSpeedup = 1.0 + i*0.1; /* incremental (default) */
			}
			System.out.printf("Speedup: %f\n", cpuSpeedup);
			final double cost = cpuSpeedup;
			infrastructure.nodeTypes[i] = new NodeType(i, name,  cost, cpuSpeedup);
		}

		return infrastructure;
	}

	static public ComputingInfrastructure initDefaultInfrastructure () {
		int numOfResTypes = Configuration.getInstance().getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3);
		return initDefaultInfrastructure(numOfResTypes);
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
