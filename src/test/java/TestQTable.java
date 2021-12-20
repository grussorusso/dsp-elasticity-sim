import it.uniroma2.dspsim.Configuration;
import it.uniroma2.dspsim.ConfigurationKeys;
import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.FAQLearningOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.ArrayBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.MapBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.NeuralStateRepresentation;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class TestQTable {

    static private final int LAMBDA_LEVELS = 30;

    @Test
    public void qTableHashingTest() {
        //guavaTableAdQTableHashingTest(3, 5);
        //guavaTableAdQTableHashingTest(5, 10);
        //guavaTableAdQTableHashingTest(10, 5);
        //guavaTableAdQTableHashingTest(7, 10);

        arrayBasedQTableHashingTest(7, 10);
        printUsedMemoryMB();

        //doubleMatrixAsQTableHashingTest(3, 5);
        //doubleMatrixAsQTableHashingTest(5, 10);
        //doubleMatrixAsQTableHashingTest(10, 5);
        //doubleMatrixAsQTableHashingTest(10, 10);
    }

    private long getUsedMemoryMB() {
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) >> 20;
    }

    private void printUsedMemoryMB() {
        System.out.printf("Mem: %d\n", getUsedMemoryMB());
    }

    @Test
    public void qSizeTest() {
        int arrResTypes[] = {1,2,4,6, 8, 10};
        int parallelism[] = {10,20};

        for (int p : parallelism)  {
            for (int resTypes : arrResTypes) {
            ComputingInfrastructure.initDefaultInfrastructure(resTypes);
                long qEntries = 0;

                StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, p,
                        ComputingInfrastructure.getInfrastructure(), LAMBDA_LEVELS);
                while (stateIterator.hasNext()) {
                    State state = stateIterator.next();
                    //int h = state.hashCode();
                    ActionIterator actionIterator = new ActionIterator();
                    while (actionIterator.hasNext()) {
                        Action action = actionIterator.next();
                        //h = action.hashCode();
                        if (state.validateAction(action))
                            qEntries++;
                    }
                }

                // Comparison
                Operator operator = new Operator("rank",
                        new MG1OperatorQueueModel(1.0, 0.0), p);
                FAQLearningOM faq = new FAQLearningOM(operator);
                final int faFeatures = faq.getFeaturesCount();

                Configuration conf = Configuration.getInstance();
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPT_USE_RESOURCE_SET, "false");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_MINIMAL_INPUT_REPR, "true");
                // NOTE: configuration is used here:
                NeuralStateRepresentation repr = new NeuralStateRepresentation(p, LAMBDA_LEVELS, Configuration.getInstance());
                long weightsMinimal = TestNeuralNetworkDL4j.numWeights(3, repr);

                conf.setString(ConfigurationKeys.DL_OM_NETWORK_MINIMAL_INPUT_REPR, "false");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPR, "true");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_LAMBDA_ONE_HOT, "false");
                // NOTE: configuration is used here:
                repr = new NeuralStateRepresentation(p, LAMBDA_LEVELS, Configuration.getInstance());
                long weightsReduced = TestNeuralNetworkDL4j.numWeights(3, repr);

                conf.setString(ConfigurationKeys.DL_OM_NETWORK_MINIMAL_INPUT_REPR, "false");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPR, "true");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_LAMBDA_ONE_HOT, "true");
                // NOTE: configuration is used here:
                repr = new NeuralStateRepresentation(p, LAMBDA_LEVELS, Configuration.getInstance());
                long weightsReduced2 = TestNeuralNetworkDL4j.numWeights(3, repr);

                conf.setString(ConfigurationKeys.DL_OM_NETWORK_MINIMAL_INPUT_REPR, "false");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_REDUCED_K_REPR, "false");
                conf.setString(ConfigurationKeys.DL_OM_NETWORK_LAMBDA_ONE_HOT, "true");
                // NOTE: configuration is used here:
                repr = new NeuralStateRepresentation(p, LAMBDA_LEVELS, Configuration.getInstance());
                long weightsLarge = TestNeuralNetworkDL4j.numWeights(3, repr);

                String out = String.format("%d;%d;%d;%d;%d;%d;%d;%d", resTypes, p, qEntries, faFeatures, weightsMinimal, weightsReduced, weightsReduced2, weightsLarge);
                System.out.println(out);
            }
        }
    }

    private  double usedMemory(long params) {
        return (double)params/1024.0/1024.0*Double.BYTES;
    }

    private void doubleMatrixAsQTableHashingTest(int nodesNumber, int opMaxParallelism) {
        ComputingInfrastructure.initDefaultInfrastructure(nodesNumber);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0),opMaxParallelism);

        DoubleMatrix<Integer, Integer> qTable = new DoubleMatrix<>(0.0);

        fillMatrixWithValue(qTable, 0.0, operator);
        fillMatrixWithValue(qTable, 1.0, operator);

        for (Integer x : qTable.getRowLabels()) {
            for (Integer y : qTable.getColLabels(x)) {
                Assert.assertEquals(1.0, qTable.getValue(x, y), 0.0);
                System.out.println(qTable.getValue(x, y));
            }
        }
    }

    @Test
    public void testDumpingAndLoading ()
    {
        ComputingInfrastructure.initDefaultInfrastructure(3);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0), 6);


        int maxActionHash = -1;
        int maxStateHash = -1;
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            int h = state.hashCode();
            if (h  > maxStateHash)
                maxStateHash = h;
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                h = action.hashCode();
                if (h > maxActionHash)
                    maxActionHash = h;
            }
        }

        QTable qTable = new ArrayBasedQTable(4.0, maxStateHash, maxActionHash);
        qTable.dump(new File("/tmp/provaQ.ser"));
        QTable qTable2 = new ArrayBasedQTable(0.0, maxStateHash, maxActionHash);
        qTable2.load(new File("/tmp/provaQ.ser"));

        stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                Assert.assertEquals(4.0, qTable.getQ(state, action), 0.0);
                //System.out.println(qTable.getQ(state, action));
            }
        }

        qTable = new MapBasedQTable(4.0);
        stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                qTable.setQ(state, action, 4.0);
            }
        }
        qTable.dump(new File("/tmp/provaQ.ser"));
        qTable2 = new MapBasedQTable(0.0);
        qTable2.load(new File("/tmp/provaQ.ser"));

        stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                Assert.assertEquals(4.0, qTable.getQ(state, action), 0.0);
                //System.out.println(qTable.getQ(state, action));
            }
        }
    }


    private void arrayBasedQTableHashingTest(int nodesNumber, int opMaxParallelism) {
        ComputingInfrastructure.initDefaultInfrastructure(nodesNumber);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0), opMaxParallelism);


        int maxActionHash = -1;
        int maxStateHash = -1;
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), LAMBDA_LEVELS);
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            int h = state.hashCode();
            if (h  > maxStateHash)
                maxStateHash = h;
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                if (!state.validateAction(action))
                    continue;
                h = action.hashCode();
                if (h > maxActionHash)
                    maxActionHash = h;
            }
        }

        ArrayBasedQTable qTable = new ArrayBasedQTable(0.0, maxStateHash, maxActionHash);

        fillTableWithValue(qTable, 0.0, operator);
        fillTableWithValue(qTable, 1.0, operator);

        stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), LAMBDA_LEVELS);

        long i=0;
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                if (!state.validateAction(action))
                    continue;
                Assert.assertEquals(1.0, qTable.getQ(state, action), 0.0);
                //System.out.println(qTable.getQ(state, action));
            }
            i++;
        }
    }

    private void guavaTableAdQTableHashingTest(int nodesNumber, int opMaxParallelism) {
        ComputingInfrastructure.initDefaultInfrastructure(nodesNumber);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0), opMaxParallelism);

        MapBasedQTable qTable = new MapBasedQTable(0.0);

        fillTableWithValue(qTable, 0.0, operator);
        fillTableWithValue(qTable, 1.0, operator);

        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), LAMBDA_LEVELS);

        long i = 0;
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                Assert.assertEquals(1.0, qTable.getQ(state, action), 0.0);
                //System.out.println(qTable.getQ(state, action));
            }
            i++;
            //System.err.println(i);
        }
    }

    private void fillTableWithValue(QTable table, double value, Operator operator) {
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), LAMBDA_LEVELS);

        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                table.setQ(state, action, value);
            }
        }
    }

    private void fillMatrixWithValue(DoubleMatrix<Integer, Integer> table, double value, Operator operator) {
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);

        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                table.add(state.hashCode(), action.hashCode(), value);
            }
        }
    }

}
