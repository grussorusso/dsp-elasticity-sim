package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ExperienceReplay;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.Transition;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.HashCache;
import org.apache.commons.lang3.tuple.Pair;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Iterator;

public abstract class DeepLearningOM extends ReinforcementLearningOM {
    protected int stateFeatures;
    protected int numActions;

    protected int inputLayerNodesNumber;
    protected int outputLayerNodesNumber;

    protected MultiLayerConfiguration networkConf;
    protected MultiLayerNetwork network;

    protected double gamma;
    protected int fitNetworkEvery;

    protected HashCache<State, Object> networkCache = null;

    protected ExperienceReplay expReplay;
    private int batchSize;



    private int iterations = 0;

    public DeepLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // memory size
        this.batchSize = configuration.getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_BATCH_KEY,32);
        int memorySize = configuration.getInteger(ConfigurationKeys.DL_OM_SAMPLES_MEMORY_SIZE_KEY, 512);
        this.expReplay = new ExperienceReplay(memorySize);

        this.fitNetworkEvery = configuration.getInteger(ConfigurationKeys.DL_OM_FIT_EVERY_ITERS, 5);

        // gamma initial value
        this.gamma = configuration.getDouble(ConfigurationKeys.DP_GAMMA_KEY, 0.99);

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

        int cacheSize = configuration.getInteger(ConfigurationKeys.DL_OM_NETWORK_CACHE_SIZE, 0);
        if (cacheSize > 0) {
            this.networkCache = new HashCache<>(cacheSize);
        }

        dumpPolicyOnFile(String.format("%s/%s/%s/policy",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
                "others"));

        if (configuration.getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }
    }

    protected boolean hasNetworkCache()
    {
        return this.networkCache != null;
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
        //System.out.println(count);
        return count;
    }

    protected void learn () {
        ++iterations;
        if (fitNetworkEvery > 1 && iterations % fitNetworkEvery != 0)
            return;

        Collection<Transition> batch = expReplay.sampleBatch(this.batchSize);
        if (batch != null) {
            Pair<INDArray, INDArray> targets = getTargets(batch);
            this.network.fit(targets.getLeft(), targets.getRight());

            if (hasNetworkCache())
                this.networkCache.clear();
        }
    }

    protected abstract Pair<INDArray, INDArray> getTargets(Collection<Transition> batch);

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

}
