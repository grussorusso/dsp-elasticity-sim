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

    public static State createState(StateType stateType, int[] k, int lambda, Operator operator) {
        switch (stateType) {
            case REDUCED:
                return new ReducedState(k, lambda, operator);
            case K_LAMBDA:
                return new KLambdaState(k, lambda, operator);
            case GENERAL_RESOURCES:
                return new GeneralResourcesState(k, lambda, operator);
            default:
                throw new IllegalArgumentException(
                        String.format("Type %s not known", stateType.toString())
                );
        }
    }
}
