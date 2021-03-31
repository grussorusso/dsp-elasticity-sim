package it.uniroma2.dspsim;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.EDF;
import it.uniroma2.dspsim.dsp.edf.MonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.*;
import it.uniroma2.dspsim.stats.metrics.*;
import it.uniroma2.dspsim.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Map;

public class Simulation {

	/** Simulated time */
	private long time = 0l;

	private final double LATENCY_SLO;

	/* Statistics */
	private Metric metricViolations;
	private Metric metricReconfigurations;
	private Metric metricResCost;
	private Metric metricAvgCost;
	private Metric[] metricDeployedInstances;

	private InputRateFileReader inputRateFileReader;
	private Application app;

	private Logger logger = LoggerFactory.getLogger(Simulation.class);

	private double wSLO;
	private double wReconf;
	private double wRes;

	private boolean detailedScalingLog;

	public Simulation (InputRateFileReader inputRateFileReader, Application application) {
		this.inputRateFileReader = inputRateFileReader;
		this.app = application;

		Configuration conf = Configuration.getInstance();
		this.LATENCY_SLO = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);
		this.wSLO = conf.getDouble(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY, 0.33);
		this.wReconf = conf.getDouble(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY, 0.33);
		this.wRes = conf.getDouble(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY, 0.33);

		this.detailedScalingLog = conf.getBoolean(ConfigurationKeys.SIMULATION_DETAILED_SCALING_LOG, false);

		logger.info("SLO latency: {}", LATENCY_SLO);
	}

	private void registerMetrics () {
		Statistics statistics = Statistics.getInstance();

		final String STAT_LATENCY_VIOLATIONS = "Violations";
		final String STAT_RECONFIGURATIONS = "Reconfigurations";
		final String STAT_RESOURCES_COST = "ResourcesCost";
		final String STAT_APPLICATION_COST_AVG = "AvgCost";

		this.metricViolations = new CountMetric(STAT_LATENCY_VIOLATIONS);
		statistics.registerMetric(metricViolations);

		Configuration conf = Configuration.getInstance();
		boolean sampleCostValues = conf.getBoolean(ConfigurationKeys.STATS_SAMPLE_COST_VALUES, false);
		boolean sampleAvgCost = conf.getBoolean(ConfigurationKeys.STATS_SAMPLE_AVG_COST_VALUES, true);

		this.metricAvgCost = new RealValuedMetric(STAT_APPLICATION_COST_AVG, sampleCostValues, sampleAvgCost);
		statistics.registerMetric(metricAvgCost);

		this.metricReconfigurations = new CountMetric(STAT_RECONFIGURATIONS);
		statistics.registerMetric(metricReconfigurations);

		this.metricResCost = new RealValuedMetric(STAT_RESOURCES_COST);
		statistics.registerMetric(metricResCost);

		this.metricDeployedInstances = new RealValuedMetric[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
		for (int i = 0; i < ComputingInfrastructure.getInfrastructure().getNodeTypes().length; i++) {
			this.metricDeployedInstances[i]	 = new RealValuedMetric("InstancesType" + i);
			statistics.registerMetric(this.metricDeployedInstances[i]);
		}
	}

	public void run (long stopTime) throws IOException {
		registerMetrics();

		logger.warn("Starting simulation");

		EDF edf = new EDF(app, LATENCY_SLO);
		MonitoringInfo monitoringInfo = new MonitoringInfo();

		while (inputRateFileReader.hasNext() && (stopTime <= 0 || time <= stopTime)) {
			double inputRate = inputRateFileReader.next();

			// compute application cost in this iteration
			double iterationCost = 0.0;

			double responseTime = app.endToEndLatency(inputRate);
			if (detailedScalingLog) {
				logger.info("[T={}] AppInputRate: {}", time, inputRate);
				logger.info("[T={}] AppResponseTime: {}", time, responseTime);
			}
			if (responseTime > LATENCY_SLO) {
				metricViolations.update(1);

				// add slo violation cost
				iterationCost += this.wSLO;
				logger.info("SloCost={}",1.0);

				if (detailedScalingLog)
					logger.info("[T={}] AppSLOViolation", time);
			} else {
				logger.info("SloCost={}",0.0);
			}

			final double deploymentCost = app.computeDeploymentCost();
			metricResCost.update(deploymentCost);

			// add deployment cost normalized
			final double cRes = (deploymentCost / app.computeMaxDeploymentCost());
			iterationCost += cRes * this.wRes;

			/* Reconfiguration */
			monitoringInfo.setInputRate(inputRate);
			Map<Operator, Reconfiguration> reconfigurations = edf.pickReconfigurations(monitoringInfo);
			applyReconfigurations(reconfigurations);

			// add reconfiguration cost if there is a configuration in this iteration
			boolean isAppReconfigured = checkReconfigurationPresence(reconfigurations);
			iterationCost += isAppReconfigured ? this.wReconf : 0.0;

			if (detailedScalingLog && isAppReconfigured)
				logger.info("[T={}] AppReconfiguration", time);

			// update application avg cost
			metricAvgCost.update(iterationCost);

			// update avg deployment
			int[] globalDeployment = app.computeGlobalDeployment();
			for (int i = 0; i < globalDeployment.length; i++) {
				metricDeployedInstances[i].update(globalDeployment[i]);
			}

			if (detailedScalingLog) {
				for (Operator op : app.getOperators()) {
					int deployment[] = op.getCurrentDeployment();
					logger.info("[T={}] OpDeployment of {}: {}", time, op.getName(), Arrays.toString(deployment));
				}
			}

			if (time % 10000 == 0)
				System.out.print('.');
			if (time > 0 && time % 100000 == 0)
				System.out.print('|');

			time++;
		}
		System.out.print('\n');

		edf.dumpPolicies();
	}

	private boolean checkReconfigurationPresence(Map<Operator, Reconfiguration> reconfigurations) {
		for (Reconfiguration reconfiguration : reconfigurations.values()) {
			if (reconfiguration.isReconfiguration()) {
				return true;
			}
		}
		return false;
	}

	private boolean applyReconfigurations(Map<Operator, Reconfiguration> reconfigurations) {
		// TODO we assume to have unlimited resources
		boolean appReconfigured = false;

		for (Operator op : reconfigurations.keySet()) {
			Reconfiguration rcf = reconfigurations.get(op);
			if (!rcf.isReconfiguration())
				continue;

			if (detailedScalingLog) {
				logger.info("[T={}] OpReconfiguration of {}: {}", time, op.getName(), rcf);
			}

			op.reconfigure(rcf);
			appReconfigured = true;
		}

		if (appReconfigured)
			metricReconfigurations.update(1);

		return appReconfigured;
	}

	private void dumpConfigs() {
		File configCopy = new File(String.format("%s/configs",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, "")));
		if (!configCopy.getParentFile().exists()) {
			configCopy.getParentFile().mkdirs();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(configCopy);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Configuration.getInstance().dump(fos);
	}

	private void dumpStats() {
		/** Add dummy metric for time */
		Statistics statistics = Statistics.getInstance();
		CountMetric timeMetric = new CountMetric("SimulationTime");
		statistics.registerMetricIfNotExists(timeMetric);
		timeMetric.update((int)this.time);


		File statsOutput = new File(String.format("%s/final_stats",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, "")));
		if (!statsOutput.getParentFile().exists()) {
			statsOutput.getParentFile().mkdirs();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(statsOutput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		statistics.dumpAll(fos);


		// TODO: make this optional
		statistics.dumpSorted();
	}

	public static void main (String[] args) {

		Configuration conf = Configuration.getInstance();
		conf.parseDefaultConfigurationFile();
		if (args.length > 0) {
			for (String arg : args) {
				try {
					InputStream is = new FileInputStream(new File(arg));
					conf.parseConfigurationFile(is);
					is.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					System.exit(1);
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(1);
				}
			}
		}

		LoggingUtils.configureLogging();
		ComputingInfrastructure.initDefaultInfrastructure();

		Simulation simulation = null;

		try {
			final String inputFile = conf
					.getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, "/home/gabriele/profile.dat");
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Application app = ApplicationBuilder.buildApplication();
			simulation = new Simulation(inputRateFileReader, app);

			long stopTime = conf.getLong(ConfigurationKeys.SIMULATION_STOP_TIME, -1l);
			simulation.run(stopTime);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}


		try {
			/* Dump used configuration in output folder. */
			simulation.dumpConfigs();

			/* Dump statistics in output folder. */
			simulation.dumpStats();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
