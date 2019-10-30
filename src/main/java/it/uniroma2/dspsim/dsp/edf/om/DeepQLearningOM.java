package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
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

import java.util.ArrayList;
import java.util.Random;

public class DeepQLearningOM extends ReinforcementLearningOM {

    private int numStates;
    private int numActions;

    private MultiLayerConfiguration networkConf;
    private MultiLayerNetwork network;

    private Random actionSelectionRng = new Random();

    private double gamma;
    private double epsilon;


    public DeepQLearningOM(Operator operator) {
        super(operator);

        // TODO parametrize
        this.numStates = 3;
        this.numActions = 9;

        this.gamma = 0.9;

        this.epsilon = 0.05;

        this.networkConf = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        new DenseLayer.Builder()
                                .nIn(numStates)
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

        // get action index
        int actionIndex = actionToIndex(action);
        if (actionIndex != -1) {
            // update old state output in actionIndex position with new estimation
            // we get min(newQ) because we want to minimize cost
            // reward = cost -> minimize Q equals minimize cost
            // TODO right?
            oldQ.put(0, actionIndex, reward + gamma * (double) newQ.minNumber());

            // get old state input array
            INDArray trainingInput = stateToArray(oldState);

            // create dataset with new training input and oldQ as label
            DataSet dataSet = new DataSet(trainingInput, oldQ);

            // training step
            this.network.fit(dataSet);
        }
    }

    private int actionToIndex(Action action) {
        // iterate over actions to find action index
        ActionIterator ait = new ActionIterator();
        int count = 0;
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (action.equals(a)) {
                return count;
            }
            count++;
        }
        return -1;
    }

    // array's arg min
    private int argMin(INDArray array) {
        double min = Double.POSITIVE_INFINITY;
        int index = -1;
        for (int i = 0; i < array.length(); i++) {
            if (min > array.getDouble(i)) {
                min = array.getDouble(i);
                index = i;
            }
        }
        return index;
    }

    @Override
    protected Action pickNewAction(State state) {
        return epsilonGreedyActionSelection(state, epsilon);
    }

    private Action epsilonGreedyActionSelection (State s, double epsilon) {
        if (actionSelectionRng.nextDouble() <= epsilon)
            return randomActionSelection(s);
        else
            return greedyActionSelection(s);
    }

    private Action randomActionSelection(State s) {
        ArrayList<Action> actions = new ArrayList<>();
        ActionIterator ait = new ActionIterator();
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (!a.isValidInState(s, this.operator))
                continue;
            actions.add(a);
        }

        int aIndex = actionSelectionRng.nextInt(actions.size());
        System.out.println(aIndex);
        return actions.get(aIndex);
    }


    private Action greedyActionSelection (State s) {
        int prediction = argMin(getQ(s));
        ActionIterator ait = new ActionIterator();
        Action newAction = null;
        int count = 0;
        while (ait.hasNext()) {
            final Action a = ait.next();
            if (count == prediction) {
                if (!a.isValidInState(s, this.operator))
                    // TODO if action predicted isn't valid in state s?
                    newAction = new Action(0, 0);
                else
                    newAction = a;
                break;
            }
            count++;
        }
        System.out.println(prediction);
        return newAction;
    }

    private INDArray getQ(State state) {
        INDArray input = stateToArray(state);
        return this.network.output(input);
    }

    private INDArray stateToArray(State s) {
        INDArray array = Nd4j.create(s.getK().length);
        for (int i = 0; i < s.getK().length; i++) {
            array.put(0, i, s.getK()[i]);
        }
        return array;
    }
}
