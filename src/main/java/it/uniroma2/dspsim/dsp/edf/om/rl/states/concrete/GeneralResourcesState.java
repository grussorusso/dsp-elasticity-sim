package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import scala.reflect.macros.Infrastructure;

import java.util.Arrays;
import java.util.Objects;

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

        this.normalizedCPUSpeedup = computeCPUSpeedupInUse(k) / computeMaxCPUSpeedup(operator);
    }

    /**
     * Computes biggest node in use by operator at the moment
     * @param k usage vector
     * @return int
     */
    private int maxNodeInUse(int[] k) {
        int index = 0;
        for (int i = 0; i < k.length; i++) {
            if (k[i] > 0)
                index = i;
        }
        return index;
    }

    /**
     * Computes smallest node in use by operator at the moment
     * @param k usage vector
     * @return int
     */
    private int minNodeInUse(int[] k) {
        int index;
        for (index = 0; index < k.length; index++) {
            if (k[index] > 0)
                return index;
        }
        return k.length - 1;
    }

    /**
     * Computes CPU speedup sum in use at the moment
     * @param k usage vector
     * @return double
     */
    private double computeCPUSpeedupInUse(int[] k) {
        double speedup = 0.0;
        for (int i = 0; i < k.length; i++) {
            speedup += ComputingInfrastructure.getInfrastructure().getNodeTypes()[i].getCpuSpeedup() * k[i];
        }
        return speedup;
    }

    /**
     * Compute max CPU speedup that can be used by operator multiplying max CPU's speedup by max operator's parallelism
     * @param o Operator
     * @return double
     */
    private double computeMaxCPUSpeedup(Operator o) {
        double max = 0.0;

        for (NodeType nodeType : ComputingInfrastructure.getInfrastructure().getNodeTypes()) {
            if (nodeType.getCpuSpeedup() > max)
                max = nodeType.getCpuSpeedup();
        }

        return max * o.getMaxParallelism();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GeneralResourcesState)) return false;
        if (!super.equals(o)) return false;
        GeneralResourcesState that = (GeneralResourcesState) o;
        return lambdaLevel == that.lambdaLevel &&
                Double.compare(that.normalizedCPUSpeedup, normalizedCPUSpeedup) == 0 &&
                Double.compare(that.maxCPUSpeedupInUse, maxCPUSpeedupInUse) == 0 &&
                Double.compare(that.minCPUSpeedupInUse, minCPUSpeedupInUse) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lambdaLevel, normalizedCPUSpeedup, maxCPUSpeedupInUse, minCPUSpeedupInUse);
    }
}
