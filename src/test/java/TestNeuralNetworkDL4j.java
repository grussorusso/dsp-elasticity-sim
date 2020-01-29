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
                                .activation(Activation.SOFTMAX)
                                .nIn(32)
                                .nOut(7)
                                .build()
                )
                //.backprop(true)
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
