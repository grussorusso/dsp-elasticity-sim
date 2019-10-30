package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

public abstract class ReinforcementLearningOM extends OperatorManager {
    protected Action lastChosenAction;
    protected State lastState;

    private int maxInputRate;
    private int inputRateLevels;

    private double wReconf;
    private double wSLO;
    private double wResources;

    public ReinforcementLearningOM(Operator operator) {
        super(operator);

        // TODO define in configuration
        this.maxInputRate = 600;
        this.inputRateLevels = 20;
        this.wReconf = 0.33;
        this.wSLO = 0.33;
        this.wResources = 0.33;
    }

    @Override
    public Reconfiguration pickReconfiguration(OMMonitoringInfo monitoringInfo) {
        // compute new state
        State currentState = computeNewState(monitoringInfo);

        // learning step
        if (lastChosenAction != null) {
            // compute reconfiguration's cost and use it as reward
            double reward = computeCost(lastChosenAction, currentState, monitoringInfo.getInputRate());
            // learning step
            learningStep(lastState, lastChosenAction, currentState, reward);
        }

        // pick new action
        lastChosenAction = pickNewAction(currentState);

        // update state
        lastState = currentState;

        // construct reconfiguration from action
        return action2reconfiguration(lastChosenAction);
    }

    private double computeCost(Action action, State currentState, double inputRate) {
        double cost = 0.0;

        if (action.getDelta() != 0)
            cost += wReconf;

        /* TODO Given the application latency SLO,
           we should give the OperatorManager a per-operator SLO
           at the beginning (e.g., each operator gets at most 1/n of the application SLO,
           where n is the number of operators on a source-sink path.
        */
        final double OPERATOR_SLO = 0.1;

        if (operator.responseTime(inputRate) > OPERATOR_SLO)
            cost += wSLO;

        cost += operator.computeNormalizedDeploymentCost() * wResources;

        return cost;
    }

    private State computeNewState(OMMonitoringInfo monitoringInfo) {
        // read actual deployment
        final int[] deployment = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
        for (NodeType nt : operator.getInstances()) {
            deployment[nt.getIndex()] += 1;
        }
        // get input rate level
        final int inputRateLevel = discretizeInputRate(maxInputRate, inputRateLevels, monitoringInfo.getInputRate());
        // build new state
        return new State(deployment, inputRateLevel);
    }

    private int discretizeInputRate(double max, int levels, double inputRate)
    {
        final double quantum = max / levels;
        final int level = (int)Math.floor(inputRate/quantum);
        return level < levels? level : levels-1;
    }

    private Reconfiguration action2reconfiguration(Action action) {
        int delta = action.getDelta();

        if (delta > 1 || delta < -1) throw new RuntimeException("Unsupported action!");

        if (delta == 0) return Reconfiguration.doNothing();

        return delta > 0 ?
                Reconfiguration.scaleOut(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]) :
                Reconfiguration.scaleIn(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]);
    }

    protected abstract void learningStep(State oldState, Action action, State currentState, double reward);

    protected abstract Action pickNewAction(State state);
}
