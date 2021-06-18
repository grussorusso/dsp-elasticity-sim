package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.EpsilonGreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.Random;
import java.util.Set;

public abstract class BaseTBValueIterationOM extends DynamicProgrammingOM implements ActionSelectionPolicyCallback {

    private Random rng;

    protected int statesCount;
    protected int actionsCount;

    protected long tbviIterations = 0l;

    // tbvi parameters
    protected long tbviMaxIterations;
    protected long tbviMillis;
    protected long tbviTrajectoryLength;

    private double deltaRunningAvg = 0.0;

    final private boolean fallbackToVI = false;

    public BaseTBValueIterationOM(Operator operator) {
        super(operator);

        this.actionsCount = computeActionsCount();
        this.statesCount = computeStatesCount();

        System.out.printf("States = %d, actions = %d\n", this.statesCount, this.actionsCount);

        Configuration configuration = Configuration.getInstance();

        this.tbviMaxIterations = configuration.getLong(ConfigurationKeys.TBVI_EXEC_ITERATIONS_KEY, 300000L);
        this.tbviMillis = configuration.getLong(ConfigurationKeys.TBVI_EXEC_SECONDS_KEY, 60L) * 1000;
        this.tbviTrajectoryLength = configuration.getLong(ConfigurationKeys.TBVI_TRAJECTORY_LENGTH_KEY, 512L);

        final int seed = configuration.getInteger(ConfigurationKeys.DL_OM_ND4j_RANDOM_SEED_KET, 1) + 5;
        this.rng = new Random(seed);
    }

    protected void vi(long maxIterations, long millis) {
        // elapsed millis
        long elapsedMillis = 0L;

        while ((maxIterations <= 0L || tbviIterations < maxIterations) && (millis <= 0L || elapsedMillis < millis)) {
            System.out.printf("TBVI: %d iterations (delta = %.4f)\n",tbviIterations, deltaRunningAvg);

            long startIteration = System.currentTimeMillis();

            StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                    ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());
            while (stateIterator.hasNext()) {
                State s = stateIterator.next();
                ActionIterator actionIterator = new ActionIterator();

                while (actionIterator.hasNext()) {
                    Action action = actionIterator.next();
                    if (!validateAction(s, action))
                        continue;

                    tbviIteration(s,action);
                    tbviIterations++;
                }
            }


            elapsedMillis += (System.currentTimeMillis() - startIteration);
        }

        System.out.println("VI Total Iters: " + tbviIterations);
        this.trainingEpochsCount.update((int)tbviIterations);
        this.planningTimeMetric.update((int)(elapsedMillis));
    }

    protected void tbvi(long maxIterations, long millis, long trajectoryLength) {
        if (fallbackToVI) {
            vi(maxIterations, millis);
            return;
        }

        ActionSelectionPolicy epsGreedyASP = ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        ((EpsilonGreedyActionSelectionPolicy) epsGreedyASP).setEpsilon(0.5);
        ((EpsilonGreedyActionSelectionPolicy) epsGreedyASP).setEpsilonDecaySteps(-1);
        // get initial state
        State state = null;
        // current trajectory length
        long tl = 0L;

        // trajectory counter
        long trajectoriesComputed = 0;

        // elapsed millis
        long elapsedMillis = 0L;

        while ((maxIterations <= 0L || tbviIterations < maxIterations) && (millis <= 0L || elapsedMillis < millis)) {
            if (tbviIterations % 10000 == 0) {
                System.out.printf("TBVI: %d iterations (delta = %.4f)\n",tbviIterations, deltaRunningAvg);
            }

            long startIteration = System.currentTimeMillis();

            if (trajectoryLength > 0 && tl % trajectoryLength == 0)
                tl = 0L;
            if (tl == 0L) {
                resetTrajectoryData();
                state = randomInitialState(trajectoriesComputed);
                //System.out.println("Initial state: " + state.toString());

                trajectoriesComputed++;
            }

            Action action = epsGreedyASP.selectAction(state);


            state = tbviIteration(state, action);

            tl++;


            tbviIterations++;
            elapsedMillis += (System.currentTimeMillis() - startIteration);
        }

        System.out.println("TBVI Total Trajectories: " + trajectoriesComputed);
        System.out.println("TBVI Total Iters: " + tbviIterations);
        this.trainingEpochsCount.update((int)tbviIterations);
        this.planningTimeMetric.update((int)(elapsedMillis));
    }

    protected State tbviIteration(State s, Action a) {
        double oldQ = computeQ(s, a);
        double newQ = evaluateNewQ(s, a);

        double delta = newQ - oldQ;
        updateDeltaRunningAvg(delta);

        learn(delta, newQ, s, a);

        return sampleNextState(s, a);
    }

    protected int computeStatesCount() {
        StateIterator stateIterator = new StateIterator(getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), getInputRateLevels());

        int count = 0;
        while (stateIterator.hasNext()) {
            stateIterator.next();
            count++;
        }
        return count;
    }

    protected int computeActionsCount() {
        ActionIterator actionIterator = new ActionIterator();

        int count = 0;
        while (actionIterator.hasNext()) {
            actionIterator.next();
            count++;
        }
        return count;
    }

    protected double computeResourcesCost (State pds) {
        return StateUtils.computeDeploymentCostNormalized(pds, this) * this.getwResources();
    }

    static private boolean RANDOM_NG = true;
    private State randomInitialState(long trajectoriesCount) {

        if (!RANDOM_NG) {
            int randomIndex = rng.nextInt(this.statesCount);
            StateIterator stateIterator = new StateIterator(getStateRepresentation(), this.operator.getMaxParallelism(),
                    ComputingInfrastructure.getInfrastructure(), getInputRateLevels());

            State s = null;
            while (stateIterator.hasNext()) {
                s = stateIterator.next();
                if (s.getIndex() == randomIndex)
                    return s;
            }
            return s;
        } else {
            final int randomResType = rng.nextInt(ComputingInfrastructure.getInfrastructure().getNodeTypes().length);
            final int parallelism = 1 + rng.nextInt(operator.getMaxParallelism());
            final int rate = (int)trajectoriesCount % getInputRateLevels();
            int k[] = new int[ComputingInfrastructure.getInfrastructure().getNodeTypes().length];
            k[randomResType] = parallelism;
            return StateFactory.createState(StateType.K_LAMBDA, -1, k, rate, getInputRateLevels()-1,
                    operator.getMaxParallelism());
        }
    }

    protected State sampleNextState(State s, Action a) {
        final boolean simplifiedSampling = false;

        if (fallbackToVI)
            return null;

        State pds = StateUtils.computePostDecisionState(s, a, this);

        int nextLambda = 0;
        if (simplifiedSampling) {
            nextLambda = s.getLambda() + (-1 + rng.nextInt() % 3);
            if (nextLambda < 0)
                nextLambda = 0;
            if (nextLambda >= this.getInputRateLevels())
                nextLambda = this.getInputRateLevels() - 1;
        } else {
            double randomN = rng.nextDouble();
            double totalProb = 0.0;
            Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
            for (int lambda : possibleLambdas) {
                nextLambda = lambda;
                // get transition probability from s.lambda to lambda level
                final double p = this.getpMatrix().getValue(s.getLambda(), lambda);
                totalProb += p;

                if (totalProb > randomN)
                    break;
            }
        }

        return StateFactory.createState(getStateRepresentation(), -1, pds.getActualDeployment(), nextLambda,
                getInputRateLevels() - 1, this.operator.getMaxParallelism());
    }

    protected double evaluateNewQ(State s, Action a) {
        double cost = 0.0;
        // compute reconfiguration cost
        if (a.getDelta() != 0)
            cost += this.getwReconf();
        // from s,a compute pds
        State pds = StateUtils.computePostDecisionState(s, a, this);
        // compute deployment cost using pds wighted on wRes
        cost += StateUtils.computeDeploymentCostNormalized(pds, this) * this.getwResources();
        // for each lambda level with p != 0 in s.getLambda() row
        Set<Integer> possibleLambdas = getpMatrix().getColLabels(s.getLambda());
        for (int lambda : possibleLambdas) {
            // change pds.lambda to lambda
            pds.setLambda(lambda);
            // get Q(s, a) using the greedy action selection policy
            // from post decision state with lambda ad pds.lambda
            Action greedyAction = getActionSelectionPolicy().selectAction(pds);
            double q = computeQ(pds, greedyAction);
            // get transition probability from s.lambda to lambda level
            double p = this.getpMatrix().getValue(s.getLambda(), lambda);
            // compute slo violation cost
            double sloCost = StateUtils.computeSLOCost(pds, this) * this.getwSLO();

            cost += p * (sloCost + getGamma() * q);
        }

        return cost;
    }

    protected void updateDeltaRunningAvg (double delta) {
        final double alpha = 0.001;
        deltaRunningAvg = alpha * Math.abs(delta) + (1.0-alpha) * deltaRunningAvg;
    }

    @Override
    public boolean validateAction(State s, Action a) {
        return s.validateAction(a);
    }



    /**
     * ABSTRACT METHODS
     */

    protected abstract void resetTrajectoryData();
    protected abstract double computeQ(State s, Action a);
    protected abstract void learn(double tbviDelta, double newQ, State state, Action action);
}
