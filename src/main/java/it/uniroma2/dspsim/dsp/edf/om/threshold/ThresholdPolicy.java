package it.uniroma2.dspsim.dsp.edf.om.threshold;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.infrastructure.NodeType;

public abstract class ThresholdPolicy {
    public Reconfiguration applyThresholdPolicy(double utilization, double instanceNumber,
                                                Operator operator, double scaleOutThreshold) {
        if (utilization > scaleOutThreshold && instanceNumber < operator.getMaxParallelism()) {
            /* scale-out */
            return Reconfiguration.scaleOut(selectNodeTypeToScale());
        } else if (instanceNumber > 1 && utilization*instanceNumber/(instanceNumber-1) < 0.75 * scaleOutThreshold) {
            /* scale-in */
            return Reconfiguration.scaleIn(selectNodeTypeToScale());
        }

        return Reconfiguration.doNothing();
    }

    protected abstract NodeType selectNodeTypeToScale();
}
