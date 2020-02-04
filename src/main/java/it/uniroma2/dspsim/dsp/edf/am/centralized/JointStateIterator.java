package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

public class JointStateIterator {

	private StateIterator s1;
	private State state1;
	private StateIterator s2;
	private State state2;

	private int maxParallelism[];
	private ComputingInfrastructure infrastructure;
	private int lambdaLevels;

	public JointStateIterator (int nOperators, int maxParallelism[], ComputingInfrastructure infrastructure, int lambdaLevels)
	{
		if (nOperators != 2) {
			throw new RuntimeException("JointStateIterator only supports 2 operators");
		}

		this.maxParallelism = maxParallelism;
		this.infrastructure = infrastructure;
		this.lambdaLevels = lambdaLevels;

		s1 = new StateIterator(StateType.K_LAMBDA, maxParallelism[0], infrastructure, lambdaLevels);
		s2 = new StateIterator(StateType.K_LAMBDA, maxParallelism[1], infrastructure, lambdaLevels);
		state2 = s2.next();
	}

	public boolean hasNext()
	{
		return s1.hasNext() || s2.hasNext();
	}


	public JointState next()
	{
		if (!s1.hasNext()) {
			s1 = new StateIterator(StateType.K_LAMBDA, maxParallelism[0], infrastructure, lambdaLevels);
			state2 = s2.next();
		}

		state1 = s1.next();

		return new JointState (state1, state2);
	}


}
