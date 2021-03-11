package it.uniroma2.dspsim.dsp.edf.om.rl.states;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

public class NeuralStateRepresentation {

	protected boolean oneHotForLambda;
	protected boolean reducedDeploymentRepresentation;
	protected boolean useResourceSetInReducedRepr;

	private int representationLength;
	private int maxParallelism;
	private int lambdaLevels;

	public NeuralStateRepresentation(int maxParallelism, int lambdaLevels) {
		this.maxParallelism = maxParallelism;
		this.lambdaLevels = lambdaLevels;

		Configuration conf = Configuration.getInstance();
		this.oneHotForLambda = conf.getBoolean(ConfigurationKeys.DL_OM_NETWORK_LAMBDA_ONE_HOT, true);
		this.reducedDeploymentRepresentation = conf.getBoolean(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPR, true);
		this.useResourceSetInReducedRepr = conf.getBoolean(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPT_USE_RESOURCE_SET, true);

		this.representationLength = computeArrayRepresentationLength();
	}

	private int getTotalStates() {
		StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, this.maxParallelism,
				ComputingInfrastructure.getInfrastructure(), lambdaLevels);
		int count = 0;
		while (stateIterator.hasNext()) {
			stateIterator.next();
			count++;
		}
		return count;
	}

	public int getRepresentationLength()
	{
		return this.representationLength;
	}

	public int computeArrayRepresentationLength() {
		int features;

		if (reducedDeploymentRepresentation) {
			final int nodeTypes = ComputingInfrastructure.getInfrastructure().getNodeTypes().length;

			features = maxParallelism * nodeTypes;

			if (useResourceSetInReducedRepr)
				features += Math.pow(2, nodeTypes)-1;
			if (oneHotForLambda) {
				features += lambdaLevels;
			} else {
				features += 1;
			}
		} else {
			if (oneHotForLambda) {
				features = (this.getTotalStates() / lambdaLevels) + lambdaLevels;
			} else {
				features = (this.getTotalStates() / lambdaLevels) + 1;
			}
		}

		return features;
	}
}
