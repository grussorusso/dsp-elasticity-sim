package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

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
            return state;
        }
    }

    public static double computePostDecisionCost(int[] deployment, double inputRate, RewardBasedOM om) {
        double cost = 0.0;

        double currentSpeedup = Double.POSITIVE_INFINITY;
        List<NodeType> usedNodeTypes = new ArrayList<>();
        List<NodeType> operatorInstances = new ArrayList<>();
        for (int i = 0; i < deployment.length; i++) {
            if (deployment[i] > 0)
                usedNodeTypes.add(ComputingInfrastructure.getInfrastructure().getNodeTypes()[i]);
            for (int j = 0; j < deployment[i]; j++) {
                operatorInstances.add(ComputingInfrastructure.getInfrastructure().getNodeTypes()[i]);
            }
        }

        for (NodeType nt : usedNodeTypes)
            currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

        if (om.getOperator().getQueueModel().responseTime(inputRate, operatorInstances.size(), currentSpeedup) > om.getOperator().getSloRespTime())
            cost += om.getwSLO();

        double deploymentCost = 0.0;
        for (NodeType nt : operatorInstances)
            deploymentCost += nt.getCost();

        double maxCost = om.getOperator().getMaxParallelism() * ComputingInfrastructure.getInfrastructure().getMostExpensiveResType().getCost();

        cost += (deploymentCost / maxCost) * om.getwResources();

        return cost;
    }
}
