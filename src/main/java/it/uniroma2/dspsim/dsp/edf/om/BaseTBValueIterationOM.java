package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.concrete.EpsilonGreedyActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public abstract class BaseTBValueIterationOM extends DynamicProgrammingOM implements ActionSelectionPolicyCallback {

    private Random rng;

    protected int statesCount;
    protected int actionsCount;

    public BaseTBValueIterationOM(Operator operator) {
        super(operator);

        this.actionsCount = computeActionsCount();
        this.statesCount = computeStatesCount();

        this.rng = new Random();
    }

    protected void tbvi(long millis, long trajectoryLength) {
        ActionSelectionPolicy epsGreedyASP = ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.EPSILON_GREEDY, this);
        ((EpsilonGreedyActionSelectionPolicy) epsGreedyASP).setEpsilon(0.1);
        ((EpsilonGreedyActionSelectionPolicy) epsGreedyASP).setEpsilonDecaySteps(-1);
        // get initial state
        State state = null;
        // current trajectory length
        long tl = 0L;

        // trajectory counter
        long trajectoriesComputed = 0;

        while (millis > 0) {
            long startIteration = System.currentTimeMillis();

            if (trajectoryLength > 0 && tl % trajectoryLength == 0)
                tl = 0L;
            if (tl == 0L) {
                resetTrajectoryData();
                // start new trajectory
                state = randomState();

                trajectoriesComputed++;
            }

            Action action = epsGreedyASP.selectAction(state);


            state = tbviIteration(state, action);

            tl++;

            millis -= (System.currentTimeMillis() - startIteration);
        }

        System.out.println("Trajectory length: " + trajectoryLength);
        System.out.println("Trajectories computed: " + trajectoriesComputed);
        System.out.println("Samples: " + trajectoriesComputed * trajectoryLength);
    }

    protected State tbviIteration(State s, Action a) {
        double oldQ = computeQ(s, a);
        double newQ = evaluateNewQ(s, a);
        double delta = newQ - oldQ;

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

    private State randomState() {

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
    }

    private State sampleNextState(State s, Action a) {
        // TODO view sampling
        State pds = StateUtils.computePostDecisionState(s, a, this);
        List<Double> pArray = new ArrayList<>();
        for (int l : this.getpMatrix().getColLabels(s.getLambda())) {
            int times = (int) Math.floor(100 * this.getpMatrix().getValue(s.getLambda(), l));
            times = times > 0 ? times : 1;
            for (int i = 0; i < times; i++) {
                pArray.add((double) l);
            }
        }
        double[] lambdas = new double[pArray.size()];
        for (int i = 0; i < lambdas.length; i++) {
            lambdas[i] = pArray.get(i);
        }
        INDArray lambdaArray = Nd4j.create(lambdas);
        Nd4j.shuffle(lambdaArray, 0);
        int nextLambda = lambdaArray.getInt(new Random().nextInt(lambdaArray.length()));
        return StateFactory.createState(getStateRepresentation(), -1, pds.getActualDeployment(), nextLambda,
                getInputRateLevels() - 1, this.operator.getMaxParallelism());
    }

    private double evaluateNewQ(State s, Action a) {
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
            double sloCost = StateUtils.computeSLOCost(pds, this);

            cost += p * (sloCost + getGamma() * q);
        }

        return cost;
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
