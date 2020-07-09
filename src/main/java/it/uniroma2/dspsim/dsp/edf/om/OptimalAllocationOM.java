package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.InputRateFileReader;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.request.BasicOMRequest;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;

import java.io.IOException;

/**
 * At every time slot, picks the minimum parallelism needed to satisfy input rate in the next slot,
 * which is assumed to be perfectly predicted.
 *
 * It is greedy as it does not consider reconfiguration cost.
 */
public class OptimalAllocationOM extends OperatorManager {

	private InputRateFileReader inputRateFileReader;

	public OptimalAllocationOM(Operator operator) {
		super(operator);

		if (ComputingInfrastructure.getInfrastructure().getNodeTypes().length != 1) {
			throw new RuntimeException("OptimalAllocationOM currently does not support more than 1 resource type");
		}

		initInputRatePredictor();
	}

	@Override
	public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo) {

		double nextInputRate = this.predictInputRate();
		int newParallelism = computeNewParallelism(nextInputRate);
		int currentParallelism = operator.getInstances().size();

		Reconfiguration rcf;
		if (newParallelism == currentParallelism) {
			rcf = Reconfiguration.doNothing();
		} else if (newParallelism > currentParallelism) {
			NodeType[] toAdd = new NodeType[newParallelism-currentParallelism];
			for (int i = 0; i < toAdd.length; i++)
				toAdd[i] = operator.getInstances().get(0);
			rcf = Reconfiguration.scaleOut(toAdd);
		} else {
			NodeType[] toRemove = new NodeType[currentParallelism-newParallelism];
			for (int i = 0; i < toRemove.length; i++)
				toRemove[i] = operator.getInstances().get(0);
			rcf = Reconfiguration.scaleIn(toRemove);
		}

		return new BasicOMRequest(rcf);
	}

	private int computeNewParallelism(double nextInputRate) {
		final double speedup = operator.getInstances().get(0).getCpuSpeedup();

		int newParallelism = 1;
		double r = operator.getQueueModel().responseTime(nextInputRate, speedup);

		while (newParallelism <= operator.getMaxParallelism() && r > operator.getSloRespTime()) {
			newParallelism++;
			r = operator.getQueueModel().responseTime(nextInputRate/newParallelism, speedup);
		}

		return newParallelism;
	}


	private void initInputRatePredictor() {
		Configuration conf = Configuration.getInstance();
		String inputRateFile = conf.getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, null);

		try {
			this.inputRateFileReader = new InputRateFileReader(inputRateFile);
			this.inputRateFileReader.next(); // skip first time slot
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}


	private double predictInputRate() {
		try {
			return this.inputRateFileReader.next();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
