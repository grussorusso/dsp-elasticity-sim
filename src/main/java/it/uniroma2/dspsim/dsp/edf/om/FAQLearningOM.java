package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.fa.FunctionApproximationManager;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.simple.ReconfigurationFeature;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingBuilder;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingType;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.RectangleTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.StripeTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.TilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.AvgMetric;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;
import it.uniroma2.dspsim.stats.samplers.StepSampler;

public class FAQLearningOM extends ReinforcementLearningOM {

    protected FunctionApproximationManager functionApproximationManager;

    protected double alpha;
    private double alphaDecay;
    private double alphaMinValue;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private double gamma;

    private ActionSelectionPolicy greedyActionSelection;

    private static final String STAT_BELLMAN_ERROR_AVG = "Q-Learning Bellman Error Avg";
    private static final String STAT_BELLMAN_ERROR_SUM = "Q-Learning Bellman Error Sum";

    public FAQLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.functionApproximationManager = initFunctionApproximationManager();

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
        final double oldQ  = functionApproximationManager.evaluateQ(oldState, action, this);
        final double newQ = (reward + this.gamma * functionApproximationManager
                        .evaluateQ(currentState, greedyActionSelection.selectAction(currentState), this));

        final double bellmanError = Math.abs(newQ - oldQ);

        updateWeights(newQ - oldQ, oldState, action);

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

    private FunctionApproximationManager initFunctionApproximationManager() {
        FunctionApproximationManager fa = new FunctionApproximationManager();

        // add scale out and scale in features
        fa.addFeature(new ReconfigurationFeature(1));
        fa.addFeature(new ReconfigurationFeature(-1));


        // compute max resource types in use in the same moment
        int resTypesNumber = Configuration.getInstance().getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3);

        // compute first tiling parameters
        double[] parallelismRange = new double[] {1, this.operator.getMaxParallelism() + 1};
        double[] lambdaLevelsRange = new double[] {0, this.getInputRateLevels()};
        double[] resourcesInUseRange = new double[] {1, Math.pow(2, resTypesNumber)};

        // compute first tiling tiles number
        int parallelismTiles = this.operator.getMaxParallelism() + 1;
        int lambdaLevelTiles = (int) Math.ceil(this.getInputRateLevels() / 2.0);
        int resourcesInUseTiles = (int) (Math.pow(2, resTypesNumber));

        // add tiling with rectangle shape
        fa.addFeature(
                buildTiling(TilingType.K_LAMBDA_RES_TYPE,
                        new RectangleTilingShape(parallelismTiles, lambdaLevelTiles, resourcesInUseTiles),
                        parallelismRange,
                        lambdaLevelsRange,
                        resourcesInUseRange
                )
        );

        // compute second tiling parameters
        double[] movedLambdaLevelsRange = new double[] { - ((double) this.getInputRateLevels() / (double) lambdaLevelTiles / 2.0),
                this.getInputRateLevels() + 1 };

        // add tiling with rectangle shape
        fa.addFeature(
                buildTiling(TilingType.K_LAMBDA_RES_TYPE,
                        new RectangleTilingShape(parallelismTiles, lambdaLevelTiles, resourcesInUseTiles),
                        parallelismRange,
                        movedLambdaLevelsRange,
                        resourcesInUseRange
                )
        );

        // compute third tiling parameters
        int stripes = this.operator.getMaxParallelism() + 1;
        double stripeSlope = 2.0;

        // add third tiling with stripes shape
        fa.addFeature(
                buildTiling(TilingType.K_LAMBDA_RES_TYPE,
                        new StripeTilingShape(stripes, stripeSlope, resourcesInUseTiles),
                        parallelismRange,
                        lambdaLevelsRange,
                        resourcesInUseRange
                )
        );

        return fa;
    }

    private Tiling buildTiling(TilingType type, TilingShape shape, double[] xRange, double[] yRange, double[] zRange) {
        return new TilingBuilder()
                .setShape(shape)
                .setXRange(xRange)
                .setYRange(yRange)
                .setZRange(zRange)
                .build(type);
    }

    private void updateWeights(double delta, State state, Action action) {
        if (!Double.isNaN(delta)) {
            for (Feature f : this.functionApproximationManager.getFeatures()) {
                double updatingValue = f.isActive(state, action, this) ? this.alpha * delta : 0.0;
                if (updatingValue != 0.0)
                    f.update(updatingValue, state, action, this);
            }
        }
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        return this.functionApproximationManager.evaluateQ(s, a, this);
    }
}
