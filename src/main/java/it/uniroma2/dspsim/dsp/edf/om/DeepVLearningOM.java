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
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.CachedNeuralNetwork;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.Transition;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.Collection;

/**
 * Deep Q Learning variant.
 * This OM uses V function as objective function.
 * It computes s' (post-decision state) state from s (current state) and a (valid action) for each valid action a.
 * Then computes V(s') through its neural network and add c(a) (action cost) to V(s') in order to obtain Q(s,a)
 * of current state and action couple.
 * if action.getDelta() != 0 c(a) = reconfiguration's weight else c(a) = 0
 * To chose best action it selects min Q(s,a) for each a
 * In learning step phase it subtracts c(a) from Q(s,a) to obtain V(s) as label to train the neural network
 */
public class DeepVLearningOM extends DeepLearningOM {

    // this asp is used to select action in learning step
    private ActionSelectionPolicy greedyASP, targetGreedyASP;

    private INDArray inputs;
    private INDArray labels;

    private boolean useEstimatedUnknownCost;
    private boolean approximateSpeedupsForCostEstimation;
    private boolean approximateOperatorModel;

    public DeepVLearningOM(Operator operator) {
        super(operator);

        Configuration configuration = Configuration.getInstance();

        this.useEstimatedUnknownCost = configuration.getBoolean(ConfigurationKeys.PDS_ESTIMATE_COSTS, false);
        this.approximateSpeedupsForCostEstimation = configuration.getBoolean(ConfigurationKeys.APPROX_SPEEDUPS, true);
        this.approximateOperatorModel = configuration.getBoolean(ConfigurationKeys.VI_APPROX_MODEL, true);

        this.inputs = Nd4j.create(batchSize, neuralStateRepresentation.getRepresentationLength());
        this.labels = Nd4j.create(batchSize, 1);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        this.greedyASP = ActionSelectionPolicyFactory.getPolicy(
                ActionSelectionPolicyType.GREEDY,
                this
        );

        if (!useDoubleNetwork) {
            this.targetGreedyASP = this.greedyASP;
        } else {
            this.targetGreedyASP = ActionSelectionPolicyFactory.getPolicy(
                    ActionSelectionPolicyType.GREEDY,
                    new ActionSelectionPolicyCallback() {
                        @Override
                        public boolean validateAction(State s, Action a) {
                            return this.validateAction(s,a);
                        }

                        @Override
                        public double evaluateAction(State s, Action a) {
                            return getQ(s, a, targetNetwork);
                        }
                    }
            );

        }
        return this.greedyASP;
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        this.expReplay.add(new Transition(oldState, action, currentState, reward));

        // training step
        this.learn();
    }

    @Override
    protected Pair<INDArray, INDArray> getTargets(Collection<Transition> batch) {
        int row = 0;
        for (Transition t : batch) {
            // get post decision state from old state and action
            State pdState = StateUtils.computePostDecisionState(t.getS(), t.getA(), this);

            // unknown cost
            double cU = t.getReward() - computeActionCost(t.getA()) -
                    (StateUtils.computeDeploymentCostNormalized(pdState, this) * this.getwResources());

            // Can be used to learn only the difference w.r.t to an estimate
            final double diffCost = cU - estimateUnknownCost(pdState);

            Action greedyAction = this.targetGreedyASP.selectAction(t.getNextS());
            double newV = getQ(t.getNextS(), greedyAction, this.targetNetwork) * gamma + diffCost;

            labels.put(row, 0, newV);

            // get post decision input array
            INDArray trainingInput = buildInput(pdState);
            inputs.putRow(row, trainingInput);

            ++row;
        }

        return Pair.of(inputs, labels);
    }

    private double getV(State state, CachedNeuralNetwork neuralNet) {
        return neuralNet.output(state).getDouble(0);
    }

    private double getV(State state) {
        return this.network.output(state).getDouble(0);
    }

    private double getQ(State state, Action action) {
        return getQ(state, action, this.network);
    }

    private double getQ(State state, Action action, CachedNeuralNetwork neuralNet) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        double knownCost = computeActionCost(action);
        knownCost += (StateUtils.computeDeploymentCostNormalized(postDecisionState, this) * this.getwResources());
        double v = getV(postDecisionState, neuralNet);
        return v + estimateUnknownCost(postDecisionState) + knownCost;
    }

    public double estimateUnknownCost (State pds) {
        if (this.useEstimatedUnknownCost) {
            // Here we assume that lambda does not change..
            return this.getwSLO() * StateUtils.computeSLOCost(pds, this, approximateSpeedupsForCostEstimation,
                    approximateOperatorModel);
        } else {
            return 0.0;
        }
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    private double computeActionCost(Action action) {
        if (action.getDelta() != 0)
            return this.getwReconf();
        else
            return 0;
    }


    /**
     * DEEP LEARNING OM
     */

    @Override
    protected int computeOutputLayerNodesNumber() { return 1; }

    @Override
    protected int computeInputLayerNodesNumber() {
        return this.neuralStateRepresentation.getRepresentationLength();
    }


    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        // return network Q-function prediction associated to action a in state s
        return getQ(s, a);
    }
}
