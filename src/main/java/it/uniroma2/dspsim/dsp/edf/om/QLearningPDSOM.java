package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QLearningPDSOM extends ReinforcementLearningOM {
    private QTable qTable;

    private VariableParameter alpha;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private double gamma;

    private Logger logger = LoggerFactory.getLogger(QLearningPDSOM.class);

    private ActionSelectionPolicy greedyActionSelection;

    public QLearningPDSOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.qTable = new GuavaBasedQTable(0.0);
        if (PolicyIOUtils.shouldLoadPolicy(configuration)) {
            this.qTable.load(PolicyIOUtils.getFileForLoading(this.operator, "qTable"));
        }

        double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.1);
        double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 1.0);
        double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

        this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);

        logger.info("Alpha conf: init={}, decay={}, currValue={}", alphaInitValue,
                alphaDecay, alpha.getValue());


        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY,0.99);

        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        Action nullA = new Action(0,0,0);
        final State pds = StateUtils.computePostDecisionState(oldState, action, this);

        double unknownCost = reward - getwResources()*StateUtils.computeDeploymentCostNormalized(pds,this);
        if (action.getDelta()!=0)
            unknownCost -= getwReconf();

        final double oldV  = qTable.getQ(oldState, nullA);
        final double newV = (1.0 - alpha.getValue()) * oldV + alpha.getValue() * (unknownCost +
                        this.gamma * evaluateAction(currentState, greedyActionSelection.selectAction(currentState)));

        qTable.setQ(pds, nullA, newV);

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

    @Override
    public void savePolicy()
    {
        this.qTable.dump(PolicyIOUtils.getFileForDumping(this.operator, "qTable"));
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        State pds = StateUtils.computePostDecisionState(s, a, this);
        double knownCost = 0.0;
        if (a.getDelta() != 0)
            knownCost += getwReconf();
        knownCost += getwResources() * StateUtils.computeDeploymentCostNormalized(pds, this);
        double v = qTable.getQ(pds, new Action(0,0,0));
        return v + knownCost;
    }
}
