package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTableFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.PolicyIOUtils;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

public class ValueIterationOM extends DynamicProgrammingOM implements ActionSelectionPolicyCallback {

    protected QTable qTable;
    private Logger logger = LoggerFactory.getLogger(ValueIterationOM.class);
    private int updatedStateActions = 0;

    public ValueIterationOM(Operator operator) {
        super(operator);

        Configuration configuration = Configuration.getInstance();

        int maxIterations = configuration.getInteger(ConfigurationKeys.VI_MAX_ITERATIONS_KEY, 0);
        long maxTimeMillis = configuration.getLong(ConfigurationKeys.VI_MAX_TIME_SECONDS_KEY, 60L) * 1000;
        double theta = configuration.getDouble(ConfigurationKeys.VI_THETA_KEY, 1E-5);

        // We can replace the embedded operator if an approximate model is required
        final boolean useApproximateModel = configuration.getBoolean(ConfigurationKeys.VI_APPROX_MODEL, false);
        Operator realOperator = this.operator;
        if (useApproximateModel) {
            this.operator = getApproximateOperator();
        }

        if (!PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
            valueIteration(maxIterations, maxTimeMillis, theta);
        }

        this.operator = realOperator;

        dumpQOnFile(String.format("%s/qtable",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, "")));
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);
    }

    protected QTable getQTable() {
        return this.qTable;
    }

    protected DoubleMatrix<Integer, Integer> getPMatrix() {
        return pMatrix;
    }


    /**
     * VALUE ITERATION ALGORITHM
     * @param maxIterations max iterations of the algorithm, if maxIteration is greater than 0 it will be stopped
     *                      even if delta is still greater than theta
     * @param theta accuracy threshold
     */
    private void valueIteration(int maxIterations, long maxTimeMillis, double theta) {
        long startIterationTime = 0L;
        double delta = Double.POSITIVE_INFINITY;
        while (delta > theta && (maxIterations < 1 || updatedStateActions < maxIterations) && maxTimeMillis > 0) {
            if (maxTimeMillis > 0L)
                startIterationTime = System.currentTimeMillis();

            delta = vi();

            if (maxTimeMillis > 0L)
                maxTimeMillis -= (System.currentTimeMillis() - startIterationTime);
        }

        this.trainingEpochsCount.update(updatedStateActions); // TODO: check if updatedStateActions is ever reset to 0
        this.planningTimeMetric.update((int)(System.currentTimeMillis() - startIterationTime));
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

    protected double computeValueIteration(State state) {
        double delta = 0.0;

        ActionIterator actionIterator = new ActionIterator();

        while (actionIterator.hasNext()) {
            Action action = actionIterator.next();
            if (!validateAction(state, action))
                continue;

            double oldQ = qTable.getQ(state, action);
            double newQ = evaluateQ(state, action);
            qTable.setQ(state, action, newQ);
            updatedStateActions++;

            delta = Math.max(delta, Math.abs(newQ - oldQ));
        }

        return delta;
    }

    private double evaluateQ(State s, Action a) {
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
            // from post decision state with lambda as pds.lambda
            Action greedyAction = getActionSelectionPolicy().selectAction(pds);
            double q = qTable.getQ(pds, greedyAction);
            // get transition probability from s.lambda to lambda level
            double p = getpMatrix().getValue(s.getLambda(), lambda);
            // compute slo violation cost
            double pdCost = StateUtils.computeSLOCost(pds, this) * this.getwSLO();

            cost += p * (pdCost + getGamma() * q);
        }
        return cost;
    }

    @Override
    protected void buildQ() {
        this.qTable = QTableFactory.newQTable(operator.getMaxParallelism(), getInputRateLevels());

        if (PolicyIOUtils.shouldLoadPolicy(Configuration.getInstance())) {
            this.qTable.load(PolicyIOUtils.getFileForLoading(this.operator, "qTable"));
        }
    }

    @Override
    public boolean validateAction(State s, Action a) {
        return s.validateAction(a);
    }

    @Override
    public double evaluateAction(State s, Action a) {
        return this.qTable.getQ(s, a);
    }

    @Override
    protected ActionSelectionPolicy initActionSelectionPolicy() {
        return ActionSelectionPolicyFactory.getPolicy(ActionSelectionPolicyType.GREEDY, this);
    }

    public double computeSLOViolationProbability (State s, Action a) {
        State newS = StateUtils.computePostDecisionState(s,a, this);

        double prob = 0.0;
        for (int lambda = 0; lambda < getInputRateLevels(); ++lambda) {
            newS.setLambda(lambda);
            double p = getpMatrix().getValue(s.getLambda(), newS.getLambda());
            double sloCost = StateUtils.computeSLOCost(newS, this); /* 0 or 1 */
            prob += p*sloCost;
        }

        return prob;
    }
    /**
     * Dump policy on file
     */
    @Override
    protected void dumpQOnFile(String filename) {
        // create file
        File file = new File(filename);
        try {
            if (!file.exists()) {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            PrintWriter printWriter = new PrintWriter(new FileOutputStream(new File(filename), true));
            StateIterator stateIterator = new StateIterator(getStateRepresentation(), operator.getMaxParallelism(),
                    ComputingInfrastructure.getInfrastructure(), getInputRateLevels());
            while (stateIterator.hasNext()) {
                State s = stateIterator.next();
                // print state line
                printWriter.println(s.dump());
                ActionIterator ait = new ActionIterator();
                while (ait.hasNext()) {
                    Action a = ait.next();
                    double v = this.qTable.getQ(s, a);
                    if (s.validateAction(a)) {
                        printWriter.print(String.format("%s\t%f\n", a.dump(), v));
                    }
                }
            }
            printWriter.flush();
            printWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void savePolicy()
    {
        this.qTable.dump(PolicyIOUtils.getFileForDumping(this.operator, "qTable"));

        //StateIterator stateIterator = new StateIterator(this.getStateRepresentation(), this.operator.getMaxParallelism(),
        //        ComputingInfrastructure.getInfrastructure(), this.getInputRateLevels());

        //long entries = 0;
        //while (stateIterator.hasNext()) {
        //    State state = stateIterator.next();
        //    ActionIterator actionIterator = new ActionIterator();

        //    while (actionIterator.hasNext()) {
        //        Action action = actionIterator.next();
        //        if (!validateAction(state, action))
        //            continue;

        //        entries++;
        //    }
        //}
        //System.out.println("Real entries: " + entries);
    }
}
