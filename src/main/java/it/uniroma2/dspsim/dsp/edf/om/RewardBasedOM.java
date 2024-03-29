package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.QBasedReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedMetric;

public abstract class RewardBasedOM extends OperatorManager {

    protected Action lastChosenAction;
    protected State lastState;

    protected CountMetric trainingEpochsCount;
    protected RealValuedMetric planningTimeMetric;

    private int maxInputRate;
    private int inputRateLevels;

    private double wReconf;
    private double wSLO;
    private double wResources;

    private StateType stateRepresentation;

    private ActionSelectionPolicy actionSelectionPolicy;

    public RewardBasedOM(Operator operator) {
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

        // state representation
        this.stateRepresentation = StateType.fromString(
                configuration.getString(ConfigurationKeys.RL_OM_STATE_REPRESENTATION_KEY, "k_lambda"));

        // ASP
        this.actionSelectionPolicy = initActionSelectionPolicy();

        // register statistics
        try {
            this.registerMetrics(Statistics.getInstance());
        } catch (Statistics.MetricExistsException e ){
            e.printStackTrace();
        }
    }

    protected void registerMetrics(Statistics statistics) {
        this.trainingEpochsCount = new CountMetric("TrainingCount_" + this.operator.getName());
        statistics.registerMetricIfNotExists(this.trainingEpochsCount);

        this.planningTimeMetric = new RealValuedMetric("PlanningTime_" + this.operator.getName());
        statistics.registerMetricIfNotExists(this.planningTimeMetric);
    }

    protected String getOperatorMetricName(String metricName) {
        return metricName + "_" + operator.getName();
    }

    @Override
    public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
        // compute new state
        State currentState = StateUtils.computeCurrentState(monitoringInfo, operator, maxInputRate, inputRateLevels, stateRepresentation);

        // learning step
        if (lastChosenAction != null) {
            // compute reconfiguration's cost and use it as reward
            double reward = computeCost(lastChosenAction, currentState, monitoringInfo.getInputRate());

            useReward(reward, lastState, lastChosenAction, currentState, monitoringInfo);
        }

        // pick new action
        lastChosenAction = this.actionSelectionPolicy.selectAction(currentState);

        // update state
        lastState = currentState;

        return prepareOMRequest(currentState, lastChosenAction);
    }

    protected OMRequest prepareOMRequest(State currentState, Action chosenAction) {
        /*
         * Provide information to the AM.
         */
        double actionScore = 0.0;
        double noReconfigurationScore = 0.0;
        if (this instanceof ActionSelectionPolicyCallback) {
            /* V(pds) */
            actionScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, chosenAction);
            Action nop = ActionIterator.getDoNothingAction();
            if (nop.equals(chosenAction)) {
                noReconfigurationScore = actionScore;
            } else {
                noReconfigurationScore = ((ActionSelectionPolicyCallback) this).evaluateAction(currentState, nop);
            }
        }

        return new RewardBasedOMRequest(action2reconfiguration(chosenAction),
                new QBasedReconfigurationScore(actionScore), new QBasedReconfigurationScore(noReconfigurationScore));
    }

    protected double computeCost(Action action, State currentState, double inputRate) {
        double cost = 0.0;

        if (action.getDelta() != 0)
            cost += wReconf;

        /* we give the OperatorManager a per-operator SLO
           at the beginning (e.g., each operator gets at most 1/n of the application SLO,
           where n is the number of operators on a source-sink path.
        */
        if (operator.responseTime(inputRate) > operator.getSloRespTime())
            cost += wSLO;

        cost += operator.computeNormalizedDeploymentCost() * wResources;

        return cost;
    }

    static public Reconfiguration action2reconfiguration(Action action) {
        int delta = action.getDelta();

        if (delta > 1 || delta < -1) throw new RuntimeException("Unsupported action!");

        if (delta == 0) return Reconfiguration.doNothing();

        return delta > 0 ?
                Reconfiguration.scaleOut(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]) :
                Reconfiguration.scaleIn(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]);
    }

    public void forgetLastAction() {
        this.lastChosenAction = null;
    }

    /**
     * ABSTRACT METHODS
     */

    protected abstract ActionSelectionPolicy initActionSelectionPolicy();
    protected abstract void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo);

    /**
     * GETTERS
     */

    public ActionSelectionPolicy getActionSelectionPolicy() {
        return actionSelectionPolicy;
    }

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

    public StateType getStateRepresentation() {
        return stateRepresentation;
    }

    protected void dumpQ()
    {

    }
}
