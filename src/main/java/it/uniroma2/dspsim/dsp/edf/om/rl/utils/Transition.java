package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

import java.util.Objects;

public class Transition {

	private State s;
	private Action a;
	private State nextS;
	private double reward;

	public Transition(State s, Action a, State nextS, double reward) {
		this.s = s;
		this.a = a;
		this.nextS = nextS;
		this.reward = reward;
	}

	public State getS() {
		return s;
	}

	public Action getA() {
		return a;
	}

	public State getNextS() {
		return nextS;
	}

	public double getReward() {
		return reward;
	}

	@Override
	public int hashCode() {
		return Objects.hash(s, a, nextS, reward);
	}
}
