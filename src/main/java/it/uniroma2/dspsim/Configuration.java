package it.uniroma2.dspsim;


import com.sun.org.apache.xpath.internal.operations.Bool;

import java.io.*;
import java.time.Instant;
import java.util.Properties;

public class Configuration {

	private static Configuration instance = null;

	private Configuration() {
		this.properties = new Properties();
	}

	private Properties properties;

	public static synchronized Configuration getInstance() {
		if (instance == null) {
			instance = new Configuration();
		}

		return instance;
	}

	public void parseConfigurationFile (String configFilePath) {
		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(configFilePath));
			parseConfigurationFile(inputStream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void parseDefaultConfigurationFile () {
		final String propFileName = "config.properties";
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);
		if (inputStream == null) {
			System.err.println("property file '" + propFileName + "' not found in the classpath");
			return;
		}

		parseConfigurationFile(inputStream);
	}

	public void parseConfigurationFile (InputStream inputStream) {
		try {
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public Integer getInteger (String key, Integer defaultValue) {
		if (properties.containsKey(key)) {
			try {
				return Integer.parseInt(properties.getProperty(key));
			} catch (NumberFormatException e) { }
		}

		properties.setProperty(key, Integer.toString(defaultValue));
		return defaultValue;
	}

	public Long getLong (String key, Long defaultValue) {
		if (properties.containsKey(key)) {
			try {
				return Long.parseLong(properties.getProperty(key));
			} catch (NumberFormatException e) { }
		}

		properties.setProperty(key, Long.toString(defaultValue));
		return defaultValue;
	}

	public Double getDouble (String key, Double defaultValue) {
		if (properties.containsKey(key)) {
			try {
				return Double.parseDouble(properties.getProperty(key));
			} catch (NumberFormatException e) { }
		}

		properties.setProperty(key, Double.toString(defaultValue));
		return defaultValue;
	}

	public String getString (String key, String defaultValue) {
		if (properties.containsKey(key))
			return properties.getProperty(key);

		properties.setProperty(key, defaultValue);
		return defaultValue;
	}

	public boolean getBoolean(String key, boolean defaultValue) {
		if (properties.containsKey(key))
			return Boolean.parseBoolean(properties.getProperty(key));

		properties.setProperty(key, Boolean.toString(defaultValue));
		return defaultValue;
	}

	/**
	 * Prints currently loaded properties
	 * @param os OutputStream to be used.
	 */
	public void dump(OutputStream os) {
		try {
			properties.store(os, "");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
