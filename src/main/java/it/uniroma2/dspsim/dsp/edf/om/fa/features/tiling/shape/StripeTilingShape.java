package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate2D;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Geometry2DUtils;

import java.io.Serializable;

public class StripeTilingShape extends TilingShape implements Serializable {
    private int stripesNum;
    private double stripesSlope;
    private int zTiles;

    public StripeTilingShape(int stripesNum, double stripesSlope) {
        this(stripesNum, stripesSlope, 0);
    }

    public StripeTilingShape(int stripesNum, double stripesSlope, int zTiles) {
        this.stripesNum = stripesNum;
        this.stripesSlope = stripesSlope;
        this.zTiles = zTiles;
    }

    @Override
    public Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling) {
        // compute q_min and q_max
        double q_max = this.stripesSlope > 0 ? tiling.getMaxY() - this.stripesSlope * tiling.getMinX()
                : tiling.getMaxY() - this.stripesSlope * tiling.getMaxX();

        double q_min = this.stripesSlope > 0 ? tiling.getMinY() - this.stripesSlope * tiling.getMaxX()
                : tiling.getMinY() - this.stripesSlope * tiling.getMinX();

        // compute current q from coordinate3D x and y
        // eq : y = m*x + q -> q = y - m*x
        double q = coordinate3D.getY() - this.stripesSlope * coordinate3D.getX();

        // compute difference between q_min and q_max adjusted on stripesNum (consider it as new coordinate's x)
        // eq : floor(abs(q_max - q_min) / stripesNum)
        // it represent a unique line in tiling's weights matrix
        // and foreach tiling that calls this method will be always the same
        int delta = Math.floorDiv((int) (q_max - q_min), this.stripesNum);

        // compute offset (consider it as new coordinate's y)
        // it represents the column of tiling's weights matrix
        // it is computed simply calculating difference between current q and q_min adjusted on delta
        int offset = Math.floorDiv((int) (q - q_min), delta);

        // compute z
        int z = Geometry2DUtils.mapInIntervals(this.zTiles, tiling.getMinZ(), tiling.getMaxZ(), coordinate3D.getZ());

        return new Coordinate3D(delta, offset, z);
    }

    @Override
    public int getTiles() {
        return this.stripesNum * (zTiles > 0? zTiles : 1);
    }
}
