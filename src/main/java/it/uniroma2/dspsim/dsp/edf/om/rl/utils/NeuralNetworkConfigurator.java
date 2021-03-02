package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.slf4j.LoggerFactory;

public class NeuralNetworkConfigurator {

	private NeuralNetworkConfigurator() {}

	public static MultiLayerConfiguration configure(int inputLayerNodesNumber, int outputLayerNodesNumber)
	{
		Configuration conf = Configuration.getInstance();
		int hiddenNodes1 = (int)Math.round(inputLayerNodesNumber*conf.getDouble(ConfigurationKeys.DL_OM_NETWORK_HIDDEN_NODES1_COEFF,
				2.0));
		int hiddenNodes2 = (int)Math.round(hiddenNodes1*conf.getDouble(ConfigurationKeys.DL_OM_NETWORK_HIDDEN_NODES2_COEFF,
				0.5));

		final double learningRate = conf.getDouble(ConfigurationKeys.DL_OM_NETWORK_ALPHA, 0.1);

		MultiLayerConfiguration netConf;

		if (hiddenNodes2 > 0) {
			netConf = new NeuralNetConfiguration.Builder()
					.seed(conf.getInteger(ConfigurationKeys.DL_OM_ND4j_RANDOM_SEED_KET, 12345))
					.weightInit(WeightInit.XAVIER)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
					.updater(new Sgd(learningRate))
					.list(
							new DenseLayer.Builder()
									.nIn(inputLayerNodesNumber)
									.nOut(hiddenNodes1)
									.activation(Activation.RELU)
									.build(),
							new DenseLayer.Builder()
									.nIn(hiddenNodes1)
									.nOut(hiddenNodes2)
									.activation(Activation.RELU)
									.build(),
							new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
									.nIn(hiddenNodes2)
									.nOut(outputLayerNodesNumber)
									.activation(Activation.IDENTITY)
									.build()
					)
					.pretrain(false)
					.backprop(true)
					.build();
			LoggerFactory.getLogger(NeuralNetworkConfigurator.class).info("NeuralNet: {}-{}-{}-{}",
					inputLayerNodesNumber, hiddenNodes1, hiddenNodes2, outputLayerNodesNumber);
		} else {
			netConf = new NeuralNetConfiguration.Builder()
					.seed(conf.getInteger(ConfigurationKeys.DL_OM_ND4j_RANDOM_SEED_KET, 12345))
					.weightInit(WeightInit.XAVIER)
					.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
					.updater(new Sgd(learningRate))
					.list(
							new DenseLayer.Builder()
									.nIn(inputLayerNodesNumber)
									.nOut(hiddenNodes1)
									.activation(Activation.RELU)
									.build(),
							new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
									.nIn(hiddenNodes1)
									.nOut(outputLayerNodesNumber)
									.activation(Activation.IDENTITY)
									.build()
					)
					.pretrain(false)
					.backprop(true)
					.build();
			LoggerFactory.getLogger(NeuralNetworkConfigurator.class).info("NeuralNet: {}-{}-{}",
					inputLayerNodesNumber, hiddenNodes1, outputLayerNodesNumber);
		}

		return netConf;
	}

}
