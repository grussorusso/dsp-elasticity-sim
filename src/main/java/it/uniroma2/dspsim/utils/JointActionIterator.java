package it.uniroma2.dspsim.utils;

public class JointActionIterator {

	/* Number of OMs. */
	private int N;

	/* Reconfigurations requested by each OM. */
	private int omRcfCount[];

	private int actionCounter;
	private int nJointActions;
	private int a[];

	public JointActionIterator (int numOM, int omRcfCount[])
	{
		this.N = numOM;
		this.omRcfCount = omRcfCount;
		this.actionCounter = 0;

		nJointActions = 1;
		for (int i=0; i<N; i++) {
			nJointActions *= 1+omRcfCount[i]; /* +1 to account for no reconf. */
		}
	}

	public boolean hasNext()
	{
		return actionCounter < nJointActions;
	}

	public int[] next()
	{
		if (actionCounter == 0) {
			a = new int[N];
			for (int i=0; i<N; i++) {
				 a[i] = 0;
			}
		} else {
			/* next action */
			boolean incremented = false;
			int j = 0;
			while (j < N && !incremented) {
				if (a[j] < omRcfCount[j]) {
					a[j]++;
					incremented = true;
				} else {
					a[j] = 0;
				}
				j++;
			}
		}

		++actionCounter;
		return a;
	}
}
