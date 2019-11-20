package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class DeepQLearningOM extends ReinforcementLearningOM {

    private int numStatesFeatures;
    private int numActions;

    private MultiLayerConfiguration networkConf;
    private MultiLayerNetwork network;

    private double gamma;
    private double gammaDecay;
    private int gammaDecaySteps;
    private int gammaDecayStepsCounter;


    public DeepQLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // gamma
        this.gamma = configuration.getDouble(ConfigurationKeys.DQL_OM_GAMMA_KEY, 0.9);
        // gamma decay
        this.gammaDecay = configuration.getDouble(ConfigurationKeys.DQL_OM_GAMMA_DECAY_KEY, 0.9);
        // gamma decay steps
        this.gammaDecaySteps = configuration.getInteger(ConfigurationKeys.DQL_OM_GAMMA_DECAY_STEPS_KEY, -1);
        // gamma decay steps counter (init)
        this.gammaDecayStepsCounter = 0;

        // nd4j random seed
        long nd4jSeed = configuration.getLong(ConfigurationKeys.DQL_OM_ND4j_RANDOM_SEED_KET, 1234L);
        Nd4j.getRandom().setSeed(nd4jSeed);

        /*states :
            this.getTotalStates() / (this.inputRateLevels + 1) = one hot vector input nodes (k[])
            + 1 = lambda input level normalized
        */
        this.numStatesFeatures = (this.getTotalStates() / (this.getInputRateLevels() + 1)) + 1;
        this.numActions = this.getTotalActions();

        // build network
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

        /*
        // http://localhost:9000/train
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        this.network.setListeners(new StatsListener(statsStorage));
        */
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
        INDArray trainingInput = buildInput(oldState);

        // training step
        this.network.fit(trainingInput, oldQ);

        // decrement gamma if necessary
        decrementGamma();
    }

    @Override
    protected State computeNewState(OMMonitoringInfo monitoringInfo) {
        // TODO
        /*
        State newState = super.computeNewState(monitoringInfo);

        StateIterator stateIterator = new StateIterator(this.operator,
                ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        while (stateIterator.hasNext()) {
            State indexedNewState = stateIterator.next();
            if (newState.equals(indexedNewState)) {
                return indexedNewState;
            }
        }

        throw new RuntimeException("No possible state generated");
        */
        return super.computeNewState(monitoringInfo);
    }

    private INDArray getQ(State state) {
        INDArray input = buildInput(state);
        return this.network.output(input);
    }

    private INDArray buildInput(State state) {
        INDArray input = stateKToOneHotVector(state);
        // append lambda level normalized value to input array
        // TODO
        // input = Nd4j.append(input, 1, lambdaLevelNormalized(state.getLambda(), this.getInputRateLevels()), 1);
        input = Nd4j.append(input, 1, lambdaLevelNormalized(10, this.getInputRateLevels()), 1);
        return input;
    }

    private INDArray stateKToOneHotVector(State state) {
        // get only k[] nodes
        INDArray oneHotVector = Nd4j.create(this.numStatesFeatures - 1);
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, ... , 0) state
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (2, 0, 0) state and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, ... , maxParallelism) state
        // TODO
        // oneHotVector.put(0, Math.floorDiv(state.getIndex(), this.getInputRateLevels() + 1), 1);
        oneHotVector.put(0, Math.floorDiv(0, this.getInputRateLevels() + 1), 1);
        return oneHotVector;
    }

    private double lambdaLevelNormalized(int value, int maxValue) {
        return (double) value/ (double) maxValue;
    }

    private int getTotalStates() {
        StateIterator stateIterator = new StateIterator(this.operator,
                ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());
        return this.getTotalObjectsInIterator(stateIterator);
    }

    private int getTotalActions() {
        ActionIterator actionIterator = new ActionIterator();
        return this.getTotalObjectsInIterator(actionIterator);
    }

    private int getTotalObjectsInIterator(Iterator iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        System.out.println(count);
        return count;
    }

    private void decrementGamma() {
        if (this.gammaDecaySteps > 0) {
            this.gammaDecayStepsCounter++;
            if (this.gammaDecayStepsCounter >= this.gammaDecaySteps) {
                this.gammaDecayStepsCounter = 0;
                this.gamma = this.gammaDecay * this.gamma;
            }
        }
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
