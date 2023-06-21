Run the simulator using the example `docker.properties`:

	docker run --rm -v "$(pwd)/docker.properties":/conf/conf.properties grussorusso/dspsim:1.0

## Threshold-based scaling

Edit the configuration file (default: `docker.properties`) to choose the
threshold-based OM policy:

    edf.om.type = threshold

This policy adds a replica if the average utilization exceeds a given threshold
`T`. A replica out of `N` is removed if the average utilization rescaled over
`(N-1)` replicas is lower than `T'` (where `T'=T*a`).

Try setting different values for the threshold `T` and the coefficient `a`, e.g.:

    edf.om.threshold = 0.7
    edf.om.threshold.lower.coeff = 0.75

Observe the average cost obtained, as well as the number of violations and
reconfigurations, and resource usage.
    
## Q-learning

Edit the configuration file (default: `docker.properties`) to choose the
Q-learning OM policy:

    edf.om.type = qlearning

Experiment with different configurations for `alpha`  (learning rate) and `epsilon`
(exploration probability). Both can be configured to decay over time.

Edit these parts of the configuration:

    # alpha
    edf.ql.om.alpha = 1.0
    edf.ql.om.alpha.decay = 0.98
    edf.ql.om.alpha.min.value = 0.1
    edf.ql.om.alpha.decay.steps = 10

    # epsilon
    asp.eg.epsilon = 1.0
    asp.eg.epsilon.decay = 0.95
    asp.eg.epsilon.min.value = 0.01
    asp.eg.om.epsilon.decay.steps = 1

You can also experiment with different values for `gamma` (discount factor):

    edf.dp.gamma = 0.95

## Model-free vs Model-based RL

Compare the performance (and execution time) of Q-learning, Q-learning+PDS and
Model-based RL, with 1, 2 or 3 types of computing resources.

Edit the `edf.om.type`:

- `qlearning`
- `qlearning-pds`
- `model-based`

Edit `node.types.number = M` to set `M` types of computing resources.
