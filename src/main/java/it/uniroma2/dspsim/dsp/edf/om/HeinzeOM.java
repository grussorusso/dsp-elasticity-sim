package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.QBasedReconfigurationScore;
import it.uniroma2.dspsim.dsp.edf.om.request.RewardBasedOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.HeinzeQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.HeinzeState;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;

public class HeinzeOM extends OperatorManager {

    protected Action lastChosenAction;
    protected HeinzeState lastState;

    private double targetUtil;
    private HeinzeQTable qTable;

    private VariableParameter alpha;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private double gamma;

    public HeinzeOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.targetUtil = configuration.getDouble(ConfigurationKeys.OM_THRESHOLD_KEY, 0.7);
        this.qTable = new HeinzeQTable(0.0);

        // TODO: preinitialize qTable

        double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 1.0);
        double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 0.98);
        double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);

        this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);

        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY,0.99);
    }

    @Override
    public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {
        // compute new state
        HeinzeState newState = new HeinzeState(monitoringInfo.getCpuUtilization());

        // pick new action: e-greedy
        ActionIterator ait = new ActionIterator();
        Action best = null;
        double minCost = -1.0;
        while (ait.hasNext()) {
            Action a = ait.next();
            if (this.operator.getCurrentParallelism() + a.getDelta() < 1)
                continue;
            if (this.operator.getCurrentParallelism() + a.getDelta() > operator.getMaxParallelism())
                continue;
            double q = qTable.getQ(newState, a);
            if (best == null || q < minCost) {
                best = a;
                minCost = q;
            }
        }
        Action newAction = best;

        // learning step
        if (lastChosenAction != null) {
            // compute reconfiguration's cost and use it as reward
            double reward = computeCost(lastChosenAction, lastState, newState);

            final double oldQ  = qTable.getQ(lastState, lastChosenAction);
            final double newQ = (1.0 - alpha.getValue()) * oldQ + alpha.getValue() * (reward +
                    this.gamma * qTable.getQ(newState, newAction));

            qTable.setQ(lastState, lastChosenAction, newQ);

            // TODO: enforce monotonicity
        }


        // update state
        lastState = newState;
        lastChosenAction = newAction;

        decrementAlpha();

        return prepareOMRequest(newState, lastChosenAction);
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

    protected OMRequest prepareOMRequest(HeinzeState currentState, Action chosenAction) {
        /*
         * Provide information to the AM.
         */

        return new RewardBasedOMRequest(action2reconfiguration(chosenAction),
                new QBasedReconfigurationScore(-1.0), new QBasedReconfigurationScore(-1.0));
    }

    protected double computeCost(Action action, HeinzeState prevState, HeinzeState newState) {
        double cost = Math.abs(newState.getUtil() - this.targetUtil);
        return cost;
    }

    static public Reconfiguration action2reconfiguration(Action action) {
        int delta = action.getDelta();

        if (delta > 1 || delta < -1) throw new RuntimeException("Unsupported action!");

        if (delta == 0) return Reconfiguration.doNothing();

        return delta > 0 ?
                Reconfiguration.scaleOut(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]) :
                Reconfiguration.scaleIn(
                        ComputingInfrastructure.getInfrastructure().getNodeTypes()[action.getResTypeIndex()]);
    }

}
