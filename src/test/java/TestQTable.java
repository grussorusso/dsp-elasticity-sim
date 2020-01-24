import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.ArrayBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

public class TestQTable {

    @Test
    public void qTableHashingTest() {
        //guavaTableAdQTableHashingTest(3, 5);
        //guavaTableAdQTableHashingTest(5, 10);
        //guavaTableAdQTableHashingTest(10, 5);
        //guavaTableAdQTableHashingTest(7, 10);

        arrayBasedQTableHashingTest(10, 10);

        //doubleMatrixAsQTableHashingTest(3, 5);
        //doubleMatrixAsQTableHashingTest(5, 10);
        //doubleMatrixAsQTableHashingTest(10, 5);
        //doubleMatrixAsQTableHashingTest(10, 10);
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

    private void arrayBasedQTableHashingTest(int nodesNumber, int opMaxParallelism) {
        ComputingInfrastructure.initDefaultInfrastructure(nodesNumber);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0), opMaxParallelism);


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

        ArrayBasedQTable qTable = new ArrayBasedQTable(0.0, maxStateHash, maxActionHash);

        fillTableWithValue(qTable, 0.0, operator);
        fillTableWithValue(qTable, 1.0, operator);

        stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);

        long i=0;
        while (stateIterator.hasNext()) {
            State state = stateIterator.next();
            ActionIterator actionIterator = new ActionIterator();
            while (actionIterator.hasNext()) {
                Action action = actionIterator.next();
                Assert.assertEquals(1.0, qTable.getQ(state, action), 0.0);
                //System.out.println(qTable.getQ(state, action));
            }
            i++;
            System.err.println(i);
        }
    }

    private void guavaTableAdQTableHashingTest(int nodesNumber, int opMaxParallelism) {
        ComputingInfrastructure.initDefaultInfrastructure(nodesNumber);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0), opMaxParallelism);

        GuavaBasedQTable qTable = new GuavaBasedQTable(0.0);

        fillTableWithValue(qTable, 0.0, operator);
        fillTableWithValue(qTable, 1.0, operator);

        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);

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
            System.err.println(i);
        }
    }

    private void fillTableWithValue(QTable table, double value, Operator operator) {
        StateIterator stateIterator = new StateIterator(StateType.K_LAMBDA, operator.getMaxParallelism(),
                ComputingInfrastructure.getInfrastructure(), 30);

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
