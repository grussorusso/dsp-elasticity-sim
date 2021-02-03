package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class ApplicationBuilder {

	static private Logger log = LoggerFactory.getLogger(ApplicationBuilder.class);

	static public Application buildApplication()
	{
		Application application;
		String appName = Configuration.getInstance().getString(ConfigurationKeys.APPLICATION, "single-operator");

		if (appName.equalsIgnoreCase("single-operator")) {
			application = singleOperatorApplication(1.0);
		} else if (appName.equalsIgnoreCase("single-operator-fast")) {
			application = singleOperatorApplication(5.0);
		} else if (appName.equalsIgnoreCase("fromfile")) {
			application = buildApplicationFromFile();
		} else if (appName.equalsIgnoreCase("fork-join")) {
			application = buildForkJoinApplication();
		} else if (appName.equalsIgnoreCase("debs2019")) {
			application = buildDEBS2019Application();
		} else if (appName.equalsIgnoreCase("simple-tandem")) {
			application = simpleTandemApplication();
		} else if (appName.equalsIgnoreCase("simple-tree")) {
			application = simpleTreeApplication();
		} else {
			throw new RuntimeException("Invalid application: " + appName);
		}

		String amType = Configuration.getInstance().getString(ConfigurationKeys.AM_TYPE_KEY, "");
		if (!amType.equalsIgnoreCase("centralized"))
			computeOperatorsSLO(application);

		return application;
	}

	static public Application buildApplicationFromFile() {
		String appFilename = Configuration.getInstance().getString(ConfigurationKeys.APPLICATION_FILE_SPEC, "");
		if (appFilename.isEmpty())
			throw new RuntimeException("Invalid application file: " + appFilename);

		return buildApplicationFromFile(appFilename);
	}

	private static Application buildApplicationFromFile(String filename) {
		File file = new File(filename);
		Application app = new Application();
		Map<String, Operator> name2op = new HashMap<>();
		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);

		double appSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.1);

		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while ((line = br.readLine()) != null) {
				String tokens[] = line.split(",");
				tokens = Arrays.stream(tokens).map(String::trim).toArray(String[]::new);

				if (tokens[0].equalsIgnoreCase("OP")) {
					/* operator spec */
					if (tokens.length < 4)
						throw new RuntimeException("Invalid op line: " + Arrays.toString(tokens));

					String opName = tokens[1];
					final double opStMean = 1.0/Double.parseDouble(tokens[2]);
					final double opStSCV = Double.parseDouble(tokens[3]);
					final double opStVar = opStSCV*opStMean*opStMean;
					Operator op = new Operator(opName, new MG1OperatorQueueModel(opStMean, opStVar), maxParallelism);

					if (name2op.containsKey(opName))
						throw new RuntimeException("Already used operator name!");

					name2op.put(opName, op);
					app.addOperator(op);

					if (tokens.length > 4) {
						final double sloQuota = Double.parseDouble(tokens[4]);
						op.setSloRespTime(sloQuota * appSLO);
					}
				} else if (tokens[0].equalsIgnoreCase("EDGE")) {
					/* edge spec */
					if (tokens.length != 3)
						throw new RuntimeException("Invalid edge line: " + Arrays.toString(tokens));
					Operator op1 = name2op.get(tokens[1]);
					Operator op2 = name2op.get(tokens[2]);
					app.addEdge(op1, op2);
				}
			}

			log.info("Operators: {}", app.getOperators().size());
			log.info("Paths: {}", app.getAllPaths().size());
			return app;
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Invalid application file: " + filename);
		}
	}

	static public Application singleOperatorApplication(double muScalingFactor) {
		Application app = new Application();

		final double mu = 180.0 * muScalingFactor;
		final double serviceTimeMean = 1/mu;
		final double serviceTimeVariance = 1.0/mu*1.0/mu/2.0;

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);
		Operator op = new Operator("singleOp",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		app.addOperator(op);

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

		return app;
	}

	protected static void computeOperatorsSLO(Application app) {
		Configuration conf = Configuration.getInstance();
		double rSLO = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.1);

		String operatorSLOmethod = conf.getString(ConfigurationKeys.OPERATOR_SLO_COMPUTATION_METHOD, "");

		if (operatorSLOmethod.equalsIgnoreCase("fromfile")) {
			log.info("Assuming operator SLO has been provided in the app file.");

			if (!conf.getString(ConfigurationKeys.APPLICATION, "").equalsIgnoreCase("fromfile")) {
				throw new RuntimeException("Cannot use SLO fromfile option if the application is not read from file!")	;
			}
		} else if (operatorSLOmethod.equalsIgnoreCase("heuristic")) {
			log.info("Computing operators SLO using heuristic.");
			computeHeuristicOperatorSLO(app, false);
		} else if (operatorSLOmethod.equalsIgnoreCase("heuristic-approx")) {
			log.info("Computing operators SLO using heuristic-approx.");
			computeHeuristicOperatorSLO(app, true);
		} else if (operatorSLOmethod.equalsIgnoreCase("custom")) {
			String[] quotas = conf.getString(ConfigurationKeys.OPERATOR_SLO_COMPUTATION_CUSTOM_QUOTAS, "").
					split(",");
			if (app.getOperators().size() != quotas.length)
				throw new RuntimeException("Invalid quotas for computing operator SLO");

			for (int i = 0; i<app.getOperators().size(); i++) {
				final Operator op = app.getOperators().get(i);
				final double opSlo = rSLO * Double.parseDouble(quotas[i]);
				op.setSloRespTime(opSlo);
			}
		} else {
			log.info("Computing operators SLO using default method.");
			/* default */
			for (Operator op : app.getOperators()) {
				double opSlo = rSLO / app.getMaxPathLength(op);
				op.setSloRespTime(opSlo);
			}
		}

		for (Operator op : app.getOperators()) {
			log.info("SLO[{}] = {}", op.getName(), op.getSloRespTime());
		}
	}

	protected static void computeHeuristicOperatorSLO(Application app, boolean approximateModel) {
		double rSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);
		double inputRate = Configuration.getInstance().getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);

		inputRate = inputRate/2.0; // TODO

		Map<String, Double> opSLOMap = optimizeSLODivisionOnPaths(app, rSLO, inputRate, approximateModel);

		for (Operator op : app.getOperators()) {
			final double opSlo = opSLOMap.get(op.getName());
			op.setSloRespTime(opSlo);
			log.info("SLO[{}] = {}", op.getName(), opSlo);
		}
	}

	private static Map<String, Double> optimizeSLODivisionOnPaths(Application app, double applicationSLO,
																  double inputRate, boolean approximateModel) {
		// TODO: this is overly complex and also not correct: check pseudocode on my PhD thesis (Gab)
		
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

		// the heuristic needs a queueing model of each op
		Map<String, OperatorQueueModel> opQueueingModels = new HashMap<>();
		Random r = new Random();
		for (Operator op : app.getOperators()) {
			if (approximateModel) {
				opQueueingModels.put(op.getName(), op.getQueueModel().getApproximateModel(r, 0.05, 0.1));
			} else {
				opQueueingModels.put(op.getName(), op.getQueueModel());
			}
		}

		// optimize paths
		DoubleMatrix<Integer, String> sloApplicationMap = new DoubleMatrix<>(Double.POSITIVE_INFINITY);
		while (!done) {
			done = true;
			for (int i = 0; i < paths.size(); i++) {
				Tuple2<Map<String, Double>, Boolean> pathOptimizationResults = optimizePath(inputRate, applicationSLO,
						paths.get(i), opParallelismMap, opMaxParallelismMap, opQueueingModels);
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
			double pathSLOSum = 0.0;
			for (Double slo : sloApplicationMap.getRow(i).values())
				pathSLOSum += slo;

			double slack = applicationSLO - pathSLOSum;
			Map<String, Double> opSLOWithSlackMap =
					allocateSlack(slack, sloApplicationMap.getRow(i), paths.get(i));
			for (String opName : opSLOWithSlackMap.keySet()) {
				sloApplicationMap.setValue(i, opName, opSLOWithSlackMap.get(opName));
			}
		}

		// Give each operator minimum SLO across paths
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
		balanceSLO(applicationSLO, operatorSLOMap, paths);

		log.info("Heuristic SLO: {}", operatorSLOMap);
		log.info("Heuristic parallelism: {}", opParallelismMap);

		//for (String opName : opParallelismMap.keySet())
		//	System.out.println(String.format("%s\t->\t%d", opName, opParallelismMap.get(opName)));

		return operatorSLOMap;
	}

	private static Tuple2<Map<String, Double>, Boolean> optimizePath(double inputRate, double applicationSLO,
																	 ArrayList<Operator> path,
																	 Map<String, Integer> operatorParallelismMap,
																	 Map<String, Integer> operatorMaxParallelismMap,
																	 Map<String, OperatorQueueModel> opQueueingModels) {
		// flag
		boolean changed = false;

		// build path slo map
		Map<String, Double> pathSLOMap = new HashMap<>();
		while (true) {
			double pathRespTime = 0.0;
			for (Operator op : path) {
				// TODO operator input rate != source input rate
				// TODO speedup?
				OperatorQueueModel qn = opQueueingModels.get(op.getName());
				double opRespTime = qn.responseTime(inputRate / operatorParallelismMap.get(op.getName()), 1.0);
				//if (Double.isInfinite(opRespTime)) {
				//	pathRespTime = Double.POSITIVE_INFINITY;
				//	break;
				//}
				pathSLOMap.put(op.getName(), opRespTime);
				pathRespTime += opRespTime;
			}

			if (pathRespTime <= applicationSLO) {
				log.info("Path R = {}", pathRespTime);
				log.info("PathSLOMAP: {}", pathSLOMap);
				return new Tuple2<>(pathSLOMap, changed);
			}

			String bottleneckOperatorIndex = null;
			double maxDiff = 0.0;

			for (Operator op : path) {
				int opParallelism = operatorParallelismMap.get(op.getName());
				OperatorQueueModel qn = opQueueingModels.get(op.getName());
				double diff = Math.abs(qn.responseTime(inputRate / (opParallelism + 1), 1.0) - pathSLOMap.get(op.getName()));
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

	private static Map<String, Double> allocateSlack(double slack, Map<String, Double> sloMap, ArrayList<Operator> path) {
		Map<String, Double> opWeightMap = new HashMap<>();
		double totalWeight = 0.0;
		for (Operator op : path) {
			//double opWeight = op.getQueueModel().getServiceTimeMean(); // TODO
			double opWeight = sloMap.get(op.getName()); // TODO
			opWeightMap.put(op.getName(), opWeight);
			totalWeight += opWeight;
		}

		for (Operator op : path) {
			sloMap.put(op.getName(), sloMap.get(op.getName()) +
					(slack * (opWeightMap.get(op.getName()) / totalWeight)));
		}

		return sloMap;
	}

	private static void balanceSLO(double applicationSLO, Map<String, Double> opSloMap, List<ArrayList<Operator>> paths) {

		Map<String, Boolean> opSloAssignableMap = new HashMap<>();
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
				if (opSloAssignableMap.containsKey(op.getName()))
					continue;
				toBalance.add(op);
			}
			Map<String, Double> pathSloMap = new HashMap<>();
			for (Operator op : path) {
				pathSloMap.put(op.getName(), opSloMap.get(op.getName()));
			}
			Map<String, Double> slackMap = allocateSlack(slackPerPathList.get(j).getV(), pathSloMap, toBalance);

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
