package it.uniroma2.dspsim.dsp.edf.om.rl.utils;

import it.uniroma2.dspsim.dsp.Operator;
import it.uniroma2.dspsim.dsp.edf.om.rl.State;
import it.uniroma2.dspsim.infrastructure.ComputingInfrastructure;

import java.util.Iterator;

public class StateIterator implements Iterator<State> {

    private int[] k;
    private int lambda;
    private int maxLambda;
    private int maxParallelism;
    private int lastKIndex;
    private int resourcesNumber;
    private int stateIndex;

    public StateIterator(Operator operator, ComputingInfrastructure infrastructure, int maxLambda) {
        this.resourcesNumber = infrastructure.getNodeTypes().length;
        this.k = new int[this.resourcesNumber];
        this.k[0] = 1; // avoid (0,0, ... , 1)
        this.lambda = -1;
        this.stateIndex = -1;
        this.maxLambda = maxLambda;
        this.maxParallelism = operator.getMaxParallelism();
        this.lastKIndex = 0;
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
        this.lambda++;
        if (this.lambda > this.maxLambda) {
            this.lambda = 0;

            goNextK();
        }

        return new State(this.stateIndex, this.k, lambda);
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
                    if (arraySum(this.k) <= this.maxParallelism)
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

    private int arraySum(int[] n) {
        int sum = 0;
        for (int value : n) sum += value;
        return sum;
    }
}
