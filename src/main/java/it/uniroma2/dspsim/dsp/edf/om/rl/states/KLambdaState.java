package it.uniroma2.dspsim.dsp.edf.om.rl.states;

// TODO: the current class hierarchy should be revised
// There is no reason to keep this class distinct from State any more
public class KLambdaState extends State {

    public KLambdaState(int index, int[] k, int lambda, int maxLambda, int maxParallelism) {
        super(index, k, lambda, maxLambda, maxParallelism);
    }




}
