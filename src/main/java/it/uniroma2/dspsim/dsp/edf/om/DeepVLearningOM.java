package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Arrays;
import java.util.Iterator;

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
        State pdState = computePostDecisionState(oldState, action);

        // get V(current state) as min{Q(current state, a) - c(a)} for each action
        ActionIterator ait = new ActionIterator();
        double bestV = Double.POSITIVE_INFINITY;
        Action bestAction = new Action(0, 0, 0);
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!this.actionValidation(currentState, a))
                continue;
            // Q(current state, a)
            double q = this.evaluateQ(currentState, a);
            // c(a)
            double c = this.computeActionCost(a);

            final double diff = q - c;

            if (diff < bestV) {
                bestV = diff;
                bestAction = a;
            }
        }

        // compute new post decision state from current state and best action from current state
        State newPDState = computePostDecisionState(currentState, bestAction);
        // update old state output in actionIndex position with new estimation
        // we get min(newQ) because we want to minimize cost
        // reward = cost -> minimize Q equals minimize cost
        INDArray v = getV(pdState);
        v.put(0, 0, reward + gamma * getV(newPDState).getDouble(0));

        // get post decision input array
        INDArray trainingInput = buildInput(pdState);

        // training step
        this.network.fit(trainingInput, v);

        // decrement gamma if necessary
        decrementGamma();
    }

    private INDArray getV(State state) {
        INDArray input = buildInput(state);
        return this.network.output(input);
    }

    private INDArray getQ(State state, Action action) {
        State postDecisionState = computePostDecisionState(state, action);
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

    private State computePostDecisionState(State state, Action action) {
        if (action.getDelta() != 0) {
            int[] pdk = Arrays.copyOf(state.getActualDeployment(), state.getActualDeployment().length);
            int aIndex = action.getResTypeIndex();
            pdk[aIndex] = pdk[aIndex] + action.getDelta();
            return StateFactory.createState(this.getStateRepresentation(), -1, pdk,
                    state.getLambda(), this.getInputRateLevels(), this.operator);
        } else {
            return state;
        }
    }

    private INDArray buildInput(State state) {
        State indexedState = getIndexedState(state);
        return indexedState.arrayRepresentation(this.stateFeatures);
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
                                .nOut(32)
                                .activation(new ActivationReLU())
                                .build(),
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                //.activation(Activation.SOFTMAX)
                                .nIn(32)
                                .nOut(this.outputLayerNodesNumber)
                                .build()
                )
                .backprop(true)
                .build();
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateQ(State s, Action a) {
        // return network Q-function prediction associated to action a in state s
        INDArray networkOutput = getQ(s, a);
        return networkOutput.getDouble(0);
    }
}
