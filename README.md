# DSP Elasticity Simulator

This software is used to evaluate elasticity policies for distributed Data
Stream Processing applications.

## Building ##

	mvn install package -DskipTests
	java -jar target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar



## Configuration ##

The default configuration file `conf.properties` is in `resources` directory.

Available configuration options are currently described in `ConfigurationKeys` class.
