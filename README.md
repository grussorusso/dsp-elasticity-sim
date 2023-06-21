# DSP Elasticity Simulator

This software is used to evaluate elasticity policies for distributed Data
Stream Processing applications.

The system model and auto-scaling policies implemented are described
in the following paper:

    Gabriele Russo Russo, Valeria Cardellini, Francesco Lo Presti:
    Hierarchical Auto-Scaling Policies for Data Stream Processing on Heterogeneous Resources. ACM Trans. Auton. Adapt. Syst.  (2023)


Some of the algorithms (specifically, those based on tabular reinforcement
learning) are described here:

    Valeria Cardellini, Francesco Lo Presti, Matteo Nardelli, Gabriele Russo Russo:
    Decentralized self-adaptation for elastic Data Stream Processing. Future Gener. Comput. Syst. 87: 171-185 (2018)


## Building ##

	mvn install package -DskipTests
	java -jar target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar

To reduce the size of the JAR, you can add the following flag, excluding native
libraries for platforms different than Linux x86-64:

	 -Djavacpp.platform=linux-x86_64



## Configuration ##

The default configuration file `conf.properties` is in `resources` directory.
We recommend creating a custom configuration file, which can be provided as
a CLI argument to the simulator.

Available configuration options are listed in `ConfigurationKeys` class.

Most relevant configurations:

Key|Description
---|-----------
output.base.path|Directory where output files will be stored
input.file.path | Input rates trace file (see `traces/profile.dat` for example)
input.file.path.training | Historical input rates trace for training (see `traces/profile_last_month.dat` for example)
simulation.stoptime | Maximum simulated time steps
node.types.number | Types of nodes in the infrastructure 
dsp.app.type | Application to simulate (e.g., `single-operator` or `fromfile` to specify it in a separate file)
dsp.app.file | (optional) File with the application specification (see `apps/` for examples)
dsp.slo.latency | Response time SLO
operator.max.parallelism | Maximum allowed parallelism per operator
edf.om.type | Auto-scaling policy to use. (see below)
edf.vi.max.time.seconds | Maximum planning time (for VI-based algs)
edf.rl.om.reconfiguration.weight | Objective weight for the reconfiguration term
edf.rl.om.slo.weight | Objective weight for the SLO term
edf.rl.om.resources.weight | Objective weight for the resource cost term


Available OM policies:

<ul>
  <li>do-nothing</li>
  <li>threshold</li>
  <li>q-learning</li>
  <li>q-learning-pds</li>
  <li>model-based</li>
  <li>fa-q-learning (Q-Learning with function approximation)</li>
  <li>vi (value iteration)</li>
  <li>fa-tb-vi (TBVI using linear function approximation)</li>
  <li>fa-hybrid (fa-tb-vi om for off-line training and fa-q-learning for on-line training)</li>
  </ul>

## Docker ##

You can build a Docker image:

	docker build -t grussorusso/dspsim:1.0 .

And use the example `docker.properties` to run the simulator:

	docker run --rm -v "$(pwd)/docker.properties":/conf/conf.properties grussorusso/dspsim:1.0


## Known Issues ##

- Double Q Network (`edf.dl.network.double.enabled`) does not work well with `deep-v-learning` OM
- Deeplearning4j beyond version `0.9.x` is not supported


