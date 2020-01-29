package it.uniroma2.dspsim.dsp.load_balancing;

import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.ArrayList;
import java.util.List;

public class RoundRobinLoadBalancer implements LoadBalancer {
    @Override
    public List<Tuple2<NodeType, Double>> balance(double totalInputRate, List<NodeType> instances) {
        double rrInputRate = totalInputRate / (double) instances.size();

        List<Tuple2<NodeType, Double>> balancing = new ArrayList<>();
        for (NodeType instance : instances) {
            balancing.add(new Tuple2<>(instance, rrInputRate));
        }

        return balancing;
    }
}
