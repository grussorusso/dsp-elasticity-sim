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
import it.uniroma2.dspsim.utils.LoggingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
	private static final String STAT_RESOURCES_COST_MEAN = "Resources Cost Mean";

	private InputRateFileReader inputRateFileReader;
	private ApplicationManager applicationManager;

	private Statistics statistics = Statistics.getInstance();

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
		// resources cost mean
		statistics.registerMetric(new MeanMetric(STAT_RESOURCES_COST_MEAN,
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
				statistics.updateMetric(STAT_LATENCY_VIOLATIONS, 1);
			}

			// update used resources cost metric
			statistics.updateMetric(STAT_RESOURCES_COST_USED_SUM, app.computeDeploymentCost());
			// update max resources cost metric
			statistics.updateMetric(STAT_RESOURCES_COST_MAX_SUM, app.computeMaxDeploymentCost());

			/* Reconfiguration */
			monitoringInfo.setInputRate(inputRate);
			Map<Operator, Reconfiguration> reconfigurations = edf.pickReconfigurations(monitoringInfo);
			applyReconfigurations(reconfigurations);


			//System.out.println(inputRate + "\t" + responseTime);

			// update steps counter
			statistics.updateMetric(STAT_STEPS_COUNTER, 1);

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
			statistics.updateMetric(STAT_RECONFIGURATIONS, 1);

		return appReconfigured;
	}

	public static void main (String args[]) {
		LoggingUtils.configureLogging();

		Configuration conf = Configuration.getInstance();
		conf.parseDefaultConfigurationFile();
		// TODO parse cli args and other configuration files (if any)

		ComputingInfrastructure.initDefaultInfrastructure(3);


		try {
			final String inputFile = "/home/gabriele/profile.dat";
			InputRateFileReader inputRateFileReader = new InputRateFileReader(inputFile);

			Application app = ApplicationBuilder.defaultApplication();
			ApplicationManager am = new ApplicationManager(app);

			Simulation simulation = new Simulation(inputRateFileReader, am);
			simulation.run();

			/* Dump used configuration. */
			conf.dump(System.out);

			/* Dump statistics to standard output. */
			Statistics.getInstance().dumpAll();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
