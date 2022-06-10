package it.uniroma2.dspsim.infrastructure;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManagerType;

import java.util.Arrays;
import java.util.Random;

public class ComputingInfrastructure {

	private NodeType[] nodeTypes;
	private Double costNormalizationConstant = null;

	/* A copy of nodeTypes with estimated speedups */
	private NodeType[] estimatedNodeTypes = null;

	private ComputingInfrastructure() {}

	static private ComputingInfrastructure infrastructure = null;

	static public ComputingInfrastructure getInfrastructure() {
		if (infrastructure != null)
			return infrastructure;

		throw new RuntimeException("Infrastructure has not been initialized!");
	}


	static private final double scenarioAspeedups[] = {1.0, 0.7, 1.3, 0.9, 1.7, 0.8, 1.8, 2.0, 1.65, 1.5};
	static private final double scenarioBspeedups[] = {1.0, 0.05, 30.0, 0.1, 0.2, 0.4, 0.8, 2.0, 5.0, 7.0};
	static private final double scenarioCspeedups[] = {1.0, 0.7, 1.3, 0.9, 1.7, 0.8, 1.8, 2.0, 1.65, 1.5,
	1.0, 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.55, 1.35, 1.25};


	static public ComputingInfrastructure initDefaultInfrastructure (int numOfResTypes) {
		infrastructure = new ComputingInfrastructure();
		infrastructure.nodeTypes = new NodeType[numOfResTypes];

		String confScenario = Configuration.getInstance().getString(ConfigurationKeys.NODE_TYPES_SCENARIO, "A");
		double speedups[] = null;
		if (confScenario.equalsIgnoreCase("A")) {
			speedups = scenarioAspeedups;
		} else if (confScenario.equalsIgnoreCase("B")) {
			speedups = scenarioBspeedups;
		} else if (confScenario.equalsIgnoreCase("B2")) {
			speedups = scenarioBspeedups;
		} else if (confScenario.equalsIgnoreCase("C")) {
			speedups = scenarioCspeedups;
		} else {
			speedups = new double[numOfResTypes];
			for (int i = 0; i < numOfResTypes; i++) {
				speedups[i] = 1.0 + i * 0.1; /* incremental (default) */
			}
		}

		for (int i = 0; i < numOfResTypes; i++) {
			final String name = String.format("Res-%d", i);
			double cpuSpeedup = speedups[i];
			double cost;
			if (confScenario.equalsIgnoreCase("B2")) {
				cost = Math.pow(cpuSpeedup, 1.5);
			}  else {
				cost = cpuSpeedup;
			}
			//double cost = Math.pow(cpuSpeedup, 1.3);
			System.out.printf("Speedup: %.2f, cost: %.2f\n", cpuSpeedup, cost);
			infrastructure.nodeTypes[i] = new NodeType(i, name,  cost, cpuSpeedup);
		}

		/* Set cost normalization constant */
		infrastructure.costNormalizationConstant = Arrays.stream(speedups).max().getAsDouble();
		//System.out.printf("Cost normalization constant: %.2f\n", infrastructure.costNormalizationConstant);

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

	public NodeType getCheapestResType () {
		Double cost = null;
		NodeType nodeType = null;

		for (NodeType nt : nodeTypes) {
			if (nodeType == null || nt.getCost() < cost) {
				cost = nt.getCost();
				nodeType = nt;
			}
		}

		return nodeType;
	}

	public NodeType getDefaultNodeType() {
		try {
			OperatorManagerType omType = OperatorManagerType.fromString(Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""));
			if (omType == OperatorManagerType.THRESHOLD_BASED) {
				String resSelectionPolicy = Configuration.getInstance().getString(ConfigurationKeys.OM_BASIC_RESOURCE_SELECTION, "cost");
				if (resSelectionPolicy.equalsIgnoreCase("speedup")) {
					return getMostExpensiveResType();
				} else if (resSelectionPolicy.equalsIgnoreCase("cost")) {
					return getCheapestResType();
				}
			}
		} catch (IllegalArgumentException e) {
		}

		return this.nodeTypes[0];
	}

	public double getCostNormalizationConstant() {
		if (this.costNormalizationConstant == null)
			this.costNormalizationConstant = getMostExpensiveResType().getCost();

		return this.costNormalizationConstant;
	}

	public NodeType[] getEstimatedNodeTypes() {
		if (this.estimatedNodeTypes == null) {
			initEstimatedNodeTypes();
		}

		return this.estimatedNodeTypes;
	}

	private void initEstimatedNodeTypes() {
		Configuration conf = Configuration.getInstance();
		final int errorSeed = conf.getInteger(ConfigurationKeys.APPROX_SPEEDUPS_SEED, 123);
		final double maxRelError = conf.getDouble(ConfigurationKeys.APPROX_SPEEDUPS_MAX_REL_ERR, 0.2);

		Random rng = new Random(errorSeed);

		this.estimatedNodeTypes = new NodeType[this.nodeTypes.length];

		// Sort nodeTypes based on speedup
		NodeType[] sortedNodeTypes = this.nodeTypes.clone();
		Arrays.sort(sortedNodeTypes);

		for (int i = 0; i<sortedNodeTypes.length; i++) {
			final NodeType current = sortedNodeTypes[i];

			final double maxAbsError = maxRelError * current.getCpuSpeedup();
			double lb = current.getCpuSpeedup() - maxAbsError;
			double ub = current.getCpuSpeedup() + maxAbsError;
			if (i > 0)
				lb = Math.max(lb, sortedNodeTypes[i-1].getCpuSpeedup());
			if (i < sortedNodeTypes.length - 1)
				ub = Math.min(ub, sortedNodeTypes[i+1].getCpuSpeedup());

			final double newSpeedup = lb + rng.nextDouble()*(ub-lb);
			System.out.printf("Estimated speedup: %.2f -> %.2f\n", sortedNodeTypes[i].getCpuSpeedup(), newSpeedup);
			this.estimatedNodeTypes[current.getIndex()] = new NodeType(current.getIndex(), current.getName(), current.getCost(), newSpeedup);
		}
	}
}
