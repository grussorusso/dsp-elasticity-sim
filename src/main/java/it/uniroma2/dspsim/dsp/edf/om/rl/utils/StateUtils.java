package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.MathUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StateUtils {

    // avoid init
    private StateUtils() { }

    public static State computePostDecisionState(State state, Action action, RewardBasedOM om) {
        if (action.getDelta() != 0) {
            int[] pdk = Arrays.copyOf(state.getActualDeployment(), state.getActualDeployment().length);
            int aIndex = action.getResTypeIndex();
            pdk[aIndex] = pdk[aIndex] + action.getDelta();
            return StateFactory.createState(om.getStateRepresentation(), -1, pdk,
                    state.getLambda(), om.getInputRateLevels() - 1, om.getOperator().getMaxParallelism());
        } else {
            return StateFactory.createState(om.getStateRepresentation(), -1, state.getActualDeployment(),
                    state.getLambda(), om.getInputRateLevels() - 1, om.getOperator().getMaxParallelism());
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
        double maxCost = om.getOperator().getMaxParallelism() * ComputingInfrastructure.getInfrastructure().getMostExpensiveResType().getCost();
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

    public static double computeSLOCost(State state, RewardBasedOM om) {
        double cost = 0.0;

        List<NodeType> operatorInstances = getOperatorInstances(state);
        double inputRate = MathUtils.remapDiscretizedValue(om.getMaxInputRate(), state.getLambda(), om.getInputRateLevels());

        if (om.getOperator().responseTime(inputRate, operatorInstances) > om.getOperator().getSloRespTime())
            cost += 1.0;

        return cost;
    }
}
