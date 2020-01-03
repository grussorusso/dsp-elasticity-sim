package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate3D;

public abstract class TilingShape {

    /**
     * map Coordinate3D given by (state, action) couple in a tile described by a Coordinate3D
     */
    public abstract Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling);
}
