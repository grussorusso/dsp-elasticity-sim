package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;

import java.util.Iterator;

public abstract class DeepLearningOM extends ReinforcementLearningOM {
    protected int stateFeatures;
    protected int numActions;

    protected int inputLayerNodesNumber;
    protected int outputLayerNodesNumber;

    protected MultiLayerConfiguration networkConf;
    protected MultiLayerNetwork network;

    protected double gamma;
    protected double gammaDecay;
    protected int gammaDecaySteps;
    protected int gammaDecayStepsCounter;

    public DeepLearningOM(Operator operator) {
        super(operator);

        // get configuration instance
        Configuration configuration = Configuration.getInstance();

        // gamma
        this.gamma = configuration.getDouble(ConfigurationKeys.DL_OM_GAMMA_KEY, 0.9);
        // gamma decay
        this.gammaDecay = configuration.getDouble(ConfigurationKeys.DL_OM_GAMMA_DECAY_KEY, 0.9);
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

        this. networkConf = buildNeuralNetwork();

        this.network = new MultiLayerNetwork(this.networkConf);
        this.network.init();

        printNetwork();

        if (configuration.getBoolean(ConfigurationKeys.DL_OM_ENABLE_NETWORK_UI_KEY, false)) {
            startNetworkUIServer();
        }
    }

    private void printNetwork() {
        System.out.println(this.network.getLayerWiseConfigurations().toJson());
        System.out.println(this.network.getLayerWiseConfigurations().toYaml());
    }

    private void startNetworkUIServer() {
        // http://localhost:9000/train
        UIServer uiServer = UIServer.getInstance();
        StatsStorage statsStorage = new InMemoryStatsStorage();
        uiServer.attach(statsStorage);
        this.network.setListeners(new StatsListener(statsStorage));
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

    protected State getIndexedState(State state) {
        StateIterator stateIterator = new StateIterator(this.getStateRepresentation(),
                this.operator.getMaxParallelism(), ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        while (stateIterator.hasNext()) {
            State indexedNewState = stateIterator.next();
            if (state.equals(indexedNewState)) {
                return indexedNewState;
            }
        }

        throw new RuntimeException("No possible state generated");
    }

    protected void decrementGamma() {
        if (this.gammaDecaySteps > 0) {
            this.gammaDecayStepsCounter++;
            if (this.gammaDecayStepsCounter >= this.gammaDecaySteps) {
                this.gammaDecayStepsCounter = 0;
                this.gamma = this.gammaDecay * this.gamma;
            }
        }
    }

    /**
     * ABSTRACT METHODS
     */

    protected abstract int computeOutputLayerNodesNumber();

    protected abstract int computeInputLayerNodesNumber();

    protected abstract MultiLayerConfiguration buildNeuralNetwork();
}
