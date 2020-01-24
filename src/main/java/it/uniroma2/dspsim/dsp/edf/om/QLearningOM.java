package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.AvgMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.samplers.StepSampler;

public class QLearningOM extends ReinforcementLearningOM {
    private QTable qTable;

    private double alpha;
    private double alphaDecay;
    private double alphaMinValue;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private double gamma;

    private ActionSelectionPolicy greedyActionSelection;

    private static final String STAT_BELLMAN_ERROR_AVG = "Q-Learning Bellman Error Avg";
    private static final String STAT_BELLMAN_ERROR_SUM = "Q-Learning Bellman Error Sum";



    public QLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.qTable = new GuavaBasedQTable(0.0);

        this.alpha = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 1.0);
        this.alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.98);
        this.alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);
        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.gamma = 0.99;

        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);

        // PER OPERATOR METRICS
        // total bellman error
        statistics.registerMetric(new RealValuedCountMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM)));
        // bellman error avg
        AvgMetric bellmanErrorAvgMetric = new AvgMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_AVG),
                statistics.getMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM)),
                (CountMetric) statistics.getMetric(getOperatorMetricName(STAT_GET_REWARD_COUNTER)));
        statistics.registerMetric(bellmanErrorAvgMetric);
        // add step sampling to bellman error avg metric
        StepSampler stepSampler = new StepSampler(STEP_SAMPLER_ID, 1);
        bellmanErrorAvgMetric.addSampler(stepSampler);
        statistics.registerMetricIfNotExists(bellmanErrorAvgMetric);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha) * oldQ +
                alpha * (reward + this.gamma * qTable.getQ(currentState, greedyActionSelection.selectAction(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);

        qTable.setQ(oldState, action, newQ);

        decrementAlpha();

        // update bellman error metrics
        // per operator
        Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM), bellmanError);
    }

    private void decrementAlpha() {
        if (this.alphaDecaySteps > 0) {
            this.alphaDecayStepsCounter++;
            if (this.alphaDecayStepsCounter >= this.alphaDecaySteps) {
                if (this.alpha >= this.alphaMinValue) {
                    this.alphaDecayStepsCounter = 0;
                    this.alpha = this.alphaDecay * this.alpha;
                    if (this.alpha < this.alphaMinValue) {
                        this.alpha = this.alphaMinValue;
                    }
                }
            }
        }
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        return qTable.getQ(s, a);
    }
}
