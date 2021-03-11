package it.uniroma2.dspsim.dsp.edf.om.rl.states.factory;

import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.KLambdaState;

public class StateFactory {

    // avoid initialization
    private StateFactory() {}

    public static State createState(StateType stateType, int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        switch (stateType) {
            case K_LAMBDA:
                return new KLambdaState(index, k, lambda, maxLambda, maxParallelism);
            default:
                throw new IllegalArgumentException(
                        String.format("Type %s not known", stateType.toString())
                );
        }
    }
}
