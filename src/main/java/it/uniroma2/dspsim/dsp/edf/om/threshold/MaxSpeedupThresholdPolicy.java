package it.uniroma2.dspsim.dsp.edf.om.threshold;

import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

public class MaxSpeedupThresholdPolicy extends ThresholdPolicy {
    private NodeType maxSpeedupNodeType;

    public MaxSpeedupThresholdPolicy() {
        super();

        for (NodeType nt : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
            if (this.maxSpeedupNodeType == null || this.maxSpeedupNodeType.getCpuSpeedup() < nt.getCpuSpeedup())	{
                this.maxSpeedupNodeType = nt;
            }
        }
    }

    @Override
    protected NodeType selectNodeTypeToScale() {
        return this.maxSpeedupNodeType;
    }
}
