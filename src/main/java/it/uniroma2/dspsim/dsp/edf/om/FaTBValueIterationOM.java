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
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.logging.Logger;

public class FaTBValueIterationOM extends BaseTBValueIterationOM {

    protected FunctionApproximationManager functionApproximationManager;

    protected double alpha;

    public FaTBValueIterationOM(Operator operator) {
        super(operator);

        Configuration conf = Configuration.getInstance();
        this.alpha = conf.getDouble(ConfigurationKeys.TBVI_FA_ALPHA_KEY, 0.1);

        final boolean useApproximateModel = conf.getBoolean(ConfigurationKeys.VI_APPROX_MODEL, false);
        Operator realOperator = this.operator;
        if (useApproximateModel) {
            this.operator = approximateOperatorModel(conf);
        }

        if (!PolicyIOUtils.shouldLoadPolicy(conf)) {
            tbvi(this.tbviMaxIterations, this.tbviMillis,this.tbviTrajectoryLength);
        }

        this.operator = realOperator;
    }

    private Operator approximateOperatorModel (Configuration conf)
    {
        Random r = new Random(conf.getInteger(ConfigurationKeys.VI_APPROX_MODEL_SEED, 123));
        final double maxErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MAX_ERR, 0.1);
        final double minErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MIN_ERR, 0.05);

        OperatorQueueModel queueModel = operator.getQueueModel().getApproximateModel(r, maxErr, minErr);
        LoggerFactory.getLogger(FaTBValueIterationOM.class).info("Approximate stMean: {} -> {}", operator.getQueueModel().getServiceTimeMean(), queueModel.getServiceTimeMean());
        Operator tempOperator = new Operator("temp", queueModel, operator.getMaxParallelism());
        tempOperator.setSloRespTime(operator.getSloRespTime());
        return tempOperator;
    }

    @Override
    protected void resetTrajectoryData() {
        // do nothing
    }

    @Override
    protected double computeQ(State s, Action a) {
        return this.functionApproximationManager.evaluateQ(s, a, this);
    }

    @Override
    protected void learn(double tbviDelta, double reward, State state, Action action) {
        if (!Double.isNaN(tbviDelta)) {
            for (Feature f : this.functionApproximationManager.getFeatures()) {
                double updatingValue = f.isActive(state, action, this) ? this.alpha * tbviDelta : 0.0;
                if (updatingValue != 0.0)
                    f.update(updatingValue, state, action, this);
            }
        }
    }

    @Override
    protected void buildQ() {
        this.functionApproximationManager = new FunctionApproximationManager();

        // add scale out and scale in features
        this.functionApproximationManager.addFeature(new ReconfigurationFeature(1));
        this.functionApproximationManager.addFeature(new ReconfigurationFeature(-1));


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
        this.functionApproximationManager.addFeature(
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
        this.functionApproximationManager.addFeature(
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
        this.functionApproximationManager.addFeature(
                buildTiling(TilingType.K_LAMBDA_RES_TYPE,
                        new StripeTilingShape(stripes, stripeSlope, resourcesInUseTiles),
                        parallelismRange,
                        lambdaLevelsRange,
                        resourcesInUseRange
                )
        );

        if (PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
            this.functionApproximationManager.load(PolicyIOUtils.getFileForLoading(this.operator, "fa"));
        }
    }

    @Override
    protected void dumpQOnFile(String filename) {
        // TODO
    }

    @Override
    public void savePolicy()
    {
        this.functionApproximationManager.dump(PolicyIOUtils.getFileForDumping(this.operator, "fa"));
    }


    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return computeQ(s, a);
    }


    private Tiling buildTiling(TilingType type, TilingShape shape, double[] xRange, double[] yRange, double[] zRange) {
        return new TilingBuilder()
                .setShape(shape)
                .setXRange(xRange)
                .setYRange(yRange)
                .setZRange(zRange)
                .build(type);
    }
}
