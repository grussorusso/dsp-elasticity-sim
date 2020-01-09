package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.InputRateFileReader;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.RandomActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.IntegerMatrix;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.GradientNormalization;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.DropoutLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.iterator.BaseDatasetIterator;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class DeepTBValueIterationOM extends DynamicProgrammingOM implements ActionSelectionPolicyCallback {

    private int inputLayerNodesNumber;
    private int outputLayerNodesNumber;

    private int statesCount;
    private int actionsCount;

    private Random rng;

    private INDArray training = null;
    private INDArray labels = null;

    //policy
    private MultiLayerNetwork policy;

    public DeepTBValueIterationOM(Operator operator) {
        super(operator);

        this.actionsCount = computeActionsCount();
        this.statesCount = computeStatesCount();
        this.rng = new Random();

        if (Configuration.getInstance().getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }

        // TODO configure
        tbvi(60000, 512, 32);

        dumpPolicyOnFile(String.format("%s/%s/%s/policy",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
                "others"));

        printTBVIResults();
    }

    private void tbvi(long millis, long trajectoryLength, int batchSize) {
        ActionSelectionPolicy randomASP = ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.RANDOM, this);
        // get initial state
        State state = null;
        // current trajectory length
        long tl = 0L;

        while (millis > 0) {
            long startIteration = System.currentTimeMillis();

            if (trajectoryLength > 0 && tl % trajectoryLength == 0)
                tl = 0L;
            if (tl == 0L) {
                // reset memory
                this.training = null;
                this.labels = null;
                // start new trajectory
                state = randomState();
            }
            // choose random action to evaluate
            Action action = randomASP.selectAction(state);

            state = tbviIteration(state, action, batchSize);

            tl++;

            millis -= (System.currentTimeMillis() - startIteration);
        }
    }

    private State tbviIteration(State s, Action a, int batchSize) {
        double oldQ = getQ(s, a).getDouble(0);
        double r = evaluateReward(s, a);
        State pds = StateUtils.computePostDecisionState(s, a, this);
        double q = getQ(pds, getActionSelectionPolicy().selectAction(pds)).getDouble(0);
        double delta = (r + getGamma() * q) - oldQ;
        //System.out.println(delta);

        INDArray trainingInput = buildInput(StateUtils.computePostDecisionState(s, a, this));
        INDArray label = Nd4j.create(1).put(0, 0, r + (getGamma() * q));

        if (this.training == null && this.labels == null) {
            this.training = trainingInput;
            this.labels = label;
        } else {
            training = Nd4j.concat(0, training, trainingInput);
            labels = Nd4j.concat(0, labels, label);
        }
        DataSet memory = new DataSet(this.training, this.labels);
        memory.shuffle();
        List<DataSet> batches = memory.batchBy(batchSize);
        this.policy.fit(batches.get(0));

        return sampleNextState(s, a);
    }

    private State randomState() {

        int randomIndex = rng.nextInt(this.statesCount);
        StateIterator stateIterator = new StateIterator(getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), getInputRateLevels());

        State s = null;
        while (stateIterator.hasNext()) {
            s = stateIterator.next();
            if (s.getIndex() == randomIndex)
                return s;
        }
        return s;
    }

    private int computeStatesCount() {
        StateIterator stateIterator = new StateIterator(getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), getInputRateLevels());

        int count = 0;
        while (stateIterator.hasNext()) {
            stateIterator.next();
            count++;
        }
        return count;
    }

    private int computeActionsCount() {
        ActionIterator actionIterator = new ActionIterator();

        int count = 0;
        while (actionIterator.hasNext()) {
            actionIterator.next();
            count++;
        }
        return count;
    }

    private State sampleNextState(State s, Action a) {
        State pds = StateUtils.computePostDecisionState(s, a, this);
        List<Double> pArray = new ArrayList<>();
        for (int l : this.getpMatrix().getColLabels(s.getLambda())) {
            int times = (int) Math.floor(100 * this.getpMatrix().getValue(s.getLambda(), l));
            times = times > 0 ? times : 1;
            for (int i = 0; i < times; i++) {
                pArray.add((double) l);
            }
        }
        double[] lambdas = new double[pArray.size()];
        for (int i = 0; i < lambdas.length; i++) {
            lambdas[i] = pArray.get(i);
        }
        INDArray lambdaArray = Nd4j.create(lambdas);
        Nd4j.shuffle(lambdaArray, 0);
        int nextLambda = lambdaArray.getInt(new Random().nextInt(lambdaArray.length()));
        return StateFactory.createState(getStateRepresentation(), -1, pds.getActualDeployment(), nextLambda,
                getInputRateLevels() - 1, this.operator.getMaxParallelism());
    }

    private double evaluateReward(State s, Action a) {
        double cost = 0.0;
        // compute reconfiguration cost
        if (a.getDelta() != 0)
            cost += this.getwReconf();
        // from s,a compute pds
        State pds = StateUtils.computePostDecisionState(s, a, this);
        // for each lambda level with p != 0 in s.getLambda() row
        Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
        for (int lambda : possibleLambdas) {
            // get transition probability from s.lambda to lambda level
            double p = this.getpMatrix().getValue(s.getLambda(), lambda);
            // compute slo violation and deployment cost from post decision operator view
            // recover input rate value from lambda level getting middle value of relative interval
            double pdCost = StateUtils.computePostDecisionCost(pds.getActualDeployment(),
                    MathUtils.remapDiscretizedValue(this.getMaxInputRate(), lambda, this.getInputRateLevels()), this);

            cost += p * pdCost;
        }

        return cost;
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
    public boolean validateAction(State s, Action a) {
        return s.validateAction(a);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        INDArray networkOutput = getQ(s, a);
        return networkOutput.getDouble(0);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    @Override
    protected void buildPolicy() {
        this.inputLayerNodesNumber = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(),
                this.getInputRateLevels()).next().getArrayRepresentationLength();

        this.outputLayerNodesNumber = 1;

        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        new DenseLayer.Builder()
                                .nIn(this.inputLayerNodesNumber)
                                .nOut(32)
                                .activation(Activation.RELU)
                                .build(),
                        new DenseLayer.Builder()
                                .nIn(32)
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
    protected void dumpPolicyOnFile(String filename) {
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
}
