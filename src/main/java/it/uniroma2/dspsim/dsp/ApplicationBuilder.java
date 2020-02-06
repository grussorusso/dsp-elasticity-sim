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

	static public Application multiOperatorApplication() {
		Application app = new Application();

		double[] mus = {350.0, 270.0, 180.0, 300.0, 250.0};
		double[] serviceTimeMeans = new double[mus.length];
		double[] serviceTimeVariances = new double[mus.length];

		for (int i = 0; i < mus.length; i++) {
			serviceTimeMeans[i] = 1/mus[i];
			serviceTimeVariances[i] = 1.0/mus[i]*1.0/mus[i]/2.0;
		}
		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op1 = new Operator("filter-1",
				new MG1OperatorQueueModel(serviceTimeMeans[0], serviceTimeVariances[0]), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("map",
				new MG1OperatorQueueModel(serviceTimeMeans[1], serviceTimeVariances[1]), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("reduce",
				new MG1OperatorQueueModel(serviceTimeMeans[2], serviceTimeVariances[2]), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("filter-2",
				new MG1OperatorQueueModel(serviceTimeMeans[3], serviceTimeVariances[3]), maxParallelism);
		app.addOperator(op4);
		Operator op5 = new Operator("rank",
				new MG1OperatorQueueModel(serviceTimeMeans[4], serviceTimeVariances[4]), maxParallelism);
		app.addOperator(op5);

		app.addEdge(op1, op2);
		app.addEdge(op2, op3);
		app.addEdge(op3, op4);
		app.addEdge(op3, op5);

		//computeOperatorsSlo(app);
		computeBalancedOperatorSLO(app);

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

		double[] mus = new double[]{1000.0, 400.0, 310.0, 460.0, 180.0, 12000.0};
        double[] serviceTimeVariances = new double[] {0.0001, 0.0001, 0.0001, 0.0001, 0.0001, 0.0001};

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op1 = new Operator("splitter",
				new MG1OperatorQueueModel(1/mus[0], serviceTimeVariances[0]), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("parallel1",
				new MG1OperatorQueueModel(1/mus[1], serviceTimeVariances[1]), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("parallel2",
				new MG1OperatorQueueModel(1/mus[2], serviceTimeVariances[2]), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("parallel3-1",
				new MG1OperatorQueueModel(1/mus[3], serviceTimeVariances[3]), maxParallelism);
		app.addOperator(op4);
		Operator op5 = new Operator("parallel3-2",
				new MG1OperatorQueueModel(1/mus[4], serviceTimeVariances[4]), maxParallelism);
		app.addOperator(op5);
		Operator op6 = new Operator("join",
				new MG1OperatorQueueModel(1/mus[5], serviceTimeVariances[5]), maxParallelism);
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
		//rSLO = 0.8 * rSLO;
		for (Operator op : app.getOperators()) {
			op.setSloRespTime(rSLO / app.getMaxPathLength(op));
		}
	}

	protected static void computeBalancedOperatorSLO(Application app) {
		double rSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);
		double inputRate = Configuration.getInstance().getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);

		Map<String, Double> opSLOMap = optimizeSLODivisionOnPaths(app, rSLO, inputRate);

		for (Operator op : app.getOperators()) {
			op.setSloRespTime(opSLOMap.get(op.getName()));
		}
	}

	private static Map<String, Double> optimizeSLODivisionOnPaths(Application app,
																  double applicationSLO, double inputRate) {
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
			Map<String, Double> opSLOWithSlackMap =
					allocateSlack(applicationSLO, sloApplicationMap.getRow(i), paths.get(i));
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

		// balance the remaining SLO
		Map<String, Boolean> opSloAssignableMap = new HashMap<>();
		balanceSLO(applicationSLO, operatorSLOMap, paths, opSloAssignableMap);


		for (String opName : opParallelismMap.keySet())
			System.out.println(String.format("%s\t->\t%d", opName, opParallelismMap.get(opName)));

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
				// TODO operator input rate != source input rate
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
                StringBuilder pathStr = new StringBuilder("Error in path: ");
                for (int j = 0; j < path.size(); j++) {
                    pathStr.append(path.get(j).getName());
                    if (j < path.size() - 1) {
                        pathStr.append(" -> ");
                    }
                }
                System.out.println(pathStr);
				System.out.println("Parallelism Map:");
				for (String opName : operatorParallelismMap.keySet())
					System.out.println(String.format("%s\t->\t%d", opName, operatorParallelismMap.get(opName)));
				throw new RuntimeException("Balancing not feasible: max parallelism has been reached");
			}
		}

	}

	private static Map<String, Double> allocateSlack(double applicationSLO,
													 Map<String, Double> sloMap, ArrayList<Operator> path) {
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

	private static void balanceSLO(double applicationSLO,
												  Map<String, Double> opSloMap, List<ArrayList<Operator>> paths,
												  Map<String, Boolean> opSloAssignableMap) {

		List<Tuple2<Integer, Double>> slackPerPathList = new ArrayList<>();

		for (int i = 0; i < paths.size(); i++) {
			double pathSLOSum = 0.0;
			for (Operator op : paths.get(i)) {
				pathSLOSum += opSloMap.get(op.getName());
			}
			double slack = applicationSLO - pathSLOSum;

			if (slack <= 0) {
				// mark operators as no slack assignable
				for (Operator op : paths.get(i)) {
					opSloAssignableMap.put(op.getName(), false);
				}
			}
			slackPerPathList.add(new Tuple2<>(i, slack));
		}

		for (int j = 0; j < paths.size(); j++) {
			// if slack is already equal or less than 0 jump this path
			if (slackPerPathList.get(j).getV() <= 0) continue;

			ArrayList<Operator> path = paths.get(j);
			ArrayList<Operator> toBalance = new ArrayList<>();
			for (Operator op : path) {
				// avoid operator in slack not assignable map
				if (opSloAssignableMap.containsKey(op.getName())) continue;
				toBalance.add(op);
			}
			Map<String, Double> pathSloMap = new HashMap<>();
			for (Operator op : path) {
				pathSloMap.put(op.getName(), opSloMap.get(op.getName()));
			}
			Map<String, Double> slackMap = allocateSlack(applicationSLO, pathSloMap, toBalance);

			for (Operator op : toBalance) {
				opSloMap.put(op.getName(), slackMap.get(op.getName()));
			}

			double pathSLOSum = 0.0;
			for (Operator op : path)
				pathSLOSum += opSloMap.get(op.getName());

			double slack = applicationSLO - pathSLOSum;

			if (slack <= 0) {
				for (Operator op : path) {
					opSloAssignableMap.put(op.getName(), false);
				}
			}
		}
	}
}
