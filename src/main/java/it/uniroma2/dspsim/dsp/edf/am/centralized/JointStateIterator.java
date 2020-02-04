package it.uniroma2.dspsim.dsp.edf.am.centralized;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateIterator;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.ArrayList;
import java.util.Arrays;

public class JointStateIterator {

	private State[] states;
	private StateIterator[] stateIterators;

	private int maxParallelism[];
	private ComputingInfrastructure infrastructure;
	private int lambdaLevels;

	public JointStateIterator (int nOperators, int maxParallelism[], ComputingInfrastructure infrastructure, int lambdaLevels)
	{
		this.maxParallelism = maxParallelism;
		this.infrastructure = infrastructure;
		this.lambdaLevels = lambdaLevels;

		this.stateIterators = new StateIterator[nOperators];
		this.states = new State[nOperators];

		for (int i = 0; i<nOperators; i++) {
			stateIterators[i] = new StateIterator(StateType.K_LAMBDA, maxParallelism[i], infrastructure, lambdaLevels);
		}
		for (int i = 1; i<nOperators; i++) {
			// 0 is skipped
			states[i] = stateIterators[i].next();
		}
	}

	public boolean hasNext()
	{
		for (StateIterator si : stateIterators) {
			if (si.hasNext())
				return true;
		}

		return false;
	}


	private void resetIterator (int i) {
		stateIterators[i] = new StateIterator(StateType.K_LAMBDA, maxParallelism[i], infrastructure, lambdaLevels);
		states[i] = stateIterators[i].next();
	}

	public JointState next()
	{
		int i = 0;
		boolean done = false;

		while (!done && i < states.length) {
			if (stateIterators[i].hasNext()) {
				states[i] = stateIterators[i].next();
				done = true;
			} else {
				resetIterator(i);
				i++;
			}
		}

		return new JointState (states.clone());
	}


}
