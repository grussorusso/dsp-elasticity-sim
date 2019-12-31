package it.uniroma2.dspsim.dsp.edf.om.fa.tiling;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.shape.TilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.KeyValueStorage;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.DoubleMatrix;
import it.uniroma2.dspsim.utils.matrix.cube.Cube;
import it.uniroma2.dspsim.utils.matrix.cube.DoubleCube;

import java.util.List;

public abstract class Tiling extends Feature {
    private TilingShape shape;
    private double[] xRange;
    private double[] yRange;
    private double[] zRange;

    public Tiling(TilingShape shape, Tuple2<Double, Double> xRange, Tuple2<Double, Double> yRange) {
        this(shape, xRange, yRange, new Tuple2<>(0.0, 0.0));
    }

    public Tiling(TilingShape shape, Tuple2<Double, Double> xRange, Tuple2<Double, Double> yRange, Tuple2<Double, Double> zRange) {
        super();

        this.shape = shape;

        this.xRange = new double[2];
        this.yRange = new double[2];
        this.zRange = new double[2];

        this.xRange[0] = xRange.getK();
        this.xRange[1] = xRange.getV();

        this.yRange[0] = yRange.getK();
        this.yRange[1] = yRange.getV();

        this.zRange[0] = zRange.getK();
        this.zRange[1] = zRange.getV();
    }

    @Override
    protected void initWeights() {
        this.weights = new DoubleCube<Integer, Integer, Integer>(0.0);
    }

    @Override
    public double evaluate(State state, Action action) {
        if (contains(state, action))
            return 1.0;
        else
            return 0.0;
    }

    private boolean contains(State state, Action action) {
        Coordinate3D coordinate3D = sa2Coordinate3D(state, action);

        double x = coordinate3D.getX();
        double y = coordinate3D.getY();
        double z = coordinate3D.getZ();

        if ((x < xRange[0] || x >= xRange[1]) ||
                (y < yRange[0] || y >= yRange[1]) ||
                (is3D() && (z < zRange[0] || z >= zRange[1])))
            return false;

        return true;
    }

    private boolean is3D() {
        return zRange[0] != zRange[1];
    }

    public abstract Coordinate3D sa2Coordinate3D(State state, Action action);

    public TilingShape getShape() {
        return shape;
    }

    public double[] getxRange() {
        return xRange;
    }

    public double[] getyRange() {
        return yRange;
    }

    public double[] getzRange() {
        return zRange;
    }
}
