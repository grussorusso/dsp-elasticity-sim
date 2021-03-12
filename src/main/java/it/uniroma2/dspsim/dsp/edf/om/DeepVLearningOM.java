package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.Transition;
import org.apache.commons.lang3.tuple.Pair;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
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


    public DeepVLearningOM(Operator operator) {
        super(operator);
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
                            return getTargetQ(s, a);
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
        INDArray inputs = null;
        INDArray labels = null;

        for (Transition t : batch) {
            // get post decision state from old state and action
            State pdState = StateUtils.computePostDecisionState(t.getS(), t.getA(), this);

            // unknown cost
            double cU = t.getReward() - computeActionCost(t.getA()) -
                    (StateUtils.computeDeploymentCostNormalized(pdState, this) * this.getwResources());
            Action greedyAction = this.targetGreedyASP.selectAction(t.getNextS());
            double newV = getTargetQ(t.getNextS(), greedyAction) * gamma + cU;

            INDArray v = new NDArray(1,1);
            v.put(0, 0, newV);

            // get post decision input array
            INDArray trainingInput = buildInput(pdState);

            if (inputs == null) {
                inputs = trainingInput;
                labels = v;
            } else {
                inputs = Nd4j.concat(0, inputs, trainingInput);
                labels = Nd4j.concat(0, labels, v);
            }
        }

        return Pair.of(inputs, labels);
    }

    private double getV(State state) {
        INDArray input = buildInput(state);
        INDArray output = this.network.output(input);

        return output.getDouble(0);
    }

    private double getTargetV (State state) {
        if (hasNetworkCache() && networkCache.containsKey(state))
            return (double)networkCache.get(state);

        INDArray input = buildInput(state);
        INDArray output = this.targetNetwork.output(input);

        double v = output.getDouble(0);

        if (hasNetworkCache())
            networkCache.put(state, v);

        return v;
    }

    private double getQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        double v = getV(postDecisionState);
        return v + computeActionCost(action) + (StateUtils.computeDeploymentCostNormalized(postDecisionState, this) * this.getwResources());
    }

    private double getTargetQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        double v = getTargetV(postDecisionState);
        return v + computeActionCost(action) + (StateUtils.computeDeploymentCostNormalized(postDecisionState, this) * this.getwResources());
    }

    private double computeActionCost(Action action) {
        if (action.getDelta() != 0)
            return this.getwReconf();
        else
            return 0;
    }

    private INDArray buildInput(State state) {
        return state.arrayRepresentation(this.neuralStateRepresentation);
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
