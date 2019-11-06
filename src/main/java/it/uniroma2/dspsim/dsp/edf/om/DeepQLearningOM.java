package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeepQLearningOM extends ReinforcementLearningOM {

    private int numStatesFeatures;
    private int numActions;

    private MultiLayerConfiguration networkConf;
    private MultiLayerNetwork network;

    private double gamma;
    private double gammaDecay;


    public DeepQLearningOM(Operator operator) {
        super(operator);
    }

    @Override
    public void configure() {
        super.configure();

        // gamma
        this.gamma = this.getMetadata(ConfigurationKeys.DQL_OM_GAMMA_KEY, Double.class.getName());
        // gamma decay
        this.gammaDecay = this.getMetadata(ConfigurationKeys.DQL_OM_GAMMA_DECAY_KEY, Double.class.getName());
        // nd4j random seed
        long nd4jSeed = this.getMetadata(ConfigurationKeys.DQL_OM_ND4j_RANDOM_SEED_KET, Long.class.getName());
        Nd4j.getRandom().setSeed(nd4jSeed);

        // states and actions
        // TODO parametrize
        this.numStatesFeatures = computeNumStatesFeatures(operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure().getNodeTypes().length);
        this.numActions = 7;

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

        /*UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        this.network.setListeners(new StatsListener(statsStorage));*/
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

    private INDArray stateKToOneHotVector(int[] k) {
        INDArray array = Nd4j.create(this.numStatesFeatures);
        // generate one hot vector starting from (1, 0, 0, ... , 0)
        // to represent (1, 0, 0) state
        // and proceed with (0, 1, 0, ... , 0)
        // to represent (2, 0, 0) state and so on until
        // (0, 0, 0, ... , 1) to represent (0, 0, maxParallelism) state
        // TODO
        return array;
    }

    private int computeNumStatesFeatures(int maxParallelism, int nodeTypeNumber) {
        int[] maxNArray = new int[nodeTypeNumber];
        for (int i = 0; i < nodeTypeNumber; i++) {
            maxNArray[i] = maxParallelism;
        }

        List<int[]> permutations = new ArrayList<>();
        computePermutations(new int[nodeTypeNumber], maxNArray, maxNArray.length - 1, maxParallelism, permutations);
        for (int i = 0; i < permutations.size(); i++) {
            System.out.println(Arrays.toString(permutations.get(i)));
        }
        //return permutations.size();
        return 4;
    }

    private void computePermutations(int[] n, int[] maxN, int index, int maxSum, List<int[]> permutations) {
        if (index == -1) {
            int resSum = arraySum(n);
            if (resSum > 0 && resSum <= maxSum) {
                //System.out.println(Arrays.toString(n));
                permutations.add(n);
            }
            return;
        }
        for (int i = 0; i <= maxN[index]; i++) {
            n[index] = i;
            computePermutations(n.clone(), maxN, index - 1, maxSum, permutations);
        }
    }

    private int arraySum(int[] n) {
        int sum = 0;
        for (int value : n) sum += value;
        return sum;
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
