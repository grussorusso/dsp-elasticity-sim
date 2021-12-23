# DSP Elasticity Simulator

This software is used to evaluate elasticity policies for distributed Data
Stream Processing applications.

The system model and control architecture used in this software corresponds to
those described in [FGCS2018].

## Building ##

	mvn install package -DskipTests
	java -jar target/dsp-elasticity-simulator-1.0-SNAPSHOT-shaded.jar


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



## References ##

[FGCS2018] Valeria Cardellini, Francesco Lo Presti, Matteo Nardelli, Gabriele Russo Russo:
Decentralized self-adaptation for elastic Data Stream Processing. Future Gener. Comput. Syst. 87: 171-185 (2018)
