import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.NeuralStateRepresentation;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.NeuralNetworkConfigurator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.deeplearning4j.nn.api.Layer;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.junit.Test;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.activations.impl.ActivationReLU;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TestNeuralNetworkDL4j {

    @Test
    public void neuralNetworkTestDataset1() {
        Nd4j.getRandom().setSeed(12345);

        MultiLayerNetwork network = buildNetwork();

        INDArray label = getLabel(7);

        INDArray dataset = getDataset1(4);

        network.fit(dataset, label);

        for (Layer layer : network.getLayers())
            printWeights(layer, 10, "Dataset 1");
    }

    @Test
    public void neuralNetworkTestDataset2() {
        Nd4j.getRandom().setSeed(12345);

        MultiLayerNetwork network = buildNetwork();

        INDArray label = getLabel(7);

        INDArray dataset = getDataset2(4);

        network.fit(dataset, label);

        for (Layer layer : network.getLayers())
            printWeights(layer, 10, "Dataset 2");
    }

    @Test
    public void neuralNetworkTestDatasetAndCache () {
        Nd4j.getRandom().setSeed(12345);

        MultiLayerNetwork network = buildNetwork();

        INDArray label = getLabel(7);

        INDArray dataset = getDataset1(4);

        network.fit(dataset, label);

        INDArray input = network.getInput();
        INDArray input2 = input.dup();
        input2 = input2.put(0, 0, 5);
        System.out.println(input);
        System.out.println(input2);

        INDArray output = network.output(input);
        INDArray output1 = network.output(input2);
        System.out.println(output.toString());
        System.out.println(output1.toString());

        double arr[] = output.data().asDouble();
        System.out.println(Arrays.toString(arr));

    }

    @Test
    public void testDoubleNetwork () {
        Nd4j.getRandom().setSeed(12345);

        MultiLayerNetwork network = buildNetwork();
        MultiLayerNetwork network2 = network.clone();
        MultiLayerNetwork network3 = buildNetwork();
        network3.setParameters(network.params());

        INDArray label = getLabel(7);

        INDArray dataset = getDataset1(4);

        network.fit(dataset, label);
        INDArray input = network.getInput();
        input = input.put(0, 0, 5);
        System.out.println(input);

        INDArray output = network.output(input);
        INDArray output1 = network2.output(input);
        System.out.println(output.toString());
        System.out.println(output1.toString());

        network2.setParameters(network.params());

        input = network.getInput();
        input = input.put(0, 0, 5);
        System.out.println(input);

        output = network.output(input);
        output1 = network2.output(input);
        System.out.println(output.toString());
        System.out.println(output1.toString());


    }

    @Test
    public void neuralNetworkTestDataset1And2() throws IOException {
        Nd4j.getRandom().setSeed(12345);

        MultiLayerNetwork network = buildNetwork();

        INDArray label = getLabel(7);

        INDArray dataset1 = getDataset1(4);
        INDArray dataset2 = getDataset2(4);

        network.fit(dataset1, label);
        network.fit(dataset2, label);

        for (Layer layer : network.getLayers())
            printWeights(layer, 10, "Dataset 1 and 2");
    }

    @Test
    public void hashingStates() {
        ComputingInfrastructure.initDefaultInfrastructure(5);
        Operator operator = new Operator("rank",
                new MG1OperatorQueueModel(1.0, 0.0), 12);


        final int lambdaLevels = 20;
        ArrayList<State> states = new ArrayList<>();
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), lambdaLevels);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            states.add(state);
        }

        NeuralStateRepresentation repr = new NeuralStateRepresentation(operator.getMaxParallelism(), lambdaLevels);
        ArrayList<INDArray> inputs = new ArrayList<>(states.size());
        for (State s : states) {
            inputs.add(s.arrayRepresentation(repr));
        }

        long t0 = System.currentTimeMillis();
        long result = 123;
        for (State s : states) {
            result = (result + s.hashCode()) % 145;
        }
        System.out.println(System.currentTimeMillis()-t0);

        t0 = System.currentTimeMillis();
        result = 123;
        for (INDArray s : inputs) {
            result = (result + s.hashCode()) % 145;
        }
        System.out.println(System.currentTimeMillis()-t0);
    }


    private void printWeights(Layer layer, int paramsToRead, String printTitle) {
        StringBuilder str = new StringBuilder(String.format("%s\n\nLayer: %d\n", printTitle, layer.getIndex()));

        INDArray params = layer.getParam("W");

        for (int i = 0; i < paramsToRead; i++) {
            str.append(String.format("Param %d: %f\n", i, params.getDouble(i)));
        }
        System.out.println(str);
    }

    private MultiLayerNetwork buildNetwork() {
        MultiLayerConfiguration networkConf = new NeuralNetConfiguration.Builder()
                .seed(Configuration.getInstance().getInteger(ConfigurationKeys.DL_OM_ND4j_RANDOM_SEED_KET, 12345))
                .weightInit(WeightInit.XAVIER)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .updater(new Sgd(0.05))
                .list(
                        new DenseLayer.Builder()
                                .nIn(4)
                                .nOut(32)
                                .activation(new ActivationReLU())
                                .build(),
                        new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                                .activation(Activation.IDENTITY)
                                .nIn(32)
                                .nOut(7)
                                .build()
                )
                .backprop(true)
                .build();

        MultiLayerNetwork network = new MultiLayerNetwork(networkConf);
        network.init();
        return network;
    }

    private INDArray getLabel(int features) {
        return Nd4j.create(features);
    }

    private INDArray getDataset1(int features) {
        INDArray dataset = Nd4j.create(features);
        for (int i = 0; i < features; i++) {
            dataset.put(0, i, i);
        }
        return dataset;
    }

    private  INDArray getDataset2(int features) {
        INDArray dataset = Nd4j.create(features);
        for (int i = 0; i < features; i++) {
            dataset.put(0, i, i + 2);
        }
        return dataset;
    }
}
