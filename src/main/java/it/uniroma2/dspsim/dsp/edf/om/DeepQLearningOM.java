package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.Convolution1DLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.primitives.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class DeepQLearningOM extends ReinforcementLearningOM {

    private int numStatesFeatures;
    private int numActions;

    private MultiLayerConfiguration networkConf;
    private MultiLayerNetwork network;

    private Random actionSelectionRng = new Random();

    private double gamma;


    public DeepQLearningOM(Operator operator) {
        super(operator);

        // TODO parametrize
        this.numStatesFeatures = 4;
        this.numActions = 7;

        this.gamma = 0.9;

        this.networkConf = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        new DenseLayer.Builder()
                                .nIn(numStatesFeatures)
                                .nOut(32)
                                .activation(new ActivationReLU())
                                .build(),
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                //.activation(Activation.SOFTMAX)
                                .nIn(32)
                                .nOut(numActions)
                                .build()
                )
                .backprop(true)
                .build();

        this.network = new MultiLayerNetwork(this.networkConf);
        this.network.init();
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
        oldQ.put(0, action.getIndex(), reward + gamma * (double) newQ.minNumber());

        // get old state input array
        INDArray trainingInput = stateToArray(oldState);

        // training step
        this.network.fit(trainingInput, oldQ);
    }

    private INDArray getQ(State state) {
        INDArray input = stateToArray(state);
        return this.network.output(input);
    }

    private INDArray stateToArray(State s) {
        INDArray array = Nd4j.create(this.numStatesFeatures);
        for (int i = 0; i < s.getK().length; i++) {
            array.put(0, i, s.getK()[i]);
        }
        array.put(0, this.numStatesFeatures - 1, s.getLambda());
        return array;
    }

    private INDArray stateToOneHotVector(State s) {
        INDArray array = Nd4j.create(this.numStatesFeatures);
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, 0, 1) state
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (1, 0, 0, 2) state and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, maxParallelism, maxLambdaLevel) state
        //TODO
        return array;
    }

    /**
     * ACTION SELECTION POLICY CALLBACK INTERFACE
     */

    @Override
    public double evaluateQ(State s, Action a) {
        // return network Q-function prediction associated to action a in state s
        INDArray networkOutput = getQ(s);
        return networkOutput.getDouble(a.getIndex());
    }
}
