package it.uniroma2.dspsim.utils;

public class Geometry2DUtils {

    //avoid init
    private Geometry2DUtils() { }

    /**
     * Compute point to point distance
     * @param p1 first point
     * @param p2 second point
     * @return distance
     */
    public static double pointPointDistance(Coordinate2D p1, Coordinate2D p2) {
        double xDist = p1.getX() - p2.getX();
        double yDist = p1.getY() - p2.getY();
        return Math.sqrt((xDist * xDist) + (yDist * yDist));
    }

    /**
     * Compute point to point direction (using radians from p1 right direction, 0 rad)
     * @param p1 reference point
     * @param p2 other point
     * @return radians
     */
    public static double pointPointRadians(Coordinate2D p1, Coordinate2D p2) {
        // if p1 and p2 have same y it's a horizontal segment so direction in radians equals to 0 or pi
        if (p1.getY() == p2.getY()) {
            if (p1.getX() <= p2.getX()) return 0;
            else return Math.PI;
        }
        // if p1 and p2 have same x it's a vertical segment so direction in radians equals to pi/2 or 3/2 pi
        if (p1.getX() == p2.getX()) {
            if (p1.getY() < p2.getY()) return (1.0 / 2.0) * Math.PI;
            else if (p1.getY() > p2.getY()) return (3.0 / 2.0) * Math.PI;
            else return 0; // same point
        }
        // compute projection of the point with greater y on horizontal line passing from point with lower y
        Coordinate2D p3 = new Coordinate2D(p2.getX(), p1.getY());

        // compute length of cathetus built computing p3
        double c1 = pointPointDistance(p1, p3);
        double c2 = pointPointDistance(p2, p3);

        double atan = Math.atan(c2 / c1);

        switch (getQuadrant(p1, p2)) {
            case 1:
                return atan;
            case 2:
                return Math.PI - atan;
            case 3:
                return Math.PI + atan;
            case 4:
                return (2 * Math.PI) - atan;
            default:
                return 0;
        }
    }

    /**
     * Find 2D coordinates (x, y) from @point using @distance and @radians to find distance and direction
     * @param point starting point
     * @param distance distance from point
     * @param radians angle in radians (used to find direction)
     * @return computed point
     */
    public static Coordinate2D findPointFromPoint(Coordinate2D point, double distance, double radians) {
        // use distance as right triangle hypotenuse's length and radians as angle between
        // hypotenuse and cathetus built finding a point with same y of Coordinate2D point passed ad argument
        double c1 = distance * Math.cos(radians);
        double c2 = distance * Math.sin(radians);

        return new Coordinate2D(point.getX() + c1, point.getY() + c2);
    }

    /**
     * Compute distance between a point and a line
     * the line is determined by a point and a direction expressed in radians
     * @param point coordinate (x, y)
     * @param linePoint a point of the line
     * @param radians line slope relative to horizontal line passing from @linePoint
     * @return computed distance
     */
    public static double pointLineDistance(Coordinate2D point, Coordinate2D linePoint, double radians) {
        // get angle between line and point
        double theta = computeLinePointAngle(point, linePoint, radians);

        // compute distance from point to line using theta as angle
        // and distance between point and linePoint as hypotenuse
        double hypotenuse = pointPointDistance(point, linePoint);

        return hypotenuse * Math.sin(theta);
    }

    /**
     * Compute direction in radians between a point and a line
     * the line is determined by a point and a direction expressed in radians
     * @param point coordinate (x, y)
     * @param linePoint a point of the line
     * @param radians line slope relative to horizontal line passing from @linePoint
     * @return computed direction
     */
    public static double pointLineDirection(Coordinate2D point, Coordinate2D linePoint, double radians) {
        // get angle between line and point
        double theta = computeLinePointAngle(point, linePoint, radians);

        // compute third angle of a right triangle
        return (1.0 / 2.0) * Math.PI - theta;
    }

    /**
     * Compute angle between line and point in radians
     * @param point coordinate (x, y)
     * @param linePoint a point of the line
     * @param radians line slope relative to horizontal line passing from @linePoint
     * @return angle in radians
     */
    public static double computeLinePointAngle(Coordinate2D point, Coordinate2D linePoint, double radians) {
        // compute distance between point and linePoint and use it as right triangle hypotenuse
        double hypotenuse = pointPointDistance(point, linePoint);
        // compute angle between line point and point. Call it alpha
        double alpha = pointPointRadians(linePoint, point);
        // compute theta as angle between line and point using alpha and radians. Call it theta
        double theta = Math.abs(alpha - radians);
        // if theta greater than pi -> point is closer to other side of the line respect to linePoint
        if (theta > (1.0 / 2.0) * Math.PI) {
            theta = Math.PI - theta;
        }

        return theta;
    }

    /**
     * get quadrant position of p2 relative to p1
     * @param p1 reference point
     * @param p2 studied point
     * @return quadrant index {1, 2, 3, 4}
     */
    public static int getQuadrant(Coordinate2D p1, Coordinate2D p2) {
        // p2 is in first quadrant
        if (p1.getY() < p2.getY() && p1.getX() < p2.getX()) return 1;
        // p2 is in second quadrant
        else if (p1.getY() < p2.getY() && p1.getX() > p2.getX()) return 2;
        // p2 is in third quadrant
        else if (p1.getY() > p2.getY() && p1.getX() > p2.getX()) return 3;
        // p2 is in fourth quadrant
        else return 4;
    }
}