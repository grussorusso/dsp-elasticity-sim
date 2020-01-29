package it.uniroma2.dspsim.dsp.edf.om;

import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicy;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyCallback;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.ActionSelectionPolicyType;
import it.uniroma2.dspsim.dsp.edf.om.rl.action_selection.factory.ActionSelectionPolicyFactory;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.stats.Statistics;
import it.uniroma2.dspsim.stats.metrics.CpuMetric;
import it.uniroma2.dspsim.stats.metrics.MemoryMetric;
import it.uniroma2.dspsim.stats.metrics.TimeMetric;
import it.uniroma2.dspsim.stats.samplers.StepSampler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class ValueIterationOM extends DynamicProgrammingOM implements ActionSelectionPolicyCallback {

    private static final String STAT_VI_MEMORY_USAGE = "VI Memory Usage";
    private static final String STAT_VI_STEP_TIME = "VI Step Time";
    private static final String STAT_VI_CPU_USAGE = "VI CPU Usage";

    // V matrix
    protected QTable qTable;

    public ValueIterationOM(Operator operator) {
        super(operator);

        Configuration configuration = Configuration.getInstance();

        int maxIterations = configuration.getInteger(ConfigurationKeys.VI_MAX_ITERATIONS_KEY, 0);
        long maxTimeMillis = configuration.getLong(ConfigurationKeys.VI_MAX_TIME_SECONDS_KEY, 60L) * 1000;
        double theta = configuration.getDouble(ConfigurationKeys.VI_THETA_KEY, 1E-5);

        valueIteration(maxIterations, maxTimeMillis, theta);

        dumpQOnFile(String.format("%s/%s/%s/policy",
                Configuration.getInstance().getString(ConfigurationKeys.OUTPUT_BASE_PATH_KEY, ""),
                Configuration.getInstance().getString(ConfigurationKeys.OM_TYPE_KEY, ""),
                "others"));
    }

    @Override
    protected void registerMetrics(Statistics statistics) {
        super.registerMetrics(statistics);

        StepSampler stepSampler = new StepSampler("VI Step Sampler", 1);

        // per operator metrics
        MemoryMetric opMemoryMetric = new MemoryMetric(getOperatorMetricName(STAT_VI_MEMORY_USAGE));
        //opMemoryMetric.addSampler(stepSampler);
        statistics.registerMetric(opMemoryMetric);
        TimeMetric opTimeMetric = new TimeMetric(getOperatorMetricName(STAT_VI_STEP_TIME));
        //opTimeMetric.addSampler(stepSampler);
        statistics.registerMetric(opTimeMetric);
        CpuMetric opCpuMetric = new CpuMetric(getOperatorMetricName(STAT_VI_CPU_USAGE));
        //opCpuMetric.addSampler(stepSampler);
        statistics.registerMetric(opCpuMetric);

        // global metrics
        MemoryMetric memoryMetric = new MemoryMetric(STAT_VI_MEMORY_USAGE);
        memoryMetric.addSampler(stepSampler);
        statistics.registerMetric(memoryMetric);
        TimeMetric timeMetric = new TimeMetric(STAT_VI_STEP_TIME);
        timeMetric.addSampler(stepSampler);
        statistics.registerMetric(timeMetric);
        CpuMetric CpuMetric = new CpuMetric(STAT_VI_CPU_USAGE);
        CpuMetric.addSampler(stepSampler);
        statistics.registerMetric(CpuMetric);
    }

    /**
     * VALUE ITERATION ALGORITHM
     * @param maxIterations max iterations of the algorithm, if maxIteration is greater than 0 it will be stopped
     *                      even if delta is still greater than theta
     * @param theta accuracy threshold
     */
    private void valueIteration(int maxIterations, long maxTimeMillis, double theta) {
        int stepsCounter = 0;
        long startIterationTime = 0L;
        double delta = Double.POSITIVE_INFINITY;
        while (delta > theta && (maxIterations < 1 || stepsCounter < maxIterations) && maxTimeMillis > 0) {
            if (maxTimeMillis > 0L)
                startIterationTime = System.currentTimeMillis();

            delta = vi();


            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_VI_STEP_TIME), 0);
            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_VI_MEMORY_USAGE), 0);
            Statistics.getInstance().updateMetric(getOperatorMetricName(STAT_VI_CPU_USAGE), 0);
            Statistics.getInstance().updateMetric(STAT_VI_STEP_TIME, 0);
            Statistics.getInstance().updateMetric(STAT_VI_MEMORY_USAGE, 0);
            Statistics.getInstance().updateMetric(STAT_VI_CPU_USAGE, 0);

            if (maxTimeMillis > 0L)
                maxTimeMillis -= (System.currentTimeMillis() - startIterationTime);
            // if max iterations is greater than 0 increment counter
            if (maxIterations > 0) {
                stepsCounter++;
                if (stepsCounter == maxIterations) {
                    break;
                }
            }
            System.out.println(delta);
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
    	this.qTable = new GuavaBasedQTable(0.0);
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
}
