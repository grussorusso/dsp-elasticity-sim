package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.*;

public class Application {

	private List<Operator> operators = new ArrayList<>();
	private List<ArrayList<Operator>> sourceSinkPaths;

	public Application() {}


	public void addOperator (Operator op) {
		operators.add(op);
		computeSourceSinkPaths();
	}

	public void addEdge (Operator op1, Operator op2) {
		op1.addDownstream(op2);
		op2.addUpstream(op1);

		computeSourceSinkPaths();
	}

	private void computeSourceSinkPaths() {
		sourceSinkPaths = new ArrayList<>();

		for (Operator op1 : operators)	{
			if (!op1.isSource())
				continue;
			for (Operator op2 : operators) {
				if (!op2.isSink())
					continue;

				sourceSinkPaths.addAll(computeSourceSinkPaths(op1,op2));
			}
		}
	}

	private Collection<ArrayList<Operator>> computeSourceSinkPaths(Operator src, Operator sink) {
		ArrayList<ArrayList<Operator>> paths = new ArrayList<>();
		LinkedList<ArrayList<Operator>> pathsQueue = new LinkedList<>();
		ArrayList<Operator> path = new ArrayList<>();

		path.add(src);
		pathsQueue.addLast(path);

		while(!pathsQueue.isEmpty()) {
			path = pathsQueue.getFirst();
			pathsQueue.removeFirst();

			Operator last = path.get(path.size()-1);
			if (last == sink) {
				paths.add(path);
			}

			for (Operator op : last.getDownstreamOperators()) {
				/* Check if it has already been visited. */
				boolean visited = false;
				for (Operator n : path) {
					if (n == op){
						visited = true;
						break;
					}
				}

				if (!visited) {
					ArrayList<Operator> newPath = new ArrayList<>(path);
					newPath.add(op);
					pathsQueue.addLast(newPath);
				}
			}
		}

		return paths;
	}

	public Collection<ArrayList<Operator>> getAllPaths()
	{
		return sourceSinkPaths;
	}

	public double endToEndLatency (double inputRate) {
        double latency = 0.0;

        for (ArrayList<Operator> path : sourceSinkPaths) {
        	latency = Math.max(latency, endToEndLatency(inputRate, path));
		}

		return latency;
	}

	private double endToEndLatency (double inputRate, ArrayList<Operator> path) {
		double latency = 0.0;

		for (Operator op : path) {
			double operatorRespTime = op.responseTime(inputRate);
			latency += operatorRespTime;
		}


		return latency;
	}

	public List<Operator> getOperators() {
		return operators;
	}

	public List<ArrayList<Operator>> getSourceSinkPaths() {
		return sourceSinkPaths;
	}

	public double computeDeploymentCost() {
		double totalCost = 0.0;
		for (Operator op : getOperators())
			totalCost += op.computeDeploymentCost();

		return totalCost;
	}

	public double computeMaxDeploymentCost() {
		double totalCost = 0.0;
		for (Operator op : getOperators())
			totalCost += op.computeMaxDeploymentCost();

		return totalCost;
	}
}
