import it.uniroma2.dspsim.dsp.edf.om.QLearningOM;
import it.uniroma2.dspsim.dsp.edf.om.rl.GuavaBasedQTable;
import it.uniroma2.dspsim.dsp.edf.om.rl.QTable;
import org.junit.Assert;
import org.junit.Test;

public class TestGuavaBasedQTable {

	@Test
	public void getNewValue()
	{
		final double INIT = 56;
		QTable qTable = new GuavaBasedQTable(INIT)	;

		QLearningOM.State s = new QLearningOM.State(4, 5);
		QLearningOM.Action a = new QLearningOM.Action(-1);

		double q = qTable.getQ(s, a);
		Assert.assertEquals(q, INIT, 0.1);
	}

	@Test
	public void putAndGetNewValue()
	{
		final double INIT = 56;
		QTable qTable = new GuavaBasedQTable(INIT)	;

		QLearningOM.State s = new QLearningOM.State(4, 5);
		QLearningOM.Action a = new QLearningOM.Action(-1);

		qTable.setQ(s, a, INIT);

		double q = qTable.getQ(s, a);
		Assert.assertEquals(q, INIT, 0.1);
	}
}
