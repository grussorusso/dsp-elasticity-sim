package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.*;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Set;

public class DeepTBValueIterationOM extends BaseTBValueIterationOM {

    private int inputLayerNodesNumber;
    private int outputLayerNodesNumber;

    private ExperienceReplay expReplay;
    private int batchSize;

    private Logger log = LoggerFactory.getLogger(DeepTBValueIterationOM.class);

    protected MultiLayerNetwork network;

    public DeepTBValueIterationOM(Operator operator) {
        super(operator);

        if (Configuration.getInstance().getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }

        this.batchSize = Configuration.getInstance().getInteger(ConfigurationKeys.TBVI_DEEP_MEMORY_BATCH_KEY,32);
        this.expReplay = new ExperienceReplay(10000);

        tbvi(this.tbviIterations, this.tbviMillis, this.tbviTrajectoryLength);

       // dumpQOnFile(String.format("%s/%s/%s/policy",
       //         Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
       //         Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
       //         "others"));

        //printTBVIResults();
    }

    private INDArray buildInput(State state) {
        return state.arrayRepresentation(this.inputLayerNodesNumber);
    }

    private INDArray getV(State state) {
        INDArray input = buildInput(state);
        return this.network.output(input, false);
    }

    private INDArray getQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        INDArray v = getV(postDecisionState);
        v.put(0, 0, v.getDouble(0) + computeActionCost(action));
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
                .updater(new Sgd(0.1))
                //.updater(new RmsProp())
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

        this.network = new MultiLayerNetwork(config);
        network.init();
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
            printWriter.print(this.network.getLayerWiseConfigurations().toJson());
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startNetworkUIServer() {
        // http://localhost:9000/train
        /*
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        this.network.setListeners(new StatsListener(statsStorage));
        */
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
    }

    @Override
    protected double computeQ(State s, Action a) {
        return this.getQ(s, a).getDouble(0);
    }

    @Override
    protected void learn(double tbviDelta, double newQ, State state, Action action) {
        // not used
    }

    @Override
    protected State tbviIteration(State s, Action a) {
        //double oldQ = computeQ(s, a);
        //double newQ = evaluateNewQ(s, a);
        //double delta = newQ - oldQ;
        //learn(delta, newQ, s, a);
        //return sampleNextState(s, a);

        Transition t = new Transition(s, a, null, 0.0);
        expReplay.add(t);

        // check if u want to perform an update
        if (tbviIterations % 10 == 0 && tbviIterations > 100) {
            Collection<Transition> batch = expReplay.sampleBatch(this.batchSize);
            if (batch != null) {
                Pair<INDArray, INDArray> targets = getTargets(batch);
                this.network.fit(targets.getLeft(), targets.getRight());
            }
        }

        return sampleNextState(s,a);
    }

    private Pair<INDArray, INDArray> getTargets(Collection<Transition> batch) {
        INDArray inputs = null;
        INDArray labels = null;

        for (Transition t : batch) {
            State pds = StateUtils.computePostDecisionState(t.getS(), t.getA(), this);
            // transform post decision state in network input
            INDArray trainingInput = buildInput(pds);
            // set label to reward
            double targetValue = evaluateNewQ(t.getS(), t.getA()) - computeActionCost(t.getA());
            INDArray label = Nd4j.create(1).put(0, 0, targetValue);

            if (inputs == null) {
                inputs = trainingInput;
                labels = label;
            } else {
                inputs = Nd4j.concat(0, inputs, trainingInput);
                labels = Nd4j.concat(0, labels, label);
            }
        }
        return Pair.of(inputs, labels);
    }

    private double computeActionCost(Action action) {
        if (action.getDelta() != 0) {
            return this.getwReconf();
        } else {
            return 0.0;
        }
    }
}
