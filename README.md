# DSP Elasticity Simulator

This software is used to evaluate elasticity policies for distributed Data
Stream Processing applications.

## Building ##

	mvn install package -DskipTests
	java -jar target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar

To reduce the size of the JAR, you can add the following flag, excluding native
libraries for platforms different than Linux x86-64:

	 -Djavacpp.platform=linux-x86_64



## Configuration ##

The default configuration file `conf.properties` is in `resources` directory.

Available configuration options are currently described in `ConfigurationKeys` class.


## Known Issues ##

- Double Q Network (`edf.dl.network.double.enabled`) does not work well with `deep-v-learning` OM
- Deeplearning4j beyond version `0.9.x` is not supported
