package it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.shape;

import it.uniroma2.dspsim.dsp.edf.om.fa.features.tiling.Tiling;
import it.uniroma2.dspsim.utils.Coordinate2D;
import it.uniroma2.dspsim.utils.Coordinate3D;
import it.uniroma2.dspsim.utils.Geometry2DUtils;

public class StripeTilingShape extends TilingShape {
    private int stripesNum;
    private int zTiles;

    public StripeTilingShape(int stripesNum) {
        this(stripesNum, 0);
    }

    public StripeTilingShape(int stripesNum, int zTiles) {
        this.stripesNum = stripesNum;
        this.zTiles = zTiles;
    }

    @Override
    public Coordinate3D map(Coordinate3D coordinate3D, Tiling tiling) {
        // get top left 2D tiling point
        Coordinate2D topLeftPoint = new Coordinate2D(tiling.getMinX(), tiling.getMaxY());
        // get bottom right 2D tiling point
        Coordinate2D bottomRightPoint = new Coordinate2D(tiling.getMaxX(), tiling.getMinY());

        // compute anti-diagonal length and direction (angle in radians)
        double antiDiagonalLength = Geometry2DUtils.pointPointDistance(topLeftPoint, bottomRightPoint);
        double antiDiagonalRadians = Geometry2DUtils.pointPointRadians(topLeftPoint, bottomRightPoint);

        // compute stripes distance
        double stripesDist = antiDiagonalLength / this.stripesNum;

        // determine stripe intersections with anti-diagonal foreach stripe
        Coordinate2D[] antiDiagonalIntersection = new Coordinate2D[this.stripesNum];
        for (int i = 0; i < this.stripesNum; i++) {
            antiDiagonalIntersection[i] = Geometry2DUtils.findPointFromPoint(topLeftPoint,
                    i * stripesDist, antiDiagonalRadians);
        }


        // get x and y parameters from 3D coordinate and compute
        // distance and direction between (x, y) and stripe foreach stripe
        double[] pointStripeDistances = new double[antiDiagonalIntersection.length];
        double[] pointStripeDirection = new double[antiDiagonalIntersection.length];
        for (int j = 0; j < antiDiagonalIntersection.length; j++) {
            pointStripeDistances[j] = Geometry2DUtils.pointLineDistance(
                    new Coordinate2D(coordinate3D.getX(), coordinate3D.getY()),
                    antiDiagonalIntersection[j],
                    antiDiagonalRadians + (1.0 / 2.0) * Math.PI);

            pointStripeDirection[j] = Geometry2DUtils.pointLineDirection(
                    new Coordinate2D(coordinate3D.getX(), coordinate3D.getY()),
                    antiDiagonalIntersection[j],
                    antiDiagonalRadians + (1.0 / 2.0) * Math.PI);
        }

        // find (x,y)'s closer stripe that belongs to 2nd or 3rd quadrant
        int minDistanceIndex = -1;
        for (int k = 0; k < pointStripeDistances.length; k++) {
            if ((minDistanceIndex == -1 || pointStripeDistances[k] < pointStripeDistances[minDistanceIndex]) &&
                    (pointStripeDirection[k] > (1.0 / 2.0) * Math.PI && pointStripeDirection[k] < (3.0 / 2.0) * Math.PI)) {
                minDistanceIndex = k;
            }
        }

        // get intersection between detected stripe and anti diagonal to map point to right tile
        final Coordinate2D stripe = antiDiagonalIntersection[minDistanceIndex];
        // add z to complete tile mapping building
        final int z = Geometry2DUtils.mapInIntervals(this.zTiles, tiling.getMinZ(), tiling.getMaxZ(), coordinate3D.getZ());

        return new Coordinate3D(stripe.getX(), stripe.getY(), z);
    }
}
