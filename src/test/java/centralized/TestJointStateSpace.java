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
		int maxParallelism[] = {3,3, 2};
		JointStateIterator it = new JointStateIterator(maxParallelism.length, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			System.out.println(it.next());
		}
	}

	@Test
	public void testJointStateActionIterator()
	{
		ComputingInfrastructure.initDefaultInfrastructure(1);
		int maxParallelism[] = {3,2,2};
		JointStateIterator it = new JointStateIterator(maxParallelism.length, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);

		while (it.hasNext()) {
			JointState s = it.next();
			System.out.println(s);
			JointActionIterator ait = new JointActionIterator(maxParallelism.length);
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
		int maxParallelism[] = {3, 6, 7};
		JointStateIterator it;
		JointActionIterator ait;

		int counter = 0;

		JointQTable qTable = JointQTable.createQTable(maxParallelism.length, maxParallelism, 3);

		it = new JointStateIterator(maxParallelism.length, maxParallelism, ComputingInfrastructure.getInfrastructure(), 3);
		while (it.hasNext()) {
			JointState s = it.next();
			ait = new JointActionIterator(maxParallelism.length);
			while (ait.hasNext()) {
				JointAction a = ait.next();
				if (s.validateAction(a)) {
					qTable.setQ(s,a, qTable.getQ(s,a)+1.0);
					counter++;
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

		System.out.println("Counter: " + counter);
	}

	@Test
	public void testCountJointStates()
	{
		ComputingInfrastructure.initDefaultInfrastructure(1);
		for (int operators = 2; operators <= 4; ++operators) {
			for (int maxPar = 2; maxPar <= 5; ++maxPar) {
				for (int lambdaLevels = 5; lambdaLevels <= 10; ++lambdaLevels) {
					int maxParallelism[] = new int[operators];
					for (int i =0; i<operators; i++)
						maxParallelism[i] = maxPar;
					JointStateIterator it = new JointStateIterator(maxParallelism.length,
							maxParallelism, ComputingInfrastructure.getInfrastructure(), lambdaLevels);

					long statesCounter = 0;
					long actionsCounter = 0;
					while (it.hasNext()) {
						JointState s = it.next();
						// TODO: we could skip some states where lambdas increase
						++statesCounter;

						JointActionIterator ait = new JointActionIterator(maxParallelism.length);
						while (ait.hasNext()) {
							JointAction a = ait.next();
							if (s.validateAction(a)) {
								actionsCounter++;
							}
						}
					}
					StringBuilder ln = new StringBuilder();
					ln.append(String.format("%d\t%d\t%d\t", operators, maxPar, lambdaLevels));
					ln.append(statesCounter);
					ln.append('\t');
					ln.append(actionsCounter);
					ln.append('\t');
					ln.append(Math.multiplyExact(statesCounter, actionsCounter));
					System.out.println(ln.toString());
				}
			}
		}
	}
}
