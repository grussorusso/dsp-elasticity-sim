package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.VTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.VTableFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class QLearningPDSOM extends ReinforcementLearningOM {
    private VTable vTable;

    private VariableParameter alpha;
    private int alphaDecaySteps;
    private int alphaDecayStepsCounter;

    private boolean initWithVI;
    private boolean useEstimatedUnknownCost;
    private boolean approximateSpeedupsForCostEstimation;

    private double gamma;

    private Logger logger = LoggerFactory.getLogger(QLearningPDSOM.class);

    private ActionSelectionPolicy greedyActionSelection;

    public QLearningPDSOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        this.vTable = VTableFactory.newVTable(operator.getMaxParallelism(), getInputRateLevels());
        if (PolicyIOUtils.shouldLoadPolicy(configuration)) {
            this.vTable.load(PolicyIOUtils.getFileForLoading(this.operator, "vTable"));
        }

        double alphaInitValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_KEY, 0.1);
        double alphaDecay = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_DECAY_KEY, 1.0);
        double alphaMinValue = configuration.getDouble(ConfigurationKeys.QL_OM_ALPHA_MIN_VALUE_KEY, 0.1);



        this.alphaDecaySteps = configuration.getInteger(ConfigurationKeys.QL_OM_ALPHA_DECAY_STEPS_KEY, -1);
        this.alphaDecayStepsCounter = 0;

        this.initWithVI = configuration.getBoolean(ConfigurationKeys.MB_INIT_WITH_VI, false);
        this.useEstimatedUnknownCost = configuration.getBoolean(ConfigurationKeys.PDS_ESTIMATE_COSTS, false);
        this.approximateSpeedupsForCostEstimation = configuration.getBoolean(ConfigurationKeys.APPROX_SPEEDUPS, true);

        if (initWithVI && useEstimatedUnknownCost) {
            throw new IllegalArgumentException("Cannot use both initWithVI and useEstimatedUnknownCost!");
        }

        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY,0.99);

        if (initWithVI) {
            offlinePlanning();
            alphaInitValue = alphaMinValue;
        }

        this.alpha = new VariableParameter(alphaInitValue, alphaMinValue, 1.0, alphaDecay);
        logger.info("Alpha conf: init={}, decay={}, currValue={}", alphaInitValue,
                alphaDecay, alpha.getValue());

    }

    private void offlinePlanning() {
        ValueIterationOM viOM = new ValueIterationOM(operator);
        QTable viQ = viOM.getQTable();

        // init costs and transition estimates based on offline phase output
        /* Update cost estimate */
        StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        while (stateIterator.hasNext()) {
            State s = stateIterator.next();

            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                if (!validateAction(s, action))
                    continue;

                double q = viQ.getQ(s, action);
                State pds = StateUtils.computePostDecisionState(s, action, this);
                double knownCost = 0.0;
                if (action.getDelta() != 0)
                    knownCost += getwReconf();
                knownCost += getwResources() * StateUtils.computeDeploymentCostNormalized(pds, this);
                double v = q - knownCost;

                this.vTable.setV(pds, v);
            }

        }
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        this.greedyActionSelection = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );
        // TODO
        //return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        return this.greedyActionSelection;
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        final State pds = StateUtils.computePostDecisionState(oldState, action, this);

        double cRes = StateUtils.computeDeploymentCostNormalized(pds,this);
        double unknownCost = reward - getwResources()*cRes;
        //logger.info("{}+{}->{}: cRes={}", oldState, action, pds, cRes);

        if (action.getDelta()!=0)
            unknownCost -= getwReconf();

        // Can be used to learn only the difference w.r.t to an estimate
        final double diffCost = unknownCost - estimateUnknownCost(pds);

        final double oldV  = vTable.getV(oldState);
        final double newV = (1.0 - alpha.getValue()) * oldV + alpha.getValue() * (diffCost +
                        this.gamma * evaluateAction(currentState, greedyActionSelection.selectAction(currentState)));

        vTable.setV(pds, newV);

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
        this.vTable.dump(PolicyIOUtils.getFileForDumping(this.operator, "vTable"));

        // create file
        File file = new File("/tmp/qpds");
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File("/tmp/qpds"), true));
            StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
                    ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
            while (stateIterator.hasNext()) {
                State s = stateIterator.next();
                // print state line
                printWriter.println(s.dump());
                ActionIterator ait = new ActionIterator();
                while (ait.hasNext()) {
                    Action a = ait.next();
                    if (!s.validateAction(a))
                        continue;
                    double v = evaluateAction(s, a);
                    printWriter.print(String.format("%s\t%f\n", a.dump(), v));
                }
            }
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public double estimateUnknownCost (State pds) {
        if (this.useEstimatedUnknownCost) {
            // Here we assume that lambda does not change..
            return this.getwSLO() * StateUtils.computeSLOCost(pds, this, approximateSpeedupsForCostEstimation);
        } else {
            return 0.0;
        }
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
        double v = vTable.getV(pds);
        return v + estimateUnknownCost(pds) + knownCost;
    }
}
