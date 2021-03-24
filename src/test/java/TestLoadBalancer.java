import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancer;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancerFactory;
import it.uniroma2.dspsim.dsp.load_balancing.LoadBalancerType;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.Tuple2;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class TestLoadBalancer {

	@Test
	public void compareRRandHeuristic() {
		ComputingInfrastructure.initDefaultInfrastructure(3);
		ComputingInfrastructure infra = ComputingInfrastructure.getInfrastructure();


		double rates[] = {10.0, 15.0};
		final double parallelism = 5;

		List<NodeType> instances1 = new ArrayList<>();
		for (int i = 0; i< parallelism; i++) {
			instances1.add(infra.getNodeTypes()[0]);
		}

		List<NodeType> instances2 = new ArrayList<>();
		for (int i = 0; i< parallelism; i++) {
			instances2.add(infra.getNodeTypes()[i % infra.getNodeTypes().length]);
		}

		LoadBalancer lbheu = LoadBalancerFactory.getLoadBalancer(LoadBalancerType.HEURISTIC_LB);
		LoadBalancer lbrr = LoadBalancerFactory.getLoadBalancer(LoadBalancerType.ROUND_ROBIN_LB);

		for (double rate : rates) {
			printBalancingSol(lbrr.balance(rate, instances1));
			printBalancingSol(lbheu.balance(rate, instances1));
			System.out.println("---");
			printBalancingSol(lbrr.balance(rate, instances2));
			printBalancingSol(lbheu.balance(rate, instances2));
			System.out.println("====");
		}
	}

	private static void printBalancingSol (List<Tuple2<NodeType, Double>> pairs) {
		for (Tuple2 t : pairs)	{
			System.out.printf("%s -> %f; ", ((NodeType)t.getK()).getName(), t.getV());
		}
		System.out.println("");
	}

}
