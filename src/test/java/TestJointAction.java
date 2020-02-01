import it.uniroma2.dspsim.utils.JointActionIterator;
import org.junit.Test;

public class TestJointAction {

	@Test
	public void testIteration()
	{
		int N = 3;
		int omRcfCount[] = {1, 1 , 1, 1};
		int i;

		int nJointActions = 1;
		for (i=0; i<N; i++) {
			nJointActions *= 1+omRcfCount[i]; /* +1 to account for no reconf. */
		}
		System.out.println("Possible joint actions: " + nJointActions);

		JointActionIterator iterator = new JointActionIterator(N, omRcfCount);
		while (iterator.hasNext()) {
			System.out.println(iterator.next());
		}
	}
}
