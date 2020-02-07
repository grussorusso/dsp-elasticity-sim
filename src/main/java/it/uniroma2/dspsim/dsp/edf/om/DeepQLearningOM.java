package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
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
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.Iterator;

public class DeepQLearningOM extends DeepLearningOM {

    public DeepQLearningOM(Operator operator) {
        super(operator);
    }

    @Override
    protected void learningStep(State oldState, Action action, State currentState, double reward) {
        // get old state network output
        INDArray oldQ = getQ(oldState);
        // get current state network output
        INDArray newQ = getQ(currentState);

        // update old state output in actionIndex position with new estimation
        // we get min(newQ) because we want to minimize cost
        // reward = cost -> minimize Q equals minimize cost
        oldQ.put(0, action.getIndex(), reward + gamma.getValue() * (double) newQ.minNumber());

        // get old state input array
        INDArray trainingInput = buildInput(oldState);

        // training step
        this.learn(trainingInput, oldQ);

        // decrement gamma if necessary
        decrementGamma();
    }

    private INDArray getQ(State state) {
        INDArray input = buildInput(state);
        return this.network.output(input);
    }

    private INDArray buildInput(State state) {
        //State indexedState = getIndexedState(state);
        return state.arrayRepresentation(this.stateFeatures);
    }

    /**
     * DEEP LEARNING OM
     */

    @Override
    protected int computeOutputLayerNodesNumber() {
        return this.numActions;
    }

    @Override
    protected int computeInputLayerNodesNumber() {
        return this.stateFeatures;
    }

    @Override
    protected MultiLayerConfiguration buildNeuralNetwork() {
        return new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.1))
                .list(
                        new DenseLayer.Builder()
                                .nIn(this.inputLayerNodesNumber)
                                .nOut(this.inputLayerNodesNumber * 2)
                                .activation(Activation.RELU)
                                .build(),
                        new DenseLayer.Builder()
                                .nIn(this.inputLayerNodesNumber * 2)
                                .nOut(this.inputLayerNodesNumber)
                                .activation(Activation.RELU)
                                .build(),
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .nIn(this.inputLayerNodesNumber)
                                .nOut(this.outputLayerNodesNumber)
                                .activation(Activation.IDENTITY)
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
        INDArray networkOutput = getQ(s);
        return networkOutput.getDouble(a.getIndex());
    }
}
