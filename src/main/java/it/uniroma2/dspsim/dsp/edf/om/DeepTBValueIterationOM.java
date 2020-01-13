package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
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
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class DeepTBValueIterationOM extends BaseTBValueIterationOM {

    private int inputLayerNodesNumber;
    private int outputLayerNodesNumber;

    private INDArray training = null;
    private INDArray labels = null;

    private int memoryBatch;

    //policy
    protected MultiLayerNetwork policy;

    public DeepTBValueIterationOM(Operator operator) {
        super(operator);

        if (Configuration.getInstance().getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }

        // TODO configure
        this.memoryBatch = 32;

        // TODO configure
        tbvi(60000, 512);

        dumpQOnFile(String.format("%s/%s/%s/policy",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
                "others"));

        printTBVIResults();
    }

    private INDArray buildInput(State state) {
        return state.arrayRepresentation(this.inputLayerNodesNumber);
    }

    private INDArray getV(State state) {
        INDArray input = buildInput(state);
        return this.policy.output(input, false);
    }

    private INDArray getQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        INDArray v = getV(postDecisionState);
        v.put(0, 0, v.getDouble(0) + (action.getDelta() != 0 ? getwReconf() : 0));
        return v;
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return getQ(s, a).getDouble(0);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    @Override
    protected void buildQ() {
        this.inputLayerNodesNumber = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(),
                this.getInputRateLevels()).next().getArrayRepresentationLength();

        this.outputLayerNodesNumber = 1;

        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        /*
                        new DenseLayer.Builder()
                                .nIn(this.inputLayerNodesNumber)
                                .nOut(32)
                                .activation(Activation.RELU)
                                .build(),
                         */
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

        this.policy = new MultiLayerNetwork(config);
        policy.init();
    }

    @Override
    protected void dumpQOnFile(String filename) {
        // create file
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
            printWriter.print(this.policy.getLayerWiseConfigurations().toJson());
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startNetworkUIServer() {
        // http://localhost:9000/train
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        this.policy.setListeners(new StatsListener(statsStorage));
    }

    private void printTBVIResults() {
        StateIterator stateIterator = new StateIterator(getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
        while (stateIterator.hasNext()) {
            State s = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action a = actionIterator.next();
                if (validateAction(s, a)) {
                    System.out.println(s.dump() + "\t" + a.dump() + "\tV: " + getV(StateUtils.computePostDecisionState(s, a, this)));
                }
            }
        }
    }

    @Override
    protected void resetTrajectoryData() {
        // reset memory
        this.training = null;
        this.labels = null;
    }

    @Override
    protected double computeQ(State s, Action a) {
        return this.getQ(s, a).getDouble(0);
    }

    @Override
    protected void learn(double tbviDelta, double reward, State state, Action action) {
        // compute post decision state
        State pds = StateUtils.computePostDecisionState(state, action, this);

        // compute q of post decision state and action chosen by action selection policy in post decision state
        double q = computeQ(pds, getActionSelectionPolicy().selectAction(pds));

        // transform post decision state in network input
        INDArray trainingInput = buildInput(pds);
        // set label to reward + gamma * q
        INDArray label = Nd4j.create(1).put(0, 0, reward + (getGamma() * q));

        // init memory if it is null or add example and label to memory
        if (this.training == null && this.labels == null) {
            this.training = trainingInput;
            this.labels = label;
        } else {
            training = Nd4j.concat(0, training, trainingInput);
            labels = Nd4j.concat(0, labels, label);
        }
        // shuffle memory and get first memory batch elements
        DataSet memory = new DataSet(this.training, this.labels);
        memory.shuffle();
        List<DataSet> batches = memory.batchBy(this.memoryBatch);
        // train network
        this.policy.fit(batches.get(0));
    }
}
