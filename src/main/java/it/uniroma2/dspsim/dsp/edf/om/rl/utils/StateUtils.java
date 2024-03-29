package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.MathUtils;
import org.nd4j.linalg.api.ops.Op;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StateUtils {

    // avoid init
    private StateUtils() { }

    public static State computePostDecisionState(State state, Action action, RewardBasedOM om) {
        return computePostDecisionState(state, action, om.getStateRepresentation(), om.getInputRateLevels(), om.getOperator().getMaxParallelism());
    }

    public static State computePostDecisionState(State state, Action action, StateType stateType, int inputRateLevels,
                                                 int maxParallelism) {
        if (action.getDelta() != 0) {
            int[] pdk = Arrays.copyOf(state.getActualDeployment(), state.getActualDeployment().length);
            int aIndex = action.getResTypeIndex();
            pdk[aIndex] = pdk[aIndex] + action.getDelta();
            return StateFactory.createState(stateType, -1, pdk,
                    state.getLambda(), inputRateLevels - 1, maxParallelism);
        } else {
            return StateFactory.createState(stateType, -1, state.getActualDeployment(),
                    state.getLambda(), inputRateLevels - 1, maxParallelism);
        }
    }

    private static double computeDeploymentCost(State state) {
        List<NodeType> operatorInstances = getOperatorInstances(state);
        double deploymentCost = 0.0;
        for (NodeType nt : operatorInstances)
            deploymentCost += nt.getCost();

        return deploymentCost;
    }

    public static double computeDeploymentCostNormalized(State state, RewardBasedOM om) {
        return computeDeploymentCostNormalized(state, om.getOperator().getMaxParallelism());
    }

    public static double computeDeploymentCostNormalized(State state, int maxParallelism) {
        double maxCost = maxParallelism * ComputingInfrastructure.getInfrastructure().getCostNormalizationConstant();
        return computeDeploymentCost(state) / maxCost;
    }

    public static List<NodeType> getOperatorInstances(State state) {
        int[] deployment = state.getActualDeployment();

        List<NodeType> operatorInstances = new ArrayList<>();

        for (int i = 0; i < deployment.length; i++) {
            for (int j = 0; j < deployment[i]; j++) {
                operatorInstances.add(ComputingInfrastructure.getInfrastructure().getNodeTypes()[i]);
            }
        }

        return operatorInstances;
    }

    public static double computeSLOCost(State state, RewardBasedOM om, boolean approximateNodeSpeedups,
                                        boolean approximateOperatorModel) {
        double cost = 0.0;

        List<NodeType> operatorInstances = getOperatorInstances(state);
        if (approximateNodeSpeedups) {
            ComputingInfrastructure infra = ComputingInfrastructure.getInfrastructure();
            NodeType[] approxNodeTypes = infra.getEstimatedNodeTypes();

            List<NodeType> approximateInstances = new ArrayList<>(operatorInstances.size());
            for (NodeType nt : operatorInstances) {
                approximateInstances.add(approxNodeTypes[nt.getIndex()]);
            }
            operatorInstances = approximateInstances;
        }

        double inputRate = MathUtils.remapDiscretizedValue(om.getMaxInputRate(), state.getLambda(), om.getInputRateLevels());

        Operator op = approximateOperatorModel? om.getApproximateOperator() : om.getOperator();

        final double respTime = op.responseTime(inputRate, operatorInstances);
        if (respTime > om.getOperator().getSloRespTime())
            cost += 1.0;

        return cost;
    }

    public static double computeSLOCost (State state, RewardBasedOM om) {
        return computeSLOCost(state, om, false, false);
    }

    public static double computeRespTime (State state, RewardBasedOM om) {
        return computeRespTime(state, om.getOperator(), om.getMaxInputRate(), om.getInputRateLevels());
    }

    public static double computeRespTime (State state, Operator op, int maxInputRate, int inputRateLevels) {
        List<NodeType> operatorInstances = getOperatorInstances(state);
        double inputRate = MathUtils.remapDiscretizedValue(maxInputRate, state.getLambda(), inputRateLevels);
        return op.responseTime(inputRate, operatorInstances);
    }

    static public State computeCurrentState(OMMonitoringInfo monitoringInfo, Operator op, int maxInputRate, int inputRateLevels, StateType stateType) {
        // read actual deployment
        final int[] deployment = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
        for (NodeType nt : op.getInstances()) {
            deployment[nt.getIndex()] += 1;
        }
        // get input rate level
        final int inputRateLevel = MathUtils.discretizeValue(maxInputRate, monitoringInfo.getInputRate(), inputRateLevels);
        // build new state
        return StateFactory.createState(stateType, -1, deployment, inputRateLevel,
                inputRateLevels - 1, op.getMaxParallelism());
    }

}
