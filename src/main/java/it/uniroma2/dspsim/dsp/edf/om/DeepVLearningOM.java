package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DropoutLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

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
public class DeepVLearningOM extends ReinforcementLearningOM {

    private int numStatesFeatures;
    private int numActions;

    private int outputLayerNodesNumber;

    private MultiLayerConfiguration networkConf;
    private MultiLayerNetwork network;

    private double gamma;
    private double gammaDecay;
    private int gammaDecaySteps;
    private int gammaDecayStepsCounter;

    public DeepVLearningOM(Operator operator) {
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

        this.outputLayerNodesNumber = 1;

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
                                .nOut(outputLayerNodesNumber)
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
            //double c = this.computeActionCost(a);
            double c = 0.0;

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

    private State getIndexedState(State state) {
        if (state.getIndex() != -1)
            return state;

        StateIterator stateIterator = new StateIterator(this.operator,
                ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        while (stateIterator.hasNext()) {
            State indexedNewState = stateIterator.next();
            if (state.equals(indexedNewState)) {
                return indexedNewState;
            }
        }

        throw new RuntimeException("No possible state generated");
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
            int[] pdK = new int[state.getK().length];
            for (int i = 0; i < pdK.length; i++) {
                if (action.getResTypeIndex() == i) {
                    pdK[i] = state.getK()[i] + action.getDelta();
                } else {
                    pdK[i] = state.getK()[i];
                }
            }
            return new State(state.getIndex(), pdK, state.getLambda());
        } else {
            return state;
        }
    }

    private INDArray buildInput(State state) {
        State indexedState = getIndexedState(state);
        INDArray input = stateKToOneHotVector(indexedState);
        // append lambda level normalized value to input array
        input = Nd4j.append(input, 1, lambdaLevelNormalized(state.getLambda(), this.getInputRateLevels()), 1);
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
        oneHotVector.put(0, Math.floorDiv(state.getIndex(), this.getInputRateLevels() + 1), 1);
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
        INDArray networkOutput = getQ(s, a);
        return networkOutput.getDouble(0);
    }
}
