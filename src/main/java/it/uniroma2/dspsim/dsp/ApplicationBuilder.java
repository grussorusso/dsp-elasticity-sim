package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;

public class ApplicationBuilder {

	static public Application defaultApplication()
	{
		Application app = new Application();

		final int maxParallelism = 3;
		Operator op1 = new Operator("filter", new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("rank", new MG1OperatorQueueModel(1/50.0, 0.0), maxParallelism);
		app.addOperator(op2);

		app.addEdge(op1, op2);
		return app;
	}

	static public Application buildForkJoinApplication ()
	{
		Application app = new Application();

		final int maxParallelism = 3;
		Operator op1 = new Operator("splitter", new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op1);
		Operator op2 = new Operator("parallel1", new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op2);
		Operator op3 = new Operator("parallel2", new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op3);
		Operator op4 = new Operator("join", new MG1OperatorQueueModel(1/100.0, 0.0), maxParallelism);
		app.addOperator(op4);

		app.addEdge(op1, op2);
		app.addEdge(op1, op3);
		app.addEdge(op2, op4);
		app.addEdge(op3, op4);
		return app;
	}
}
