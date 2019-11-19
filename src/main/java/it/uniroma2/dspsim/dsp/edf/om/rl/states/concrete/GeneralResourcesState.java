package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import scala.reflect.macros.Infrastructure;

import java.util.Arrays;

public class GeneralResourcesState extends State {

    private int lambdaLevel;

    private double normalizedCPUSpeedup;

    private double maxCPUSpeedupInUse;

    private double minCPUSpeedupInUse;

    public GeneralResourcesState(int[] k, int lambda, Operator operator) {
        super(k, lambda, operator);

        this.lambdaLevel = lambda;

        this.maxCPUSpeedupInUse = ComputingInfrastructure.getInfrastructure().getNodeTypes()[maxNodeInUse(k)].getCpuSpeedup();
        this.minCPUSpeedupInUse = ComputingInfrastructure.getInfrastructure().getNodeTypes()[minNodeInUse(k)].getCpuSpeedup();

        this.normalizedCPUSpeedup = computeSpeedupInUse(k) / computeMaxSpeedup(operator);
    }

    private int maxNodeInUse(int[] k) {
        int index = 0;
        for (int i = 0; i < k.length; i++) {
            if (k[i] > 0)
                index = i;
        }
        return index;
    }

    private int minNodeInUse(int[] k) {
        int index;
        for (index = 0; index < k.length; index++) {
            if (k[index] > 0)
                return index;
        }
        return k.length - 1;
    }

    private double computeSpeedupInUse(int[] k) {
        double speedup = 0.0;
        for (int i = 0; i < k.length; i++) {
            speedup += ComputingInfrastructure.getInfrastructure().getNodeTypes()[i].getCpuSpeedup() * k[i];
        }
        return speedup;
    }

    private double computeMaxSpeedup(Operator o) {
        double max = 0.0;

        for (NodeType nodeType : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
            if (nodeType.getCpuSpeedup() > max)
                max = nodeType.getCpuSpeedup();
        }

        return max * o.getMaxParallelism();
    }
}
