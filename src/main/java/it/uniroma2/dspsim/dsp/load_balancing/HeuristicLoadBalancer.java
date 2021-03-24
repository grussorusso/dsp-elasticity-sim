package it.uniroma2.dspsim.dsp.load_balancing;

import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.ArrayList;
import java.util.List;

public class HeuristicLoadBalancer implements LoadBalancer {
    @Override
    public List<Tuple2<NodeType, Double>> balance(double totalInputRate, List<NodeType> instances) {
        final double totalSpeedup = instances.stream().mapToDouble(NodeType::getCpuSpeedup).sum();

        List<Tuple2<NodeType, Double>> balancing = new ArrayList<>();
        for (NodeType instance : instances) {
            balancing.add(new Tuple2<>(instance, totalInputRate * instance.getCpuSpeedup() / totalSpeedup));
        }

        return balancing;
    }
}
