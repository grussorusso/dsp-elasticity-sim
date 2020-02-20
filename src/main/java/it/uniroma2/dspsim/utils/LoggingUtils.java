package it.uniroma2.dspsim.utils;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LoggingUtils {

	private static final String DEFAULT_LOG_FILENAME = "./simLog.log";

	static public void configureLogging() {

		Configuration conf = Configuration.getInstance();
		String logFilename = conf.getString(ConfigurationKeys.OUTPUT_LOG_FILENAME, DEFAULT_LOG_FILENAME);

		ConfigurationBuilder<BuiltConfiguration> cb = ConfigurationBuilderFactory.newConfigurationBuilder();
		LayoutComponentBuilder standard = cb.newLayout("PatternLayout");
		standard.addAttribute("pattern", "%d [%t][%-5level] %C{1} - %msg%n%throwable");

		AppenderComponentBuilder console = cb.newAppender("stdout", "Console");
		console.add(standard);
		cb.add(console);

		if (!logFilename.isEmpty()) {
			AppenderComponentBuilder file = cb.newAppender("log", "File");
			file.addAttribute("fileName", logFilename);
			file.addAttribute("append", false);
			file.add(standard);
			cb.add(file);
		}

		RootLoggerComponentBuilder rootLogger = cb.newRootLogger(Level.ERROR);
		rootLogger.add(cb.newAppenderRef("stdout"));
		cb.add(rootLogger);

		LoggerComponentBuilder logger = cb.newLogger("it.uniroma2", Level.DEBUG);
		logger.add(cb.newAppenderRef("log"));
		logger.addAttribute("additivity", false);
		cb.add(logger);

		Configurator.initialize(cb.build());
	}
}
