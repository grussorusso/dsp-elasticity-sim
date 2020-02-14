package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ApplicationBuilder {

	static private Logger log = LoggerFactory.getLogger(ApplicationBuilder.class);

	static public Application buildApplication()
	{
		String appName = Configuration.getInstance().getString(ConfigurationKeys.APPLICATION, "single-operator");
		if (appName.equalsIgnoreCase("single-operator")) {
			return singleOperatorApplication();
		} else if (appName.equalsIgnoreCase("fork-join")) {
			return buildForkJoinApplication();
		} else if (appName.equalsIgnoreCase("debs2019")) {
			return buildDEBS2019Application();
		} else if (appName.equalsIgnoreCase("simple-tandem")) {
			return simpleTandemApplication();
		} else if (appName.equalsIgnoreCase("pipeline3")) {
			return pipeline3Application();
		} else if (appName.equalsIgnoreCase("pipeline4")) {
			return pipeline4Application();
		} else if (appName.equalsIgnoreCase("pipeline5")) {
			return pipeline5Application();
		} else if (appName.equalsIgnoreCase("simple-tree")) {
			return simpleTreeApplication();
		}

		throw new RuntimeException("Invalid application: " + appName);
	}

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

		computeOperatorsSLO(app);

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

		computeOperatorsSLO(app);

		return app;
	}

	static private Application buildMM1Pipeline (double mu[])
	{
		Application app = new Application();


		int N = mu.length;
		Operator ops[] = new Operator[N];
		final int maxParallelism = Configuration.getInstance() .getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);

		for (int i = 0; i< N; i++) {
			final double stMean = 1.0/mu[i];
			final double stVar = stMean*stMean;
			String name = String.format("op%d", i+1);
			ops[i] = new Operator(name, new MG1OperatorQueueModel(stMean, stVar), maxParallelism);
			app.addOperator(ops[i]);

			if (i > 0) {
				app.addEdge(ops[i-1], ops[i]);
			}
		}

		computeOperatorsSLO(app);

		return app;
	}

	static public Application simpleTandemApplication() {
		Application app = new Application();

		final double mu = 10.0;
		//final double mu = 200.0;
		final double serviceTimeMean = 1/mu;
		final double serviceTimeVariance = 1.0/mu*1.0/mu;

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op1 = new Operator("op1",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		Operator op2 = new Operator("op2",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		app.addOperator(op1);
		app.addOperator(op2);
		app.addEdge(op1, op2);

		computeOperatorsSLO(app);

		return app;
	}

	static public Application pipeline3Application() {
		final double mu0 = 200.0;
		final double mu[] = {mu0, 1.5*mu0, 0.9*mu0};
		return buildMM1Pipeline(mu);
	}

	static public Application pipeline4Application() {
		final double mu0 = 250.0;
		final double mu[] = {mu0, 2.0*mu0, 10.0*mu0, 1.5*mu0};
		return buildMM1Pipeline(mu);
	}

	static public Application pipeline5Application() {
		final double mu0 = 250.0;
		final double mu[] = {mu0, 2.0*mu0, 10.0*mu0, 1.5*mu0, mu0};
		return buildMM1Pipeline(mu);
	}

	static public Application simpleTreeApplication() {
		Application app = new Application();

		final double mu = 200.0;
		final double serviceTimeMean = 1/mu;
		final double serviceTimeVariance = 1.0/mu*1.0/mu;

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);

		Operator op1 = new Operator("op1",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		Operator op2 = new Operator("op2",
				new MG1OperatorQueueModel(serviceTimeMean*0.7, serviceTimeVariance*0.49), maxParallelism);
		Operator op3 = new Operator("op3",
				new MG1OperatorQueueModel(serviceTimeMean/5.0, serviceTimeVariance), maxParallelism);
		app.addOperator(op1);
		app.addOperator(op2);
		app.addOperator(op3);
		app.addEdge(op1, op2);
		app.addEdge(op1, op3);

		computeOperatorsSLO(app);

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

		computeOperatorsSLO(app);

		return app;
	}

	static public Application buildDEBS2019Application()
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

		computeOperatorsSLO(app);

		return app;
	}

	protected static void computeOperatorsSLO(Application app) {
		Configuration conf = Configuration.getInstance();
		double rSLO = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.1);

		String operatorSLOmethod = conf.getString(ConfigurationKeys.OPERATOR_SLO_COMPUTATION_METHOD, "");

		if (operatorSLOmethod.equalsIgnoreCase("heuristic")) {
			log.info("Computing operators SLO using heuristic.");
			computeHeuristicOperatorSLO(app);
		} else {
			log.info("Computing operators SLO using default method.");
			/* default */
			for (Operator op : app.getOperators()) {
				op.setSloRespTime(rSLO / app.getMaxPathLength(op));
			}
		}
	}

	protected static void computeHeuristicOperatorSLO(Application app) {
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


		//for (String opName : opParallelismMap.keySet())
		//	System.out.println(String.format("%s\t->\t%d", opName, opParallelismMap.get(opName)));

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
