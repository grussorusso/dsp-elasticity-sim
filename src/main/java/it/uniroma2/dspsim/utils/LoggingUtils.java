package it.uniroma2.dspsim.utils;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

public class LoggingUtils {

	private static final String LOG_FILENAME = "./simLog.log";

	static public void configureLogging() {
		ConfigurationBuilder<BuiltConfiguration> cb = ConfigurationBuilderFactory.newConfigurationBuilder();
		LayoutComponentBuilder standard = cb.newLayout("PatternLayout");
		standard.addAttribute("pattern", "%d [%t][%-5level] %C{1} - %msg%n%throwable");

		AppenderComponentBuilder console = cb.newAppender("stdout", "Console");
		console.add(standard);
		cb.add(console);

		AppenderComponentBuilder file = cb.newAppender("log", "File");
		file.addAttribute("fileName", LOG_FILENAME);
		file.addAttribute("append", false);
		file.add(standard);
		cb.add(file);

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
