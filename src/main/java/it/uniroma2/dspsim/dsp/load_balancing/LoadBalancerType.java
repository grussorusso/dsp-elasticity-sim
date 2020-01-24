package it.uniroma2.dspsim.dsp.load_balancing;

public enum LoadBalancerType {
    ROUND_ROBIN_LB;

    public static LoadBalancerType fromString(String str) {
        if (str.equalsIgnoreCase("rr")) {
            return ROUND_ROBIN_LB;
        } else {
            throw new IllegalArgumentException("Not valid load balancer type " + str);
        }
    }
}
