package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Geometry2DUtils;

import java.io.Serializable;

public class RectangleTilingShape extends TilingShape implements Serializable  {

    protected int xTiles;
    protected int yTiles;
    protected int zTiles;

    public RectangleTilingShape(int xTiles, int yTiles) {
        this(xTiles, yTiles, 0);
    }

    public RectangleTilingShape(int xTiles, int yTiles, int zTiles) {
        this.xTiles = xTiles;
        this.yTiles = yTiles;
        this.zTiles = zTiles;
    }

    @Override
    public Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling) {
        final int x = Geometry2DUtils.mapInIntervals(this.xTiles, tiling.getMinX(), tiling.getMaxX(), coordinate3D.getX());
        final int y = Geometry2DUtils.mapInIntervals(this.yTiles, tiling.getMinY(), tiling.getMaxY(), coordinate3D.getY());
        final int z = Geometry2DUtils.mapInIntervals(this.zTiles, tiling.getMinZ(), tiling.getMaxZ(), coordinate3D.getZ());

        return new Coordinate3D(x, y, z);
    }
}
