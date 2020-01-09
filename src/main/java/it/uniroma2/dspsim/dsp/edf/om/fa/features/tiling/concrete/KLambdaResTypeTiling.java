package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.concrete;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.TilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.dsp.edf.om.rl.utils.StateUtils;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.MathUtils;
import it.uniroma2.dspsim.utils.Tuple2;

public class KLambdaResTypeTiling extends Tiling {
    public KLambdaResTypeTiling(TilingShape shape, Tuple2<Double, Double> xRange, Tuple2<Double, Double> yRange) {
        super(shape, xRange, yRange);
    }

    public KLambdaResTypeTiling(TilingShape shape, Tuple2<Double, Double> xRange,
                                Tuple2<Double, Double> yRange, Tuple2<Double, Double> zRange) {
        super(shape, xRange, yRange, zRange);
    }

    @Override
    public Coordinate3D sa2Coordinate3D(State state, Action action, RewardBasedOM om) {
        State pds = StateUtils.computePostDecisionState(state, action, om);

        // create actual deployment mask
        // revert mask array to consider it a base 2 number
        int[] deploymentNumber = new int[pds.getActualDeployment().length];
        // TODO occhio potrebbe essere sbagliato devi convertire in one hot vector e poi in decimale?
        for (int i = 0; i < pds.getActualDeployment().length; i++) {
            if (pds.getActualDeployment()[pds.getActualDeployment().length - 1 - i] > 0)
                deploymentNumber[i] = 1;
            else
                deploymentNumber[i] = 0;
        }

        double x = pds.overallParallelism();
        double y = pds.getLambda();
        double z = MathUtils.toBase10(deploymentNumber, 2);

        return new Coordinate3D(x, y, z);
    }
}
