package it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;

public class KLambdaState extends State {
    // k usage
    private int[] k;

    // lambda level
    private int lambda;

    public KLambdaState(int[] k, int lambda, Operator operator) {
        super(k, lambda, operator);

        this.k = k;
        this.lambda = lambda;
    }
}
