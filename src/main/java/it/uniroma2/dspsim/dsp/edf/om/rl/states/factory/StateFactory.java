package it.uniroma2.dspsim.dsp.edf.om.rl.states.factory;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.GeneralResourcesState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.KLambdaState;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.concrete.ReducedState;

public class StateFactory {

    // avoid initialization
    private StateFactory() {}

    public static State createState(StateType stateType, int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        switch (stateType) {
            case REDUCED_K_LAMBDA:
                return new ReducedState(index, k, lambda, maxLambda, maxParallelism);
            case K_LAMBDA:
                return new KLambdaState(index, k, lambda, maxLambda, maxParallelism);
            case GENERAL_RESOURCES:
                return new GeneralResourcesState(index, k, lambda, maxLambda, maxParallelism);
            default:
                throw new IllegalArgumentException(
                        String.format("Type %s not known", stateType.toString())
                );
        }
    }
}
