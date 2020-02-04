package it.uniroma2.dspsim;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.EDF;
import it.uniroma2.dspsim.dsp.edf.MonitoringInfo;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.*;
import it.uniroma2.dspsim.stats.metrics.*;
import it.uniroma2.dspsim.stats.samplers.StepSampler;
import it.uniroma2.dspsim.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Map;

public class Simulation {

	static {
	}

	/** Simulated time */
	private long time = 0l;

	private final double LATENCY_SLO;

	/* Statistics */
	private static final String SIMULATION_STATS_NAME_PREFIX = "Simulation - ";

	private static final String STAT_STEPS_COUNTER = "Steps Counter";
	private static final String STAT_LATENCY_VIOLATIONS = "Latency Violations";
	private static final String STAT_VIOLATIONS_PERCENTAGE = "Latency Violations Percentage";
	private static final String STAT_RECONFIGURATIONS = "Reconfigurations";
	private static final String STAT_RECONFIGURATIONS_PERCENTAGE = "Reconfigurations Percentage";
	private static final String STAT_RESOURCES_COST_USED_SUM = "Resources Cost Used Sum";
	private static final String STAT_RESOURCES_COST_MAX_SUM = "Resources Cost Max Sum";
	private static final String STAT_RESOURCES_COST_PERCENTAGE = "Resources Cost Percentage";
	private static final String STAT_RESOURCES_COST_AVG = "Resources Cost Avg";
	private static final String STAT_APPLICATION_COST_AVG = "Application Avg Cost";

	private static final String STAT_SIMULATION_TIME = "Simulation Time";
	private static final String STAT_SIMULATION_MEMORY = "Simulation Memory";

	protected static final String STAT_AVG_DEPLOYMENT = "Avg Deployment in resources of type - ";

	private static final String STEP_SAMPLER_ID = "Step-sampler";

	private InputRateFileReader inputRateFileReader;
	private Application app;

	private Logger logger = LoggerFactory.getLogger(Simulation.class);

	private double wSLO;
	private double wReconf;
	private double wRes;

	public Simulation (InputRateFileReader inputRateFileReader, Application application) {
		this.inputRateFileReader = inputRateFileReader;
		this.app = application;

		Configuration conf = Configuration.getInstance();
		this.LATENCY_SLO = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);
		this.wSLO = conf.getDouble(ConfigurationKeys.RL_OM_SLO_WEIGHT_KEY, 0.33);
		this.wReconf = conf.getDouble(ConfigurationKeys.RL_OM_RECONFIG_WEIGHT_KEY, 0.33);
		this.wRes = conf.getDouble(ConfigurationKeys.RL_OM_RESOURCES_WEIGHT_KEY, 0.33);

		logger.info("SLO latency: {}", LATENCY_SLO);
	}

	public void run() throws IOException {
		run(-1l);
	}

	private void registerMetrics () {
		Statistics statistics = Statistics.getInstance();

		//SIMPLE METRICS
		// steps counter metric
		statistics.registerMetric(new CountMetric(buildMetricName(STAT_STEPS_COUNTER)));
		// SLO violations counter
		statistics.registerMetric(new CountMetric(buildMetricName(STAT_LATENCY_VIOLATIONS)));
		// reconfigurations counter
		statistics.registerMetric(new CountMetric(buildMetricName(STAT_RECONFIGURATIONS)));
		// resources cost used sum
		statistics.registerMetric(new RealValuedCountMetric(buildMetricName(STAT_RESOURCES_COST_USED_SUM)));
		// max possible resources sum
		statistics.registerMetric(new RealValuedCountMetric(buildMetricName(STAT_RESOURCES_COST_MAX_SUM)));

		// COMPOSED METRICS
		// SLO violations percentage
		statistics.registerMetric(new PercentageMetric(buildMetricName(STAT_VIOLATIONS_PERCENTAGE),
				statistics.getMetric(buildMetricName(STAT_LATENCY_VIOLATIONS)), statistics.getMetric(buildMetricName(STAT_STEPS_COUNTER))));
		// reconfigurations percentage
		statistics.registerMetric(new PercentageMetric(buildMetricName(STAT_RECONFIGURATIONS_PERCENTAGE),
				statistics.getMetric(buildMetricName(STAT_RECONFIGURATIONS)), statistics.getMetric(buildMetricName(STAT_STEPS_COUNTER))));
		// resources cost percentage
		statistics.registerMetric(new PercentageMetric(buildMetricName(STAT_RESOURCES_COST_PERCENTAGE),
				statistics.getMetric(buildMetricName(STAT_RESOURCES_COST_USED_SUM)), statistics.getMetric(buildMetricName(STAT_RESOURCES_COST_MAX_SUM))));
		// resources cost avg
		statistics.registerMetric(new IncrementalAvgMetric(buildMetricName(STAT_RESOURCES_COST_AVG)));

		// incremental avg reward
		IncrementalAvgMetric incrementalAvgMetric = new IncrementalAvgMetric(buildMetricName(STAT_APPLICATION_COST_AVG));
		StepSampler stepSampler = new StepSampler(STEP_SAMPLER_ID, 1);
		incrementalAvgMetric.addSampler(stepSampler);
		statistics.registerMetric(incrementalAvgMetric);

		// avg deployment
		for (int i = 0; i < ComputingInfrastructure.getInfrastructure().getNodeTypes().length; i++) {
			statistics.registerMetricIfNotExists(new IncrementalAvgMetric(buildMetricName(STAT_AVG_DEPLOYMENT + i)));
		}
	}

	private String buildMetricName(String name) {
		return this.SIMULATION_STATS_NAME_PREFIX + name;
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
			logger.info("Input Rate: {}", inputRate);
			logger.info("Response time: {}", responseTime);
			if (responseTime > LATENCY_SLO) {
				Statistics.getInstance().updateMetric(buildMetricName(STAT_LATENCY_VIOLATIONS), 1);

				// add slo violation cost
				iterationCost += this.wSLO;
			}

			// update used resources cost metric
			Statistics.getInstance().updateMetric(buildMetricName(STAT_RESOURCES_COST_USED_SUM), app.computeDeploymentCost());
			// update max resources cost metric
			Statistics.getInstance().updateMetric(buildMetricName(STAT_RESOURCES_COST_MAX_SUM), app.computeMaxDeploymentCost());

			// add deployment cost normalized
			iterationCost += (app.computeDeploymentCost() / app.computeMaxDeploymentCost()) * this.wRes;

			/* Reconfiguration */
			monitoringInfo.setInputRate(inputRate);
			Map<Operator, Reconfiguration> reconfigurations = edf.pickReconfigurations(monitoringInfo);
			applyReconfigurations(reconfigurations);

			// add reconfiguration cost if there is a configuration in this iteration
			iterationCost += checkReconfigurationPresence(reconfigurations) ? this.wReconf : 0.0;

			//System.out.println(inputRate + "\t" + responseTime);

			// update steps counter
			Statistics.getInstance().updateMetric(buildMetricName(STAT_STEPS_COUNTER), 1);

			// update application avg cost
			Statistics.getInstance().updateMetric(buildMetricName(STAT_APPLICATION_COST_AVG), iterationCost);

			// update avg deployment
			int[] globalDeployment = app.computeGlobalDeployment();
			for (int i = 0; i < globalDeployment.length; i++) {
				Statistics.getInstance().updateMetric(buildMetricName(STAT_AVG_DEPLOYMENT + i), globalDeployment[i]);
			}

			// metrics sampling
			Statistics.getInstance().sampleAll(time);

			if (time % 5000 == 0)
				// TODO print simulation completion percentage
				System.out.println(time);

			time++;
		}
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

			logger.info("{} reconfiguration: {}", op.getName(), rcf);

			op.reconfigure(rcf);
			appReconfigured = true;
		}

		if (appReconfigured)
			Statistics.getInstance().updateMetric(buildMetricName(STAT_RECONFIGURATIONS), 1);

		return appReconfigured;
	}

	private void dumpConfigs() {
		File configCopy = new File(String.format("%s/%s/others/configs",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
				Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, "")));
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
		File statsOutput = new File(String.format("%s/%s/others/final_stats",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
				Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, "")));
		if (!statsOutput.getParentFile().exists()) {
			statsOutput.getParentFile().mkdirs();
		}
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(statsOutput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		Statistics.getInstance().dumpAll(fos);


		// TODO: make this optional
		Statistics.getInstance().dumpSorted();
	}

	public static void main (String[] args) {
		LoggingUtils.configureLogging();

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
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		ComputingInfrastructure.initCustomInfrastructure(
				new double[]{1.0, 0.7, 1.3, 0.9, 1.7, 0.8, 1.8, 2.0, 1.65, 1.5},
				conf.getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3));

		try {
			final String inputFile = conf
					.getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, "/home/gabriele/profile.dat");
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Application app = ApplicationBuilder.buildApplication();
			Simulation simulation = new Simulation(inputRateFileReader, app);

			Statistics.getInstance().registerMetric(new TimeMetric(SIMULATION_STATS_NAME_PREFIX + STAT_SIMULATION_TIME));
			Statistics.getInstance().registerMetric(new AvgMemoryMetric(SIMULATION_STATS_NAME_PREFIX + STAT_SIMULATION_MEMORY));
			// run simulation
			simulation.run();

			Statistics.getInstance().updateMetric(SIMULATION_STATS_NAME_PREFIX + STAT_SIMULATION_TIME, 0);
			Statistics.getInstance().updateMetric(SIMULATION_STATS_NAME_PREFIX + STAT_SIMULATION_MEMORY, 0);

			/* Dump used configuration in output folder. */
			simulation.dumpConfigs();

			/* Dump statistics in output folder. */
			simulation.dumpStats();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
