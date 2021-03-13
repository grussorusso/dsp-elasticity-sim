package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.NeuralStateRepresentation;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.HashCache;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;

public class CachedNeuralNetwork extends MultiLayerNetwork {

	private long hits = 0l;

	private HashCache<State, Object> networkCache = null;
	private NeuralStateRepresentation neuralStateRepresentation;

	public CachedNeuralNetwork(MultiLayerConfiguration conf, int cacheSize, NeuralStateRepresentation nnRepr) {
		super(conf);
		if (cacheSize > 0)
			this.networkCache = new HashCache<>(cacheSize);
		this.neuralStateRepresentation = nnRepr;
	}

	public INDArray output(State s) {
		if (networkCache != null && networkCache.containsKey(s)) {
			++hits;
			return (INDArray) networkCache.get(s);
		}

		INDArray input = buildInput(s);
		INDArray output = this.output(input);

		if (networkCache != null)
			networkCache.put(s, output.dup());

		return output;
	}

	private INDArray buildInput(State state) {
		return state.arrayRepresentation(this.neuralStateRepresentation);
	}


	@Override
	public void fit (INDArray input, INDArray labels)
	{
		super.fit(input,labels);
		flush();
	}

	@Override
	public void setParameters (INDArray params) {
		super.setParameters(params);
		flush();
	}

	private void flush()
	{
		if (networkCache != null)
			this.networkCache.clear();
	}

	public long getHits()
	{
		return this.hits;
	}

}
