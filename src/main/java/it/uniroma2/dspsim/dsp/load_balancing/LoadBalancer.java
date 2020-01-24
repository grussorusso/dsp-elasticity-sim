package it.uniroma2.dspsim.dsp.load_balancing;

import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.Tuple2;

import java.util.List;

public interface LoadBalancer {
    List<Tuple2<NodeType, Double>> balance(double totalInputRate, List<NodeType> instances);
}
