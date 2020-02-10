package it.uniroma2.dspsim.dsp.edf.om.threshold;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.List;
import java.util.Random;

public class RandomSelectionThresholdPolicy extends ThresholdPolicy {

    private Random random;
    private NodeType[] nodeTypes;

    public RandomSelectionThresholdPolicy() {
        super();

        this.random = new Random();

        this.nodeTypes = ComputingInfrastructure.getInfrastructure().getNodeTypes();
    }

    @Override
    public Reconfiguration applyThresholdPolicy(double utilization, double instanceNumber, Operator operator, double scaleOutThreshold) {
        if (utilization > scaleOutThreshold && instanceNumber < operator.getMaxParallelism()) {
            /* scale-out */
            NodeType nodeToScale = selectNodeTypeToScale();
            return Reconfiguration.scaleOut(nodeToScale);
        } else if (instanceNumber > 1 && utilization*instanceNumber/(instanceNumber-1) < 0.75 * scaleOutThreshold) {
            /* scale-in */
            NodeType nodeToScale = selectNodeTypeToScale();
            while (!operator.getInstances().contains(nodeToScale)) nodeToScale = selectNodeTypeToScale();
            return Reconfiguration.scaleIn(nodeToScale);
        }

        return Reconfiguration.doNothing();
    }

    @Override
    protected NodeType selectNodeTypeToScale() {

        return this.nodeTypes[this.random.nextInt(this.nodeTypes.length)];
    }
}
