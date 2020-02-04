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
		int maxParallelism[] = {3,3};
		JointStateIterator it = new JointStateIterator(2, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

	@Test
	public void testJointStateActionIterator()
	{
		ComputingInfrastructure.initDefaultInfrastructure(1);
		int maxParallelism[] = {3,3};
		JointStateIterator it = new JointStateIterator(2, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);

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
		ComputingInfrastructure.initDefaultInfrastructure(1);
		int maxParallelism[] = {3,3};
		JointStateIterator it;
		JointActionIterator ait;

		JointQTable qTable = JointQTable.createQTable(maxParallelism.length, maxParallelism, 3);

		it = new JointStateIterator(maxParallelism.length, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);
		while (it.hasNext()) {
			JointState s = it.next();
			ait = new JointActionIterator(maxParallelism.length);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a)) {
					qTable.setQ(s,a, qTable.getQ(s,a)+1.0);
				}
			}
		}

		it = new JointStateIterator(maxParallelism.length, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);
		while (it.hasNext()) {
			JointState s = it.next();
			ait = new JointActionIterator(maxParallelism.length);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a)) {
					Assert.assertEquals(qTable.getQ(s,a), 1.0, 0.05);
				}
			}
		}
	}
}
