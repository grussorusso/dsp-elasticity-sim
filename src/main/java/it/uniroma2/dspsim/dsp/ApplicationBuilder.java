package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;

import java.util.Random;

public class ApplicationBuilder {

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

		computeOperatorsSlo(app);

		return app;
	}

	static public Application simpleTandemApplication() {
		Application app = new Application();

		final double mu = 10.0;
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

		computeOperatorsSlo(app);

		return app;
	}

	static public Application simpleTreeApplication() {
		Application app = new Application();

		final double mu = 350.0;
		final double serviceTimeMean = 1/mu;
		final double serviceTimeVariance = 1.0/mu*1.0/mu;

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);

		Operator op1 = new Operator("op1",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		Operator op2 = new Operator("op2",
				new MG1OperatorQueueModel(serviceTimeMean, serviceTimeVariance), maxParallelism);
		Operator op3 = new Operator("op3",
				new MG1OperatorQueueModel(serviceTimeMean/5.0, serviceTimeVariance), maxParallelism);
		app.addOperator(op1);
		app.addOperator(op2);
		app.addOperator(op3);
		app.addEdge(op1, op2);
		app.addEdge(op1, op3);

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

	static public Application buildDEBS2019Application()
	{
		Application app = new Application();

		Random rng = new Random(5678);

		double[] mus = {200.0, 300.0, 250.0, 250.0, 250.0, 2000.0};

		final int maxParallelism = Configuration.getInstance()
				.getInteger(ConfigurationKeys.OPERATOR_MAX_PARALLELISM_KEY, 3);;
		Operator op1 = new Operator("splitter",
				new MG1OperatorQueueModel(1/mus[0], computeVariance(mus[0], rng)), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("parallel1",
				new MG1OperatorQueueModel(1/mus[1], computeVariance(mus[1], rng)), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("parallel2",
				new MG1OperatorQueueModel(1/mus[2], computeVariance(mus[2], rng)), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("parallel3-1",
				new MG1OperatorQueueModel(1/mus[3], computeVariance(mus[3], rng)), maxParallelism);
		app.addOperator(op4);
		Operator op5 = new Operator("parallel3-2",
				new MG1OperatorQueueModel(1/mus[4], computeVariance(mus[4], rng)), maxParallelism);
		app.addOperator(op5);
		Operator op6 = new Operator("join",
				new MG1OperatorQueueModel(1/mus[5], computeVariance(mus[5], rng)), maxParallelism);
		app.addOperator(op6);

		app.addEdge(op1, op2);
		app.addEdge(op1, op3);
		app.addEdge(op1, op4);
		app.addEdge(op4, op5);
		app.addEdge(op2, op6);
		app.addEdge(op3, op6);
		app.addEdge(op5, op6);

		computeOperatorsSlo(app);

		return app;
	}

	@Deprecated
	private static double computeVariance(double mu, Random rng) {
		return 0.0;
	}

	private static void computeOperatorsSlo(Application app) {
		double rSLO = Configuration.getInstance().getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.1);
		for (Operator op : app.getOperators()) {
			op.setSloRespTime(rSLO / app.getMaxPathLength(op));
		}
	}
}
