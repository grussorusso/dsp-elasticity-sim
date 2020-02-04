package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.utils.KeyValueStorage;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;

import java.util.*;

public class ApplicationBuilder {

	static public Application singleOperatorApplication() {
		Application app = new Application();

		final double mu = 180.0;
		final double serviceTimeMean = 1/mu;
		final double serviceTimeVariance = 1.0/mu*1.0/mu/2.0;

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op = new Operator("filter",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		app.addOperator(op);

		computeOperatorsSlo(app);

		return app;
	}

	static public Application defaultApplication()
	{
		Application app = new Application();

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);;
		Operator op1 = new Operator("filter",
				new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("rank",
				new MG1OperatorQueueModel(1/50.0, 0.0), maxParallelism);
		app.addOperator(op2);

		app.addEdge(op1, op2);

		computeOperatorsSlo(app);

		return app;
	}

	static public Application buildForkJoinApplication ()
	{
		Application app = new Application();

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);;
		Operator op1 = new Operator("splitter",
				new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("parallel1",
				new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("parallel2",
				new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("join",
				new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op4);

		app.addEdge(op1, op2);
		app.addEdge(op1, op3);
		app.addEdge(op2, op4);
		app.addEdge(op3, op4);

		computeOperatorsSlo(app);

		return app;
	}

	static public Application buildPaperApplication ()
	{
		Application app = new Application();

		double[] mus = new double[]{250.0, 300.0, 190.0, 120.0, 90.0, 1200.0};
		//double[] sigmas = new double[] {0.001, 0.1, 0.05, 0.3, 0.4, 0.001};
		double[] sigmas = new double[] {1,1,1,1,1,1};

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op1 = new Operator("splitter",
				new MG1OperatorQueueModel(1/mus[0], sigmas[0]), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("parallel1",
				new MG1OperatorQueueModel(1/mus[1], sigmas[1]), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("parallel2",
				new MG1OperatorQueueModel(1/mus[2], sigmas[2]), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("parallel3-1",
				new MG1OperatorQueueModel(1/mus[3], sigmas[3]), maxParallelism);
		app.addOperator(op4);
		Operator op5 = new Operator("parallel3-2",
				new MG1OperatorQueueModel(1/mus[4], sigmas[4]), maxParallelism);
		app.addOperator(op5);
		Operator op6 = new Operator("join",
				new MG1OperatorQueueModel(1/mus[5], sigmas[5]), maxParallelism);
		app.addOperator(op6);

		app.addEdge(op1, op2);
		app.addEdge(op1, op3);
		app.addEdge(op1, op4);
		app.addEdge(op4, op5);
		app.addEdge(op2, op6);
		app.addEdge(op3, op6);
		app.addEdge(op5, op6);

		computeBalancedOperatorSLO(app);

		return app;
	}

	protected static void computeOperatorsSlo(Application app) {
		double rSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.1);
		for (Operator op : app.getOperators()) {
			op.setSloRespTime(rSLO / app.getMaxPathLength(op));
		}
	}

	protected static void computeBalancedOperatorSLO(Application app) {
		double rSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.065);
		double inputRate = Configuration.getInstance().getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);

		Map<String, Double> opSLOMap = optimizeSLODivisionOnPaths(app, rSLO, inputRate);

		for (Operator op : app.getOperators()) {
			op.setSloRespTime(opSLOMap.get(op.getName()));
		}
	}

	private static Map<String, Double> optimizeSLODivisionOnPaths(Application app, double applicationSLO, double inputRate) {
		// get application slo
		DoubleMatrix<Integer, String> sloApplicationMap = new DoubleMatrix<>(Double.POSITIVE_INFINITY);
		// flag
		boolean done = false;
		// create operator parallelism map
		// init all values to 1
		Map<String, Integer> opParallelismMap = new HashMap<>();
		Map<String, Integer> opMaxParallelismMap = new HashMap<>();
		for (Operator op : app.getOperators()) {
			opParallelismMap.put(op.getName(), 1);
			opMaxParallelismMap.put(op.getName(), op.getMaxParallelism());
		}

		// get application paths
		List<ArrayList<Operator>> paths = (List<ArrayList<Operator>>) app.getAllPaths();
		// optimize paths
		while (!done) {
			done = true;
			for (int i = 0; i < paths.size(); i++) {
				Tuple2<Map<String, Double>, Boolean> pathOptimizationResults = optimizePath(inputRate, applicationSLO, paths.get(i), opParallelismMap, opMaxParallelismMap);
				for (String opName : pathOptimizationResults.getK().keySet()) {
					sloApplicationMap.setValue(i, opName, pathOptimizationResults.getK().get(opName));
				}
				if (pathOptimizationResults.getV()) {
					done = false;
				}
			}
		}

		// assign slack
		for (int i = 0; i < paths.size(); i++) {
			Map<String, Double> opSLOWithSlackMap = allocateSlack(applicationSLO, sloApplicationMap.getRow(i), paths.get(i));
			for (String opName : opSLOWithSlackMap.keySet()) {
				sloApplicationMap.setValue(i, opName, opSLOWithSlackMap.get(opName));
			}
		}

		// build operator slo map
		Map<String, Double> operatorSLOMap = new HashMap<>();
		for (int i = 0; i < paths.size(); i++) {
			if (i == 0)
				operatorSLOMap = sloApplicationMap.getRow(i);

			for (Operator op : paths.get(i)) {
				String opName = op.getName();

				if (!operatorSLOMap.containsKey(opName))
					operatorSLOMap.put(opName, sloApplicationMap.getValue(i, opName));

				operatorSLOMap.put(opName, Math.min(operatorSLOMap.get(opName), sloApplicationMap.getValue(i, opName)));
			}
		}

		return operatorSLOMap;
	}

	private static Tuple2<Map<String, Double>, Boolean> optimizePath(double inputRate, double applicationSLO,
																	 ArrayList<Operator> path,
																	 Map<String, Integer> operatorParallelismMap,
																	 Map<String, Integer> operatorMaxParallelismMap) {
		// flag
		boolean changed = false;

		// build path slo map
		Map<String, Double> pathSLOMap = new HashMap<>();
		while (true) {
			double pathRespTime = 0.0;
			for (Operator op : path) {
				double opRespTime = op.getQueueModel().responseTime(inputRate / operatorParallelismMap.get(op.getName()), 1.0);
				pathSLOMap.put(op.getName(), opRespTime);
				pathRespTime += opRespTime;
			}

			if (pathRespTime <= applicationSLO) {
				return new Tuple2<>(pathSLOMap, changed);
			}

			String bottleneckOperatorIndex = null;
			double maxDiff = 0.0;

			for (Operator op : path) {
				int opParallelism = operatorParallelismMap.get(op.getName());
				double diff = Math.abs(op.getQueueModel().responseTime(inputRate / (opParallelism + 1), 1.0) - pathSLOMap.get(op.getName()));
				if ((bottleneckOperatorIndex == null || diff > maxDiff || Double.isNaN(diff)) && opParallelism < operatorMaxParallelismMap.get(op.getName())) {
					maxDiff = diff;
					bottleneckOperatorIndex = op.getName();
				}
			}

			if (bottleneckOperatorIndex != null) {
				operatorParallelismMap.put(bottleneckOperatorIndex, operatorParallelismMap.get(bottleneckOperatorIndex) + 1);
				changed = true;
			} else {
				System.out.println("Parallelism Map:");
				for (String opName : operatorParallelismMap.keySet())
					System.out.println(String.format("%s\t->\t%d", opName, operatorParallelismMap.get(opName)));
				throw new RuntimeException("Balancing not feasible: max parallelism has been reached");
			}
		}

	}

	private static Map<String, Double> allocateSlack(double applicationSLO, Map<String, Double> sloMap, ArrayList<Operator> path) {
		double pathSLOSum = 0.0;
		for (Double slo : sloMap.values())
			pathSLOSum += slo;

		double slack = applicationSLO - pathSLOSum;

		Map<String, Double> serviceTimeMeanPathMap = new HashMap<>();
		double pathServiceTime = 0.0;
		for (Operator op : path) {
			double opServiceTimeMean = op.getQueueModel().getServiceTimeMean();
			serviceTimeMeanPathMap.put(op.getName(), opServiceTimeMean);
			pathServiceTime += opServiceTimeMean;
		}

		for (Operator op : path) {
			sloMap.put(op.getName(), sloMap.get(op.getName()) +
					(slack * (serviceTimeMeanPathMap.get(op.getName()) / pathServiceTime)));
		}

		return sloMap;
	}
}
