package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.concrete.KLambdaResTypeTiling;
import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape.TilingShape;
import it.uniroma2.dspsim.utils.Tuple2;

public class TilingBuilder {
    private TilingShape shape;
    private Tuple2<Double, Double> xRange;
    private Tuple2<Double, Double> yRange;
    private Tuple2<Double, Double> zRange;

    public TilingBuilder setXRange(double[] range) {
        return setXRange(range[0], range[1]);
    }

    public TilingBuilder setXRange(double xMin, double xMax) {
        this.xRange = new Tuple2<>(xMin, xMax);
        return this;
    }

    public TilingBuilder setYRange(double[] range) {
        return setYRange(range[0], range[1]);
    }

    public TilingBuilder setYRange(double yMin, double yMax) {
        this.yRange = new Tuple2<>(yMin, yMax);
        return this;
    }

    public TilingBuilder setZRange(double[] range) {
        return setZRange(range[0], range[1]);
    }

    public TilingBuilder setZRange(double zMin, double zMax) {
        this.zRange = new Tuple2<>(zMin, zMax);
        return this;
    }

    public TilingBuilder setShape(TilingShape shape) {
        this.shape = shape;
        return this;
    }

    private void validate() {
        if (this.xRange == null || this.yRange == null)
            throw new IllegalArgumentException("xRange or yRange not declared");
        if (this.shape == null)
            throw new IllegalArgumentException("Shape not declared");
        if (this.zRange == null) {
            this.zRange = new Tuple2<>(0.0, 0.0);
        }
    }

    public Tiling build(TilingType type) {
        this.validate();
        switch (type) {
            default:
                return new KLambdaResTypeTiling(shape, xRange, yRange, zRange);
        }
    }
}
