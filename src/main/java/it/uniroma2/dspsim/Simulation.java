package it.uniroma2.dspsim;

import it.uniroma2.dspsim.dsp.Application;
import it.uniroma2.dspsim.dsp.ApplicationBuilder;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.ApplicationManager;
import it.uniroma2.dspsim.dsp.edf.EDF;
import it.uniroma2.dspsim.dsp.edf.MonitoringInfo;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.*;
import it.uniroma2.dspsim.stats.metrics.CountMetric;
import it.uniroma2.dspsim.stats.metrics.AvgMetric;
import it.uniroma2.dspsim.stats.metrics.PercentageMetric;
import it.uniroma2.dspsim.stats.metrics.RealValuedCountMetric;
import it.uniroma2.dspsim.stats.samplers.StepSampler;
import it.uniroma2.dspsim.stats.tracker.TrackerManager;
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
	private static final String STAT_STEPS_COUNTER = "Steps Counter";
	private static final String STAT_LATENCY_VIOLATIONS = "Latency Violations";
	private static final String STAT_VIOLATIONS_PERCENTAGE = "Latency Violations Percentage";
	private static final String STAT_RECONFIGURATIONS = "Reconfigurations";
	private static final String STAT_RECONFIGURATIONS_PERCENTAGE = "Reconfigurations Percentage";
	private static final String STAT_RESOURCES_COST_USED_SUM = "Resources Cost Used Sum";
	private static final String STAT_RESOURCES_COST_MAX_SUM = "Resources Cost Max Sum";
	private static final String STAT_RESOURCES_COST_PERCENTAGE = "Resources Cost Percentage";
	private static final String STAT_RESOURCES_COST_AVG = "Resources Cost Avg";

	private InputRateFileReader inputRateFileReader;
	private ApplicationManager applicationManager;

	private Logger logger = LoggerFactory.getLogger(Simulation.class);

	public Simulation (InputRateFileReader inputRateFileReader, ApplicationManager applicationManager) {
		this.inputRateFileReader = inputRateFileReader;
		this.applicationManager = applicationManager;

		Configuration conf = Configuration.getInstance();
		this.LATENCY_SLO = conf.getDouble(ConfigurationKeys.SLO_LATENCY_KEY, 0.100);
	}

	public void run() throws IOException {
		run(-1l);
	}

	private void registerMetrics () {
		Statistics statistics = Statistics.getInstance();

		//SIMPLE METRICS
		// steps counter metric
		statistics.registerMetric(new CountMetric(STAT_STEPS_COUNTER));
		// SLO violations counter
		statistics.registerMetric(new CountMetric(STAT_LATENCY_VIOLATIONS));
		// reconfigurations counter
		statistics.registerMetric(new CountMetric(STAT_RECONFIGURATIONS));
		// resources cost used sum
		statistics.registerMetric(new RealValuedCountMetric(STAT_RESOURCES_COST_USED_SUM));
		// max possible resources sum
		statistics.registerMetric(new RealValuedCountMetric(STAT_RESOURCES_COST_MAX_SUM));

		// COMPOSED METRICS
		// SLO violations percentage
		statistics.registerMetric(new PercentageMetric(STAT_VIOLATIONS_PERCENTAGE,
				statistics.getMetric(STAT_LATENCY_VIOLATIONS), statistics.getMetric(STAT_STEPS_COUNTER)));
		// reconfigurations percentage
		statistics.registerMetric(new PercentageMetric(STAT_RECONFIGURATIONS_PERCENTAGE,
				statistics.getMetric(STAT_RECONFIGURATIONS), statistics.getMetric(STAT_STEPS_COUNTER)));
		// resources cost percentage
		statistics.registerMetric(new PercentageMetric(STAT_RESOURCES_COST_PERCENTAGE,
				statistics.getMetric(STAT_RESOURCES_COST_USED_SUM), statistics.getMetric(STAT_RESOURCES_COST_MAX_SUM)));
		// resources cost avg
		statistics.registerMetric(new AvgMetric(STAT_RESOURCES_COST_AVG,
				statistics.getMetric(STAT_RESOURCES_COST_USED_SUM), (CountMetric) statistics.getMetric(STAT_STEPS_COUNTER)));
	}

	public void run (long stopTime) throws IOException {
		registerMetrics();

		logger.warn("Starting simulation");

		Application app = applicationManager.getApplication();
		EDF edf = new EDF(app);
		MonitoringInfo monitoringInfo = new MonitoringInfo();

		while (inputRateFileReader.hasNext() && (stopTime <= 0 || time <= stopTime)) {
			double inputRate = inputRateFileReader.next();

			double responseTime = app.endToEndLatency(inputRate);
			if (responseTime > LATENCY_SLO) {
				Statistics.getInstance().updateMetric(STAT_LATENCY_VIOLATIONS, 1);
			}

			// update used resources cost metric
			Statistics.getInstance().updateMetric(STAT_RESOURCES_COST_USED_SUM, app.computeDeploymentCost());
			// update max resources cost metric
			Statistics.getInstance().updateMetric(STAT_RESOURCES_COST_MAX_SUM, app.computeMaxDeploymentCost());

			/* Reconfiguration */
			monitoringInfo.setInputRate(inputRate);
			Map<Operator, Reconfiguration> reconfigurations = edf.pickReconfigurations(monitoringInfo);
			applyReconfigurations(reconfigurations);


			//System.out.println(inputRate + "\t" + responseTime);

			// update steps counter
			Statistics.getInstance().updateMetric(STAT_STEPS_COUNTER, 1);

			// metrics sampling
			Statistics.getInstance().sampleAll(time);

			if (time % 5000 == 0)
				// TODO print simulation completion percentage
				System.out.println(time);

			time++;
		}
	}

	private boolean applyReconfigurations(Map<Operator, Reconfiguration> reconfigurations) {
		// TODO we assume to have unlimited resources
		boolean appReconfigured = false;

		for (Operator op : reconfigurations.keySet()) {
			Reconfiguration rcf = reconfigurations.get(op);
			if (!rcf.isReconfiguration())
				continue;

			op.reconfigure(rcf);
			appReconfigured = true;
		}

		if (appReconfigured)
			Statistics.getInstance().updateMetric(STAT_RECONFIGURATIONS, 1);

		return appReconfigured;
	}

	private void dumpConfigs() {
		File configCopy = new File(String.format("%s/%s/%s/others/configs",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
				Configuration.getInstance().getInitTime(),
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
		File statsOutput = new File(String.format("%s/%s/%s/others/final_stats",
				Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
				Configuration.getInstance().getInitTime(),
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
	}

	public static void main (String[] args) {
		LoggingUtils.configureLogging();

		Configuration conf = Configuration.getInstance();
		conf.parseDefaultConfigurationFile();
		// TODO parse cli args and other configuration files (if any)

		ComputingInfrastructure
				.initDefaultInfrastructure(conf.getInteger(ConfigurationKeys.NODE_TYPES_NUMBER_KEY, 3));

		//TODO try to use tracker
		TrackerManager completeSimulationTracker = new TrackerManager.Builder("Complete Simulation Tracker")
				.trackExecTime()
				.trackMemory()
				.trackCPU()
				.addSampler(new StepSampler("STEP SAMPLER", 1))
				.build();

		try {
			final String inputFile = conf
					.getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, "/home/gabriele/profile.dat");
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Application app = ApplicationBuilder.singleOperatorApplication();
			ApplicationManager am = new ApplicationManager(app);

			Simulation simulation = new Simulation(inputRateFileReader, am);

			// track simulation run elapsed time and memory and cpu usage
			completeSimulationTracker.startTracking();
			// run simulation
			simulation.run();
			//get tracked metrics
			completeSimulationTracker.track();

			/* Dump used configuration in output folder. */
			simulation.dumpConfigs();

			/* Dump statistics in output folder. */
			simulation.dumpStats();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
