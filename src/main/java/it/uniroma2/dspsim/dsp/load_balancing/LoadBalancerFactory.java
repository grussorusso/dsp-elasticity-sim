package it.uniroma2.dspsim.dsp.load_balancing;

public class LoadBalancerFactory {

    private LoadBalancerFactory() {}

    public static LoadBalancer getLoadBalancer(LoadBalancerType type) {
        switch (type) {
            case ROUND_ROBIN_LB:
                return new RoundRobinLoadBalancer();
            case HEURISTIC_LB:
                return new HeuristicLoadBalancer();
            default:
                throw new IllegalArgumentException("Load Balancer type not known");
        }
    }
}
