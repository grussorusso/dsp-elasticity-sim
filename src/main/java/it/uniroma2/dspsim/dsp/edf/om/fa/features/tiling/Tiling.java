package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling;

import it.uniroma2.dspsim.dsp.edf.om.RewardBasedOM;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.Feature;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.TilingShape;
import it.uniroma2.dspsim.dsp.edf.om.rl.Action;
import it.uniroma2.dspsim.dsp.edf.om.rl.states.State;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Tuple2;
import it.uniroma2.dspsim.utils.matrix.Matrix;
import it.uniroma2.dspsim.utils.matrix.cube.DoubleCube;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class Tiling extends Feature implements Serializable {
    private TilingShape shape;
    private double[] xRange;
    private double[] yRange;
    private double[] zRange;

    private DoubleCube<Integer, Integer, Integer> weights;

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
        this.weights = new DoubleCube<>(0.0);
    }

    @Override
    public boolean isActive(State state, Action action, RewardBasedOM om) {
        return contains(state, action, om);
    }

    @Override
    public double evaluate(State state, Action action, RewardBasedOM om) {
        Coordinate3D coordinate3D = sa2Coordinate3D(state, action, om);
        Coordinate3D weightCoordinate = this.shape.map(coordinate3D, this);
        return this.weights.getValue((int) weightCoordinate.getX(), (int) weightCoordinate.getY(), (int) weightCoordinate.getZ());
    }

    @Override
    protected void updateWeight(double updateValue, State state, Action action, RewardBasedOM om) {
        Coordinate3D coordinate3D = sa2Coordinate3D(state, action, om);
        Coordinate3D weightCoordinate = this.shape.map(coordinate3D, this);
        double newWeightValue = this.weights.getValue((int) weightCoordinate.getX(), (int) weightCoordinate.getY(), (int) weightCoordinate.getZ()) + updateValue;
        this.weights.setValue((int) weightCoordinate.getX(), (int) weightCoordinate.getY(), (int) weightCoordinate.getZ(), newWeightValue);
    }

    @Override
    public List<Tuple2<Object, Double>> getWeights() {
        List<Tuple2<Object, Double>> weights = new ArrayList<>();
        for (Tuple2<Integer, Matrix<Integer, Integer, Double>> z : this.weights.get2DSections()) {
            for (Integer x : z.getV().getRowLabels()) {
                for (Integer y : z.getV().getColLabels(x)) {
                    Coordinate3D coordinate3D = new Coordinate3D(x , y, z.getK());
                    Double value = z.getV().getValue(x, y);
                    weights.add(new Tuple2<>(coordinate3D, value));
                }
            }
        }
        return weights;
    }

    private boolean contains(State state, Action action, RewardBasedOM om) {
        Coordinate3D coordinate3D = sa2Coordinate3D(state, action, om);

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

    public abstract Coordinate3D sa2Coordinate3D(State state, Action action, RewardBasedOM om);

    public double getMinX() { return xRange[0]; }
    public double getMaxX() { return xRange[1]; }
    public double getMinY() { return yRange[0]; }
    public double getMaxY() { return yRange[1]; }
    public double getMinZ() { return zRange[0]; }
    public double getMaxZ() { return zRange[1]; }
}
