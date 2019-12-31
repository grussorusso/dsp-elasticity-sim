package it.uniroma2.dspsim.dsp.edf.om.fa.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate3D;

public class StripeTilingShape extends TilingShape {
    private int stripesNum;

    private int xTiles;
    private int yTiles;
    private int zTiles;

    public StripeTilingShape(int stripesNum) {
        this(stripesNum, 0);
    }

    public StripeTilingShape(int stripesNum, int zTiles) {
        this.stripesNum = stripesNum;
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
        return null;
    }
}
