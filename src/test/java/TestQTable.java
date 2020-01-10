import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.RLQLearningOM;
import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.ValueIterationOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.ActionIterator;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.dsp.queueing.MG1OperatorQueueModel;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import org.junit.Assert;
import org.junit.Test;

import java.util.Iterator;

public class TestQTable {

    @Test
    public void GuavaTableAsQTableHashingTest() {
        ComputingInfrastructure.initDefaultInfrastructure(3);
        Operator operator = new Operator("rank", new MG1OperatorQueueModel(1.0, 0.0),5);

        DoubleMatrix<Integer, Integer> qTable = new DoubleMatrix<>(0.0);

        fillTableWithValue(qTable, 0.0, operator);
        fillTableWithValue(qTable, 1.0, operator);

        for (Integer x : qTable.getRowLabels()) {
            for (Integer y : qTable.getColLabels(x)) {
                Assert.assertEquals(1.0, qTable.getValue(x, y), 0.0);
                System.out.println(qTable.getValue(x, y));
            }
        }
    }

    private void fillTableWithValue(DoubleMatrix<Integer, Integer> table, double value, Operator operator) {
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
