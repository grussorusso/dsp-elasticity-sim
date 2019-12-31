package it.uniroma2.dspsim.dsp.edf.om.fa.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate3D;

public abstract class TilingShape {

    /**
     * get Tiling and transform its weights from 2D to 3D
     */
    public abstract void to3D(int zTiles);

    /**
     * map Coordinate3D given by (state, action) couple in a tile described by a Coordinate3D
     */
    public abstract Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling);
}
