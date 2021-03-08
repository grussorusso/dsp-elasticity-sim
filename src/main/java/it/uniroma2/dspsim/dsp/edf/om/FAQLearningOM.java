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
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;

public class FAQLearningOM extends ReinforcementLearningOM {

    protected FunctionApproximationManager functionApproximationManager;

    protected VariableParameter alpha;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private double gamma;

    private ActionSelectionPolicy greedyActionSelection;

    public FAQLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.functionApproximationManager = initFunctionApproximationManager();
        if (PolicyIOUtils.shouldLoadPolicy(configuration)) {
            this.functionApproximationManager.load(PolicyIOUtils.getFileForLoading(this.operator, "fa"));
        }

        double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 1.0);
        double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.98);
        double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

        this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);

        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY, 0.99);

        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );
    }

    @Override
    public void savePolicy()
    {
        this.functionApproximationManager.dump(PolicyIOUtils.getFileForDumping(this.operator, "fa"));
    }


    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final double oldQ  = functionApproximationManager.evaluateQ(oldState, action, this);
        final double newQ = (reward + this.gamma * functionApproximationManager
                        .evaluateQ(currentState, greedyActionSelection.selectAction(currentState), this));

        updateWeights(newQ - oldQ, oldState, action);

        decrementAlpha();
    }

    private void decrementAlpha() {
        if (this.alphaDecaySteps > 0) {
            this.alphaDecayStepsCounter++;
            if (this.alphaDecayStepsCounter >= this.alphaDecaySteps) {
                this.alpha.update();
                this.alphaDecayStepsCounter = 0;
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
                double updatingValue = f.isActive(state, action, this) ? this.alpha.getValue() * delta : 0.0;
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
