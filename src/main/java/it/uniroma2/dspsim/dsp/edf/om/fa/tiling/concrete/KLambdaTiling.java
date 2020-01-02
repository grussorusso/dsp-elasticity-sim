package it.uniroma2.dspsim.dsp.edf.om.fa.tiling.concrete;

import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.Tiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.shape.TilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Tuple2;

public class KLambdaTiling extends Tiling {
    public KLambdaTiling(TilingShape shape, Tuple2<Double, Double> xRange, Tuple2<Double, Double> yRange) {
        super(shape, xRange, yRange);
    }

    public KLambdaTiling(TilingShape shape, Tuple2<Double, Double> xRange, Tuple2<Double, Double> yRange, Tuple2<Double, Double> zRange) {
        super(shape, xRange, yRange, zRange);
    }

    @Override
    public Coordinate3D sa2Coordinate3D(State state, Action action) {
        return null;
    }

    @Override
    public void updateWeight(double updateValue, Number... coordinate) {

    }

    @Override
    public int getWeightIndex(State state, Action action) {
        return 0;
    }
}
