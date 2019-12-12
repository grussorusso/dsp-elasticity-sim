package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.IncrementalAvgMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;
import it.uniroma2.dspsim.stats.samplers.StepSampler;
import it.uniroma2.dspsim.utils.MathUtils;

public abstract class RewardBasedOM extends OperatorManager {

    protected Action lastChosenAction;
    protected State lastState;

    private int maxInputRate;
    private int inputRateLevels;

    private double wReconf;
    private double wSLO;
    private double wResources;

    private StateType stateRepresentation;

    private ActionSelectionPolicy actionSelectionPolicy;

    protected static final String STAT_REWARD_INCREMENTAL_MEAN = "Incremental Mean Reward";
    protected static final String STAT_REWARD_SUM = "Reward Sum";
    protected static final String STAT_GET_REWARD_COUNTER = "Get Reward Counter";

    protected static final String STEP_SAMPLER_ID = "step-sampler";

    private long counter;

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
        this.registerMetrics(Statistics.getInstance());
    }

    protected void registerMetrics(Statistics statistics) {
        // PER OPERATOR METRICS
        // learning step counter
        statistics.registerMetric(new CountMetric(getOperatorMetricName(STAT_GET_REWARD_COUNTER)));
        // total reward
        statistics.registerMetric(new RealValuedCountMetric(getOperatorMetricName(STAT_REWARD_SUM)));
        // incremental mean reward
        statistics.registerMetric(new IncrementalAvgMetric(getOperatorMetricName(STAT_REWARD_INCREMENTAL_MEAN)));

        //GLOBAL METRICS
        // learning step counter
        statistics.registerMetricIfNotExists(new CountMetric(STAT_GET_REWARD_COUNTER));
        // total reward
        statistics.registerMetricIfNotExists(new RealValuedCountMetric(STAT_REWARD_SUM));
        // incremental mean reward
        IncrementalAvgMetric incrementalAvgMetric = new IncrementalAvgMetric(STAT_REWARD_INCREMENTAL_MEAN);
        StepSampler stepSampler = new StepSampler(STEP_SAMPLER_ID, 1);
        incrementalAvgMetric.addSampler(stepSampler);
        statistics.registerMetricIfNotExists(incrementalAvgMetric);
    }

    protected String getOperatorMetricName(String metricName) {
        return metricName + "_" + operator.getName();
    }

    @Override
    public Reconfiguration pickReconfiguration(OMMonitoringInfo monitoringInfo) {
        // compute new state
        State currentState = computeNewState(monitoringInfo);

        // learning step
        if (lastChosenAction != null) {
            // compute reconfiguration's cost and use it as reward
            double reward = computeCost(lastChosenAction, currentState, monitoringInfo.getInputRate());

            // update mean reward statistic
            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_GET_REWARD_COUNTER), 1);
            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_REWARD_SUM), reward);
            Statistics.getInstance().updateMetric(STAT_GET_REWARD_COUNTER, 1);
            Statistics.getInstance().updateMetric(STAT_REWARD_SUM, reward);
            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_REWARD_INCREMENTAL_MEAN), reward);
            Statistics.getInstance().updateMetric(STAT_REWARD_INCREMENTAL_MEAN, reward);
        }

        // pick new action
        lastChosenAction = this.actionSelectionPolicy.selectAction(currentState);

        // update state
        lastState = currentState;

        // construct reconfiguration from action
        return action2reconfiguration(lastChosenAction);
    }

    protected State computeNewState(OMMonitoringInfo monitoringInfo) {
        // read actual deployment
        final int[] deployment = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
        for (NodeType nt : operator.getInstances()) {
            deployment[nt.getIndex()] += 1;
        }
        // get input rate level
        final int inputRateLevel = MathUtils.discretizeValue(maxInputRate, monitoringInfo.getInputRate(), inputRateLevels);
        // build new state
        return StateFactory.createState(stateRepresentation, -1, deployment, inputRateLevel,
                this.inputRateLevels - 1, this.operator.getMaxParallelism());
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

    protected Reconfiguration action2reconfiguration(Action action) {
        int delta = action.getDelta();

        if (delta > 1 || delta < -1) throw new RuntimeException("Unsupported action!");

        if (delta == 0) return Reconfiguration.doNothing();

        return delta > 0 ?
                Reconfiguration.scaleOut(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]) :
                Reconfiguration.scaleIn(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]);
    }

    /**
     * ABSTRACT METHODS
     */

    protected abstract ActionSelectionPolicy initActionSelectionPolicy();

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
}
