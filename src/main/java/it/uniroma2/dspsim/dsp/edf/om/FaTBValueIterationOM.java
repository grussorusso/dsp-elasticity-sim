package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.fa.FunctionApproximationManager;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.simple.ReconfigurationFeature;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingBuilder;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.TilingType;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.RectangleTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.StripeTilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.MathUtils;

public class FaTBValueIterationOM extends BaseTBValueIterationOM {

    private FunctionApproximationManager policy;

    private double alpha;

    public FaTBValueIterationOM(Operator operator) {
        super(operator);

        // TODO configure
        this.alpha = 0.5;

        // TODO configure
        tbvi(60000, 512);
    }

    @Override
    protected void resetTrajectoryData() {
        // do nothing
    }

    @Override
    protected double computeQ(State s, Action a) {
        return this.policy.evaluateQ(s, a, this);
    }

    @Override
    protected void learn(double tbviDelta, double reward, State state, Action action) {
        if (tbviDelta > 1E-10) {
            for (Feature f : this.policy.getFeatures()) {
                double updatingValue = f.isActive(state, action, this) ? this.alpha * tbviDelta : 0.0;
                if (updatingValue != 0.0)
                    f.update(updatingValue, state, action, this);
            }
        }
    }

    @Override
    protected void buildPolicy() {
        this.policy = new FunctionApproximationManager();

        // add scale out and scale in features
        this.policy.addFeature(new ReconfigurationFeature(1));
        this.policy.addFeature(new ReconfigurationFeature(-1));


        // compute max resource types in use in the same moment
        int resTypesNumber = Configuration.getInstance().getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3);

        // compute first tiling parameters
        double[] parallelismRange = new double[] {1, this.operator.getMaxParallelism() + 1};
        double[] lambdaLevelsRange = new double[] {0, this.getInputRateLevels()};
        double[] resourcesInUseRange = new double[] {1, Math.pow(2, resTypesNumber)};

        // compute first tiling tiles number
        int parallelismTiles = this.operator.getMaxParallelism() / 2;
        int lambdaLevelTiles = (int) Math.ceil(this.getInputRateLevels() / 5.0);
        int resourcesInUseTiles = (int) (Math.pow(2, resTypesNumber) / 2.0);

        // add tiling with rectangle shape
        this.policy.addFeature(new TilingBuilder()
                .setShape(new RectangleTilingShape(parallelismTiles, lambdaLevelTiles, resourcesInUseTiles))
                .setXRange(parallelismRange)
                .setYRange(lambdaLevelsRange)
                .setZRange(resourcesInUseRange)
                .build(TilingType.K_LAMBDA_RES_TYPE)
        );

        // compute second tiling parameters
        double[] movedLambdaLevelsRange = new double[] { - ((double) this.getInputRateLevels() / (double) lambdaLevelTiles / 2.0),
                this.getInputRateLevels() + 1 };

        // add tiling with rectangle shape
        this.policy.addFeature(new TilingBuilder()
                .setShape(new RectangleTilingShape(parallelismTiles, lambdaLevelTiles, resourcesInUseTiles))
                .setXRange(parallelismRange)
                .setYRange(movedLambdaLevelsRange)
                .setZRange(resourcesInUseRange)
                .build(TilingType.K_LAMBDA_RES_TYPE)
        );

        // compute third tiling parameters
        int stripes = this.operator.getMaxParallelism();
        double stripeSlope = 2.0;

        // add third tiling with stripes shape
        this.policy.addFeature(new TilingBuilder()
                .setShape(new StripeTilingShape(stripes, stripeSlope, resourcesInUseTiles))
                .setXRange(parallelismRange)
                .setYRange(lambdaLevelsRange)
                .setZRange(resourcesInUseRange)
                .build(TilingType.K_LAMBDA_RES_TYPE)
        );
    }

    @Override
    protected void dumpPolicyOnFile(String filename) {
        // TODO
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return computeQ(s, a);
    }
}
