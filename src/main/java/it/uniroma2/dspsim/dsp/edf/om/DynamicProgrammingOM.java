package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.InputRateFileReader;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.infrastructure.NodeType;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.IntegerMatrix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public abstract class DynamicProgrammingOM extends RewardBasedOM {

    private String inputRateFilePath;

    private double gamma;

    // p matrix
    private DoubleMatrix<Integer, Integer> pMatrix;

    public DynamicProgrammingOM(Operator operator) {
        super(operator);

        this.inputRateFilePath = Configuration.getInstance()
                .getString(ConfigurationKeys.INPUT_FILE_PATH_KEY, "/home/gabriele/profile.dat");

        try {
            this.pMatrix = buildPMatrix(this.inputRateFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO configure
        this.gamma = 0.99;

        buildPolicy();
    }

    protected State computePostDecisionState(State state, Action action) {
        if (action.getDelta() != 0) {
            int[] pdk = Arrays.copyOf(state.getActualDeployment(), state.getActualDeployment().length);
            int aIndex = action.getResTypeIndex();
            pdk[aIndex] = pdk[aIndex] + action.getDelta();
            return StateFactory.createState(this.getStateRepresentation(), -1, pdk,
                    state.getLambda(), this.getInputRateLevels() - 1, this.operator.getMaxParallelism());
        } else {
            return state;
        }
    }

    protected double computePostDecisionCost(int[] deployment, double inputRate) {
        double cost = 0.0;

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

        if (this.operator.getQueueModel().responseTime(inputRate, operatorInstances.size(), currentSpeedup) > this.operator.getSloRespTime())
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
    protected final void useReward(double reward, State lastState, Action lastChosenAction, State currentState, OMMonitoringInfo monitoringInfo) {}

    /**
     * ABSTRACT METHODS
     */

    protected abstract void buildPolicy();
    protected abstract void dumpPolicyOnFile(String filename);

    /**
     * GETTERS
     */
    public String getInputRateFilePath() {
        return inputRateFilePath;
    }

    public double getGamma() {
        return gamma;
    }

    public DoubleMatrix<Integer, Integer> getpMatrix() {
        return pMatrix;
    }
}
