package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.StateType;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.factory.StateFactory;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.Arrays;
import java.util.Iterator;

public class StateIterator implements Iterator<State> {

    private int[] k;
    private int lambda;
    private int maxLambda;
    private int maxParallelism;
    private int lastKIndex;
    private int resourcesNumber;
    private int stateIndex;

    private StateType stateType;
    private State lastState;

    public StateIterator(StateType stateType, int maxParallelism, ComputingInfrastructure infrastructure, int maxLambda) {
        this.resourcesNumber = infrastructure.getNodeTypes().length;
        this.k = new int[this.resourcesNumber];
        this.k[0] = 1; // avoid (0,0, ... , 0)
        this.lambda = -1;
        this.stateIndex = -1;
        this.maxLambda = maxLambda;
        this.maxParallelism = maxParallelism;
        this.lastKIndex = 0;
        this.stateType = stateType;
    }

    /**
     * ITERATOR INTERFACE
     */

    @Override
    public boolean hasNext() {
        return !(this.lastKIndex == this.k.length - 1 &&
                this.k[this.lastKIndex] == this.maxParallelism &&
                this.lambda == this.maxLambda);
    }

    @Override
    public State next() {
        if (!hasNext())
            return null;

        this.stateIndex++;
        // go next state
        State nextState = this.nextState();

        // it's the first state, return it
        if (lastState != null)
            while (this.lastState.equals(nextState))
                nextState = this.nextState();

        this.lastState = nextState;
        return lastState;
    }

    private State nextState() {
        this.lambda++;
        if (this.lambda > this.maxLambda) {
            this.lambda = 0;

            goNextK();
        }

        return StateFactory.createState(this.stateType, this.stateIndex, this.k, this.lambda, this.maxLambda, this.maxParallelism);
    }

    private void goNextK() {
        if (this.k[this.lastKIndex] == this.maxParallelism) {
            this.k = resetK();
            this.lastKIndex++;
            // avoid index out of bound
            if (hasNext())
                this.k[this.lastKIndex] = 1;
        } else {
            for (int i = 0; i <= this.lastKIndex; i++) {
                if ((this.k[i] + 1) <= this.maxParallelism) {
                    this.k[i]++;
                    // check that sum of new k is less than max parallelism
                    if (Arrays.stream(k).sum() <= this.maxParallelism)
                        break;
                }
                // otherwise
                this.k[i] = 0;
            }
        }
    }

    private int[] resetK() {
        return new int[this.resourcesNumber];
    }
}
