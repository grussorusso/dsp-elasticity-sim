package it.uniroma2.dspsim.dsp.edf.om;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.InputRateFileReader;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.Reconfiguration;
import it.uniroma2.dspsim.dsp.edf.om.OMMonitoringInfo;
import it.uniroma2.dspsim.dsp.edf.om.OperatorManager;
import it.uniroma2.dspsim.dsp.edf.om.rl.AbstractAction;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.IntegerMatrix;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class ValueIterationOM extends RewardBasedOM implements ActionSelectionPolicyCallback {

    private String inputRateFilePath;

    private double gamma;

    // p matrix
    private DoubleMatrix<Integer, Integer> pMatrix;

    // V matrix
    private DoubleMatrix<Integer, Integer> policy;

    public ValueIterationOM(Operator operator) {
        super(operator);

        this.pMatrix = new DoubleMatrix<>(0.0);
        this.policy = new DoubleMatrix<>(0.0);

        // TODO configure
        this.gamma = 0.9;

        this.inputRateFilePath = Configuration.getInstance()
                .getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, "/home/gabriele/profile.dat");


        try {
            this.pMatrix = buildPMatrix(this.inputRateFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
        long count = 0L;
        while (stateIterator.hasNext()) {
            State s = stateIterator.next();
            count++;
        }

        valueIteration(0, 0.05);
    }

    /**
     * VALUE ITERATION ALGORITHM
     * @param maxIterations max iterations of the algorithm, if maxIteration is greater than 0 it will be stopped
     *                      even if delta is still greater than theta
     * @param theta accuracy threshold
     */
    private void valueIteration(int maxIterations, double theta) {
        int stepsCounter = 0;
        double delta = Double.POSITIVE_INFINITY;
        while (delta > theta || stepsCounter < maxIterations) {
            delta = vi();
            // if max iterations is greater than 0 increment counter
            if (maxIterations > 0) {
                stepsCounter++;
                if (stepsCounter == maxIterations) {
                    break;
                }
            }
        }
    }

    private double vi() {
        double delta = 0.0;

        StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            double newDelta = computeValueIteration(state);
            delta = Math.max(delta, newDelta);
        }

        return delta;
    }

    private double computeValueIteration(State state) {
        double delta = 0.0;

        ActionIterator actionIterator = new ActionIterator();

        while (actionIterator.hasNext()) {
            Action action = actionIterator.next();
            if (!validateAction(state, action))
                continue;

            double oldQ = this.policy.getValue(state.hashCode(), action.hashCode());
            double newQ = evaluateQ(state, action);
            this.policy.setValue(state.hashCode(), action.hashCode(), newQ);

            delta = Math.max(delta, Math.abs(oldQ - newQ));
        }

        return delta;
    }

    private double evaluateQ(State s, Action a) {
        double cost = 0.0;
        // compute reconfiguration cost
        if (a.getDelta() != 0)
            cost += this.getwReconf();
        // from s,a compute pds
        State pds = computePostDecisionState(s, a);
        // get V(s) using the greedy action selection policy from post decision state
        Action greedyAction = getActionSelectionPolicy().selectAction(pds);
        double v = policy.getValue(pds.hashCode(), greedyAction.hashCode());
        // for each lambda level with p != 0 in s.getLambda() row
        Set<Integer> possibleLambdas = pMatrix.getColLabels(s.getLambda());
        for (int lambda : possibleLambdas) {
            // get transition probability from s.lambda to lambda level
            double p = this.pMatrix.getValue(s.getLambda(), lambda);
            // compute slo violation and deployment cost from post decision operator view
            // recover input rate value from lambda level getting middle value of relative interval
            double pdCost = computePostDecisionCost(pds.getActualDeployment(), a, MathUtils.remapDiscretizedValue(this.getMaxInputRate(), lambda, this.getInputRateLevels()));

            cost += p * (pdCost + this.gamma * v);
        }

        return cost;
    }

    private State computePostDecisionState(State state, Action action) {
        if (action.getDelta() != 0) {
            int[] pdk = Arrays.copyOf(state.getActualDeployment(), state.getActualDeployment().length);
            int aIndex = action.getResTypeIndex();
            pdk[aIndex] = pdk[aIndex] + action.getDelta();
            return StateFactory.createState(this.getStateRepresentation(), -1, pdk,
                    state.getLambda(), this.getInputRateLevels(), this.operator.getMaxParallelism());
        } else {
            return state;
        }
    }

    private double computePostDecisionCost(int[] deployment, Action action, double inputRate) {
        double cost = 0.0;

        if (action.getDelta() != 0)
            cost += this.getwReconf();

        final double OPERATOR_SLO = 0.1;

        double currentSpeedup = Double.POSITIVE_INFINITY;
        List<NodeType> usedNodeTypes = new ArrayList<>();
        List<NodeType> operatorInstances = new ArrayList<>();
        for (int i = 0; i < deployment.length; i++) {
            if (deployment[i] > 0)
                usedNodeTypes.add(ComputingInfrastructure.getInfrastructure().getNodeTypes()[i]);
            for (int j = 0; j < deployment[i]; j++) {
                operatorInstances.add(ComputingInfrastructure.getInfrastructure().getNodeTypes()[i]);
            }
        }

        for (NodeType nt : usedNodeTypes)
            currentSpeedup = Math.min(currentSpeedup, nt.getCpuSpeedup());

        if (this.operator.getQueueModel().responseTime(inputRate, operatorInstances.size(), currentSpeedup) > OPERATOR_SLO)
            cost += this.getwSLO();

        double deploymentCost = 0.0;
        for (NodeType nt : operatorInstances)
            deploymentCost += nt.getCost();

        double maxCost = this.operator.getMaxParallelism() * ComputingInfrastructure.getInfrastructure().getMostExpensiveResType().getCost();

        cost += (deploymentCost / maxCost) * this.getwResources();

        return cost;
    }

    private DoubleMatrix<Integer, Integer> buildPMatrix(String inputRateFilePath) throws IOException {
        InputRateFileReader inputRateFileReader = new InputRateFileReader(inputRateFilePath);

        IntegerMatrix<Integer, Integer> transitionMatrix = computeTransitionMatrix(inputRateFileReader);

        DoubleMatrix<Integer, Integer> pMatrix = new DoubleMatrix<>(0.0);

        for (Integer x : transitionMatrix.getRowLabels()) {
            Integer total = transitionMatrix.rowSum(x);
            for (Integer y : transitionMatrix.getColLabels(x)) {
                pMatrix.setValue(x, y, transitionMatrix.getValue(x, y).doubleValue() / total.doubleValue());
            }
        }

        return pMatrix;
    }

    private IntegerMatrix<Integer, Integer> computeTransitionMatrix(InputRateFileReader inputRateFileReader) throws IOException {
        IntegerMatrix<Integer, Integer> transitionMatrix = new IntegerMatrix<>(0);
        if (inputRateFileReader.hasNext()) {
            int prevInputRateLevel = MathUtils.discretizeValue(this.getMaxInputRate(), inputRateFileReader.next(), this.getInputRateLevels());
            while (inputRateFileReader.hasNext()) {
                int inputRateLevel = MathUtils.discretizeValue(this.getMaxInputRate(), inputRateFileReader.next(), this.getInputRateLevels());
                transitionMatrix.add(prevInputRateLevel, inputRateLevel, 1);
                prevInputRateLevel = inputRateLevel;
            }
        }

        return transitionMatrix;
    }

    @Override
    public boolean validateAction(State s, Action a) {
        return s.validateAction(a);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return this.policy.getValue(s.hashCode(), a.hashCode());
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }
}
