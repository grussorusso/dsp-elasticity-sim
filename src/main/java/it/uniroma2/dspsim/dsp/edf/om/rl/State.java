package it.uniroma2.dspsim.dsp.edf.om.rl;

public class State extends AbstractState {
    private int k[];
    private int lambda;

    public State (int[] k, int lambda) {
        this.k = k;
        this.lambda = lambda;
    }

    public int overallParallelism() {
        int p = 0;
        for (int _k : k) {
            p += _k;
        }

        return p;
    }

    public int[] getK() {
        return k;
    }

    public int getLambda() {
        return lambda;
    }
}
