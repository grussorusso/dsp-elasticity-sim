package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.NeuralStateRepresentation;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.*;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collection;

public class DeepTBValueIterationOM extends BaseTBValueIterationOM {

    private int inputLayerNodesNumber;
    private int outputLayerNodesNumber;

    private ExperienceReplay expReplay;
    private int batchSize;
    private int fitNetworkEvery;
    protected NeuralStateRepresentation neuralStateRepresentation;

    private Logger log = LoggerFactory.getLogger(DeepTBValueIterationOM.class);

    protected CachedNeuralNetwork network;

    public DeepTBValueIterationOM(Operator operator) {
        super(operator);

        if (Configuration.getInstance().getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }

        this.batchSize = Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_BATCH_KEY,32);
        final int memory = Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_SIZE_KEY, 10000);
        this.expReplay = new ExperienceReplay(memory);

        this.fitNetworkEvery = Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_FIT_EVERY_ITERS, 5);

        if (!PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
            tbvi(this.tbviMaxIterations, this.tbviMillis, this.tbviTrajectoryLength);
        }

        // dumpQOnFile(String.format("%s/%s/%s/policy",
       //         Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
       //         Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
       //         "others"));

        //printTBVIResults();
    }

    private INDArray buildInput(State state) {
        return state.arrayRepresentation(this.neuralStateRepresentation);
    }

    private double getV (State state) {
        return network.output(state).getDouble(0);
    }

    private double getQ(State state, Action action) {
        State postDecisionState = StateUtils.computePostDecisionState(state, action, this);
        final double v = getV(postDecisionState);
        return v + computeActionCost(action);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return getQ(s, a);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    @Override
    protected void buildQ() {
        this.neuralStateRepresentation = new NeuralStateRepresentation(this.operator.getMaxParallelism(),
                this.getInputRateLevels());

        this.inputLayerNodesNumber = this.neuralStateRepresentation.getRepresentationLength();

        this.outputLayerNodesNumber = 1;

        MultiLayerConfiguration config = NeuralNetworkConfigurator.configure(inputLayerNodesNumber,outputLayerNodesNumber);

        final int cacheSize = Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_NETWORK_CACHE_SIZE, 0);
        this.network = new CachedNeuralNetwork(config, cacheSize, neuralStateRepresentation);
        network.init();

        if (PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
            try {
                File f = PolicyIOUtils.getFileForLoading(this.operator, "network");
                FileInputStream fileIn = new FileInputStream(f.getAbsolutePath());
                ObjectInputStream in = new ObjectInputStream(fileIn);
                INDArray params = (INDArray)  in.readObject();
                in.close();
                fileIn.close();
                this.network.setParameters(params);
            } catch (IOException i) {
                i.printStackTrace();
            } catch (ClassNotFoundException c) {
                c.printStackTrace();
            }
        }
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

    @Override
    public void savePolicy()
    {
        try {
            File f = PolicyIOUtils.getFileForDumping(this.operator, "network");
            FileOutputStream fileOut = new FileOutputStream(f.getAbsolutePath());
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this.network.params());
            out.close();
            fileOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
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
        return this.getQ(s, a);
    }

    @Override
    protected void learn(double tbviDelta, double newQ, State state, Action action) {
        // not used
    }

    @Override
    protected State tbviIteration(State s, Action a) {
        Transition t = new Transition(s, a, null, 0.0);
        expReplay.add(t);

        // check if u want to perform an update
        if (fitNetworkEvery <= 1 || tbviIterations % fitNetworkEvery == 0) {
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
