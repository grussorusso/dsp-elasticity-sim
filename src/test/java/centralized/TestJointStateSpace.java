package centralized;

import it.uniroma2.dspsim.dsp.edf.am.centralized.*;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;
import org.junit.Assert;
import org.junit.Test;

public class TestJointStateSpace {

	@Test
	public void testJointStateIterator()
	{
		ComputingInfrastructure.initDefaultInfrastructure(1);
		JointStateIterator it = new JointStateIterator(2, 3, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

	@Test
	public void testJointStateActionIterator()
	{
		ComputingInfrastructure.initDefaultInfrastructure(1);
		JointStateIterator it = new JointStateIterator(2, 3, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			JointState s = it.next();
			System.out.println(s);
			JointActionIterator ait = new JointActionIterator(2);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a))
					System.out.println(a);
			}
		}
	}

	@Test
	public void testJointQTable()
	{
		int maxAHash = -1;
		int maxSHash1 = -1;
		int maxSHash2 = -1;

		ComputingInfrastructure.initDefaultInfrastructure(1);
		JointStateIterator it = new JointStateIterator(2, 3, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			JointState s = it.next();
			maxSHash1 = Math.max(maxSHash1, s.getStates()[0].hashCode());
			maxSHash2 = Math.max(maxSHash2, s.getStates()[1].hashCode());
		}
		JointActionIterator ait = new JointActionIterator(2);
		while (ait.hasNext()) {
			JointAction a = ait.next();
			maxAHash = Math.max(maxAHash, a.getActions()[0].hashCode());
		}

		JointQTable qTable = new JointQTable(0.0, maxSHash1, maxSHash2, maxAHash);

		it = new JointStateIterator(2, 3, ComputingInfrastructure.getInfrastructure(), 3);
		while (it.hasNext()) {
			JointState s = it.next();
			ait = new JointActionIterator(2);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a)) {
					qTable.setQ(s,a, qTable.getQ(s,a)+1.0);
				}
			}
		}

		it = new JointStateIterator(2, 3, ComputingInfrastructure.getInfrastructure(), 3);
		while (it.hasNext()) {
			JointState s = it.next();
			ait = new JointActionIterator(2);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a)) {
					Assert.assertEquals(qTable.getQ(s,a), 1.0, 0.05);
				}
			}
		}
	}
}
