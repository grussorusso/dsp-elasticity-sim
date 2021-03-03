package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class HybridRewardBasedOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    private BaseTBValueIterationOM offLineOM;
    private ReinforcementLearningOM onLineOM;

    public HybridRewardBasedOM(Operator operator) {
        super(operator);

        this.offLineOM = buildOffLineOM(approximateOperatorModel(Configuration.getInstance()));

        // prevent from loading from file
        Configuration.getInstance().setString(ConfigurationKeys.OM_POLICY_LOAD_DIR, "");
        this.onLineOM = buildOnLineOM(operator);

        transferKnowledge();

        // recall init action selection policy
        // to overwrite initial action selection policy with deep-v-learning configured one
        initActionSelectionPolicy();
    }

    private Operator approximateOperatorModel (Configuration conf)
    {
        Random r = new Random(conf.getInteger(ConfigurationKeys.VI_APPROX_MODEL_SEED, 123));
        final double maxErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MAX_ERR, 0.1);
        final double minErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MIN_ERR, 0.05);

        OperatorQueueModel queueModel = operator.getQueueModel().getApproximateModel(r, maxErr, minErr);
        LoggerFactory.getLogger(HybridRewardBasedOM.class).info("Approximate stMean: {} -> {}", operator.getQueueModel().getServiceTimeMean(), queueModel.getServiceTimeMean());
        Operator tempOperator = new Operator("temp", queueModel, operator.getMaxParallelism());
        tempOperator.setSloRespTime(operator.getSloRespTime());
        return tempOperator;
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        if (this.onLineOM == null) {
            return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        } else {
            return this.onLineOM.getActionSelectionPolicy();
        }
    }

    @Override
    protected void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {
        // delegate reward collection to deep-v-learning om
        this.onLineOM.useReward(reward, lastState, lastChosenAction, currentState, monitoringInfo);
    }

    @Override
    public boolean validateAction(State s, Action a) {
        if (this.onLineOM == null) {
            return s.validateAction(a);
        } else {
            return this.onLineOM.validateAction(s, a);
        }
    }

    @Override
    public double evaluateAction(State s, Action a) {
        if (this.onLineOM == null) {
            if (this.offLineOM == null ) {
                return 0.0;
            } else {
                return this.offLineOM.evaluateAction(s, a);
            }
        } else {
            return this.onLineOM.evaluateAction(s, a);
        }
    }

    /**
     * ABSTRACT METHODS
     */

    protected abstract BaseTBValueIterationOM buildOffLineOM(Operator operator);
    protected abstract ReinforcementLearningOM buildOnLineOM(Operator operator);

    /**
     * Implement this method to transfer knowledge from off-line om to on-line om
     */
    protected abstract void transferKnowledge();

    /**
     * GETTERS
     */
    public BaseTBValueIterationOM getOffLineOM() {
        return offLineOM;
    }

    public ReinforcementLearningOM getOnLineOM() {
        return onLineOM;
    }
}
