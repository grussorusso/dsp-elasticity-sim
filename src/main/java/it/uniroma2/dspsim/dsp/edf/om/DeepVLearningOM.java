package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Arrays;

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

    public DeepVLearningOM(Operator operator) {
        super(operator);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        // get post decision state from old state and action
        State pdState = StateUtils.computePostDecisionState(oldState, action, this);

        // get V(current state) as min{Q(current state, a) - c(a)} for each action
        ActionIterator ait = new ActionIterator();
        double bestQ = Double.POSITIVE_INFINITY;
        Action bestAction = new Action(0, 0, 0);
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!this.validateAction(currentState, a))
                continue;
            // Q(current state, a)
            double q = this.evaluateAction(currentState, a);
            // c(a)
            //double c = this.computeActionCost(a);
            double c = 0;

            final double diff = q - c;

            if (diff < bestQ) {
                bestQ = diff;
                bestAction = a;
            }
        }

        // compute new post decision state from current state and best action from current state
        State newPDState = StateUtils.computePostDecisionState(currentState, bestAction, this);
        // update old state output in actionIndex position with new estimation
        // we get min(newQ) because we want to minimize cost
        // reward = cost -> minimize Q equals minimize cost
        INDArray v = getV(pdState);
        v.put(0, 0, reward + gamma * bestQ);

        // get post decision input array
        INDArray trainingInput = buildInput(pdState);

        // training step
        this.learn(trainingInput, v);

        // decrement gamma if necessary
        decrementGamma();
    }

    private INDArray getV(State state) {
        INDArray input = buildInput(state);
        return this.network.output(input);
    }

    private INDArray getQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        INDArray v = getV(postDecisionState);
        v.put(0, 0, v.getDouble(0) + computeActionCost(action));
        return v;
    }

    private double computeActionCost(Action action) {
        if (action.getDelta() != 0)
            return this.getwReconf();
        else
            return 0;
    }

    private INDArray buildInput(State state) {
        //State indexedState = getIndexedState(state);
        return state.arrayRepresentation(this.stateFeatures);
    }

    /**
     * DEEP LEARNING OM
     */

    @Override
    protected int computeOutputLayerNodesNumber() { return 1; }

    @Override
    protected int computeInputLayerNodesNumber() {
        return this.stateFeatures;
    }


    @Override
    protected MultiLayerConfiguration buildNeuralNetwork() {
        return new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        new DenseLayer.Builder()
                                .nIn(this.inputLayerNodesNumber)
                                .nOut(64)
                                .activation(Activation.RELU)
                                .build(),
                        new DenseLayer.Builder()
                                .nIn(64)
                                .nOut(128)
                                .activation(Activation.RELU)
                                .build(),
                        new DenseLayer.Builder()
                                .nIn(128)
                                .nOut(64)
                                .activation(Activation.RELU)
                                .build(),
                        new DenseLayer.Builder()
                                .nIn(64)
                                .nOut(32)
                                .activation(Activation.RELU)
                                .dropOut(0.5)
                                .build(),
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(32)
                                .nOut(this.outputLayerNodesNumber)
                                .build()
                )
                .pretrain(false)
                .backprop(true)
                .build();
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateAction(State s, Action a) {
        // return network Q-function prediction associated to action a in state s
        INDArray networkOutput = getQ(s, a);
        return networkOutput.getDouble(0);
    }
}
