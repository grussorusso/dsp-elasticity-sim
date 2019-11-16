package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.MeanMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;
import it.uniroma2.dspsim.stats.Statistics;

public class RLQLearningOM extends ReinforcementLearningOM {
    private QTable qTable;

    private double alpha;
    private double alphaDecay;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private ActionSelectionPolicy greedyActionSelection;

    private static final String STAT_BELLMAN_ERROR_MEAN = "Mean Bellman Error";
    private static final String STAT_BELLMAN_ERROR_SUM = "Bellman Error Sum";

    public RLQLearningOM(Operator operator) {
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
        // total reward
        statistics.registerMetric(new RealValuedCountMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM)));
        // mean reward
        statistics.registerMetric(new MeanMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_MEAN),
                statistics.getMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM)),
                (CountMetric) statistics.getMetric(getOperatorMetricName(STAT_LEARNING_STEP_COUNTER))));
        // GLOBAL METRICS
        // total reward
        statistics.registerMetricIfNotExists(new RealValuedCountMetric(STAT_BELLMAN_ERROR_SUM));
        // mean reward
        statistics.registerMetricIfNotExists(new MeanMetric(STAT_BELLMAN_ERROR_MEAN, true, 60,
                statistics.getMetric(STAT_BELLMAN_ERROR_SUM),
                (CountMetric) statistics.getMetric(STAT_LEARNING_STEP_COUNTER)));
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = qTable.getQ(oldState, action);
        final double newQ = (1.0 - alpha) * oldQ +
                alpha * (reward + qTable.getQ(currentState, greedyActionSelection.selectAction(currentState)));

        final double bellmanError = Math.abs(newQ - oldQ);

        // update bellman error metrics
        // per operator
        Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_BELLMAN_ERROR_SUM), bellmanError);
        // global
        Statistics.getInstance().updateMetric(STAT_BELLMAN_ERROR_SUM, bellmanError);

        qTable.setQ(oldState, action, newQ);

        decrementAlpha();
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
    public double evaluateQ(State s, Action a) {
        return qTable.getQ(s, a);
    }
}
