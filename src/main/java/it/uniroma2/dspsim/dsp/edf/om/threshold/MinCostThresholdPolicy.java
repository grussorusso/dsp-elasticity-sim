package it.uniroma2.dspsim.dsp.edf.om.threshold;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

public class MinCostThresholdPolicy extends ThresholdPolicy {
    private NodeType minCostNodeType;

    public MinCostThresholdPolicy() {
        super();
        for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
            if (this.minCostNodeType == null || this.minCostNodeType.getCost() > nt.getCost())	{
                this.minCostNodeType = nt;
            }
        }
    }

    @Override
    protected NodeType selectNodeTypeToScale() {
        return this.minCostNodeType;
    }
}
