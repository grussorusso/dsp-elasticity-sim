package it.uniroma2.dspsim.dsp.load_balancing;

public class LoadBalancerFactory {

    private LoadBalancerFactory() {}

    public static LoadBalancer getLoadBalancer(LoadBalancerType type) {
        switch (type) {
            case ROUND_ROBIN_LB:
                return new RoundRobinLoadBalancer();
            default:
                throw new IllegalArgumentException("Load Balancer type not known");
        }
    }
}
