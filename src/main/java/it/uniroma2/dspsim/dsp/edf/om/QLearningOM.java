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
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private ActionSelectionPolicy greedyActionSelection;

    private static final String STAT_BELLMAN_ERROR_AVG = "Bellman Error Avg";
    private static final String STAT_BELLMAN_ERROR_SUM = "Bellman Error Sum";

    public QLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.qTable = new GuavaBasedQTable(0.0);

        this.alpha = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.2);
        this.alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.9);
        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

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
        statistics.registerMetric(new AvgMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_AVG),
                statistics.getMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM)),
                (CountMetric) statistics.getMetric(getOperatorMetricName(STAT_GET_REWARD_COUNTER))));

        // GLOBAL METRICS
        // total bellman error
        statistics.registerMetricIfNotExists(new RealValuedCountMetric(STAT_BELLMAN_ERROR_SUM));
        // bellman error avg
        AvgMetric bellmanErrorAvgMetric = new AvgMetric(STAT_BELLMAN_ERROR_AVG,
                statistics.getMetric(STAT_BELLMAN_ERROR_SUM),
                (CountMetric) statistics.getMetric(STAT_GET_REWARD_COUNTER));
        // add step sampling to bellman error avg metric
        StepSampler stepSampler = new StepSampler(STEP_SAMPLER_ID, 1);
        bellmanErrorAvgMetric.addSampler(stepSampler);
        statistics.registerMetricIfNotExists(bellmanErrorAvgMetric);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha) * oldQ +
                alpha * (reward + qTable.getQ(currentState, greedyActionSelection.selectAction(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);

        qTable.setQ(oldState, action, newQ);

        decrementAlpha();

        // update bellman error metrics
        // per operator
        Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM), bellmanError);
        // global
        Statistics.getInstance().updateMetric(STAT_BELLMAN_ERROR_SUM, bellmanError);
    }

    private void decrementAlpha() {
        if (this.alphaDecaySteps > 0) {
            this.alphaDecayStepsCounter++;
            if (this.alphaDecayStepsCounter >= this.alphaDecaySteps) {
                this.alphaDecayStepsCounter = 0;
                this.alpha = this.alphaDecay * this.alpha;
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
