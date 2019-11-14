package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.util.HashMap;

public abstract class ReinforcementLearningOM extends OperatorManager implements ActionSelectionPolicyCallback {

    private Action lastChosenAction;
    private State lastState;

    private int maxInputRate;
    private int inputRateLevels;

    private double wReconf;
    private double wSLO;
    private double wResources;

    private ActionSelectionPolicy actionSelectionPolicy;

    protected ReinforcementLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // input rate discretization
        this.maxInputRate = configuration.getInteger(ConfigurationKeys.RL_OM_MAX_INPUT_RATE_KEY, 600);
        this.inputRateLevels = configuration.getInteger(ConfigurationKeys.RL_OM_INPUT_RATE_LEVELS_KEY, 20);

        // reward weights
        this.wReconf = configuration.getDouble(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY, 0.33);
        this.wSLO = configuration.getDouble(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY, 0.33);
        this.wResources = configuration.getDouble(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY, 0.33);

        // action selection policy
        ActionSelectionPolicyType aspType = ActionSelectionPolicyType.fromString(
                configuration.getString(ConfigurationKeys.ASP_TYPE_KEY, "e-greedy"));

        this.actionSelectionPolicy = ActionSelectionPolicyFactory
                .getPolicy(aspType,this);
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
        lastChosenAction = selectNewAction(currentState);

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

    protected State computeNewState(OMMonitoringInfo monitoringInfo) {
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

    private Action selectNewAction(State s) {
        return this.actionSelectionPolicy.selectAction(s);
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public boolean actionValidation(State s, Action a) {
        return a.isValidInState(s, this.operator);
    }

    /**
     * ABSTRACT METHODS
     */
    protected abstract void learningStep(State oldState, Action action, State currentState, double reward);

    /**
     * GETTER
     */

    public Action getLastChosenAction() {
        return lastChosenAction;
    }

    public State getLastState() {
        return lastState;
    }

    public int getMaxInputRate() {
        return maxInputRate;
    }

    public int getInputRateLevels() {
        return inputRateLevels;
    }

    public double getwReconf() {
        return wReconf;
    }

    public double getwSLO() {
        return wSLO;
    }

    public double getwResources() {
        return wResources;
    }

    public ActionSelectionPolicy getActionSelectionPolicy() {
        return actionSelectionPolicy;
    }
}
