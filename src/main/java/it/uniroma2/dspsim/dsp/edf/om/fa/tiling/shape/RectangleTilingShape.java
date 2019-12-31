package it.uniroma2.dspsim.dsp.edf.om.fa.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate3D;

public class RectangleTilingShape extends TilingShape {

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
    public void to3D(int zTiles) {
        if (zTiles == 0) {
            this.zTiles = 0;
        } else {
            throw new IllegalArgumentException(
                    String.format("This tiling set has already 3D shape with %d z-tiles", this.zTiles)
            );
        }
    }

    @Override
    public Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling) {
        final double minX = tiling.getxRange()[0];
        final double maxX = tiling.getxRange()[1];
        final double minY = tiling.getyRange()[0];
        final double maxY = tiling.getyRange()[1];
        final double minZ = tiling.getzRange()[0];
        final double maxZ = tiling.getzRange()[1];

        final int x = (int) Math.ceil((this.xTiles / (maxX - minX)) * (coordinate3D.getX() - minX));
        final int y = (int) Math.ceil((this.yTiles / (maxY - minY)) * (coordinate3D.getY() - minY));
        final int z = (int) Math.ceil((this.zTiles / (maxZ - minZ)) * (coordinate3D.getZ() - minZ));

        return new Coordinate3D(x, y, z);
    }
}
