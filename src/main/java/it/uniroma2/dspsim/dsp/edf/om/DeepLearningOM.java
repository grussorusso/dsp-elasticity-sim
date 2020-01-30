package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.parameter.VariableParameter;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;
import org.deeplearning4j.datasets.iterator.ExistingDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.ExistingMiniBatchDataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.INDArrayIndex;
import org.nd4j.linalg.indexing.NDArrayIndex;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;

public abstract class DeepLearningOM extends ReinforcementLearningOM {
    protected int stateFeatures;
    protected int numActions;

    protected int inputLayerNodesNumber;
    protected int outputLayerNodesNumber;

    protected MultiLayerConfiguration networkConf;
    protected MultiLayerNetwork network;

    protected VariableParameter gamma;
    protected int gammaDecaySteps;
    protected int gammaDecayStepsCounter;

    private INDArray training = null;
    private INDArray labels = null;

    // max memory
    private int memorySize;
    // memory batch used to training
    private int memoryBatch;

    public DeepLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // memory size
        this.memorySize = configuration.getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_SIZE_KEY, 512);
        // memory batch
        this.memoryBatch = configuration.getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_BATCH_KEY, 16);

        // gamma initial value
        double gammaInitValue = configuration.getDouble(ConfigurationKeys.DL_OM_GAMMA_KEY, 0.99);
        // gamma decay
        double gammaDecay = configuration.getDouble(ConfigurationKeys.DL_OM_GAMMA_DECAY_KEY, 0.9);
        // gamma min value
        double gammaMinValue = configuration.getDouble(ConfigurationKeys.DL_OM_GAMMA_MIN_VALUE_KEY, 0.01);

        this.gamma = new VariableParameter(gammaInitValue, gammaMinValue, 1.0, gammaDecay);

        // gamma decay steps
        this.gammaDecaySteps = configuration.getInteger(ConfigurationKeys.DL_OM_GAMMA_DECAY_STEPS_KEY, -1);
        // gamma decay steps counter (init)
        this.gammaDecayStepsCounter = 0;

        this.stateFeatures = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(),
                this.getInputRateLevels()).next().getArrayRepresentationLength();
        this.numActions = this.getTotalActions();

        // input and output layer nodes number
        this.inputLayerNodesNumber = computeInputLayerNodesNumber();
        this.outputLayerNodesNumber = computeOutputLayerNodesNumber();

        this.networkConf = buildNeuralNetwork();

        this.network = new MultiLayerNetwork(this.networkConf);
        this.network.init();

        dumpPolicyOnFile(String.format("%s/%s/%s/policy",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
                "others"));

        if (configuration.getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
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

    protected void decrementGamma() {
        if (this.gammaDecaySteps > 0) {
            this.gammaDecayStepsCounter++;
            if (this.gammaDecayStepsCounter >= this.gammaDecaySteps) {
                this.gamma.update();
                this.gammaDecayStepsCounter = 0;
            }
        }
    }

    protected void learn(INDArray input, INDArray label) {
        if (this.training == null) {
            this.training = input;
            this.labels = label;
        } else {
            if (this.training.length() >= this.memorySize) {
                //drop first memory element
                this.training = this.training.get(NDArrayIndex.interval(1, this.training.length()));
                this.labels = this.labels.get(NDArrayIndex.interval(1, this.labels.length()));
            }
            // add new element to memory
            this.training = Nd4j.concat(0, this.training, input);
            this.labels = Nd4j.concat(0, this.labels, label);
        }

        //if (this.training.length() >= this.memoryBatch) {
            // train network
            DataSet tempMemory = new DataSet(this.training, this.labels);
            tempMemory.shuffle();
            List<DataSet> batches = tempMemory.batchBy(this.memoryBatch);
            ListDataSetIterator iterator = new ListDataSetIterator<>(batches);
            this.network.fit(iterator);
        //}
    }

    private void dumpPolicyOnFile(String filename) {
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

    /**
     * ABSTRACT METHODS
     */

    protected abstract int computeOutputLayerNodesNumber();

    protected abstract int computeInputLayerNodesNumber();

    protected abstract MultiLayerConfiguration buildNeuralNetwork();

    /**
     * GETTER
     */

    public int getMemorySize() {
        return memorySize;
    }

    public int getMemoryBatch() {
        return memoryBatch;
    }
}
