package it.uniroma2.dspsim.dsp;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Application {

	static private Logger logger = LoggerFactory.getLogger(Application.class);

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
			// TODO: Shouldn't we compute the inputRate for each operator based on selectivity?
			double operatorRespTime = op.responseTime(inputRate);
			//logger.info("Operator {} : response time {}", op.getName(), operatorRespTime);
			latency += operatorRespTime;
		}


		return latency;
	}

	public double endToEndLatency (Map<Operator, Double> opRespTime) {
		double latency = 0.0;

		for (ArrayList<Operator> path : sourceSinkPaths) {
			latency = Math.max(latency, endToEndLatency(opRespTime, path));
		}

		return latency;
	}

	private double endToEndLatency (Map<Operator, Double> opRespTime, ArrayList<Operator> path) {
		double latency = 0.0;

		for (Operator op : path) {
			double operatorRespTime = opRespTime.get(op);
			latency += operatorRespTime;
		}


		return latency;
	}

	public List<Operator> getOperators() {
		return operators;
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

    public double getMaxPathLength(Operator operator) {
		int length = 1;
		for (ArrayList<Operator> path : this.sourceSinkPaths) {
			for (Operator op : path) {
				if (operator.equals(op)) {
					length = Math.max(length, path.size());
					break;
				}
			}
		}
		return length;
    }

	/**
	 * Compute operator deployment as array representing the number of instances in each node type
	 * @param operator target operator
	 * @return deployment
	 */
	public int[] computeOperatorDeployment(Operator operator) {
		int[] deployment = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
		List<NodeType> opInstances = operator.getInstances();
		for (int i = 0; i < opInstances.size(); i++) {
			deployment[opInstances.get(i).getIndex()]++;
		}
		return deployment;
	}

	/**
	 * Return sum of operator replicas for each node type
	 * @return
	 */
	public int[] computeGlobalDeployment() {
		int[] globalDeployment = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];

		for (Operator operator : operators) {
			int[] opDeployment = computeOperatorDeployment(operator);
			for (int j = 0; j < opDeployment.length; j++) {
				globalDeployment[j] += opDeployment[j];
			}
		}

		return globalDeployment;
    }
}
