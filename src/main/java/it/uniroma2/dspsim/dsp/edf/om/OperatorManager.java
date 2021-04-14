package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.request.OMRequest;
import it.uniroma2.dspsim.dsp.queueing.OperatorQueueModel;
import org.nd4j.linalg.api.ops.Op;
import org.slf4j.LoggerFactory;

import java.util.Random;

public abstract class OperatorManager {

	protected Operator operator;

	public OperatorManager (Operator operator)  {
		this.operator = operator;
	}


	abstract public OMRequest pickReconfigurationRequest(OMMonitoringInfo monitoringInfo);

	public Operator getOperator() {
		return operator;
	}

	public void savePolicy()
	{

	}

	private Operator approximateOperator = null;

	private Operator approximateOperatorModel (Configuration conf)
	{
		Random r = new Random(conf.getInteger(ConfigurationKeys.VI_APPROX_MODEL_SEED, 123));
		final double maxErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MAX_ERR, 0.1);
		final double minErr = conf.getDouble(ConfigurationKeys.VI_APPROX_MODEL_MIN_ERR, 0.05);

		OperatorQueueModel queueModel = operator.getQueueModel().getApproximateModel(r, maxErr, minErr);
		LoggerFactory.getLogger(OperatorManager.class).info("Approximate stMean: {} -> {}", operator.getQueueModel().getServiceTimeMean(), queueModel.getServiceTimeMean());
		Operator tempOperator = new Operator("temp", queueModel, operator.getMaxParallelism());
		tempOperator.setSloRespTime(operator.getSloRespTime());
		return tempOperator;
	}

	public Operator getApproximateOperator() {
		if (this.approximateOperator == null)
			this.approximateOperator = approximateOperatorModel(Configuration.getInstance());
		return this.approximateOperator;
	}
}
