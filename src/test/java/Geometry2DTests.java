import it.uniroma2.dspsim.utils.Coordinate2D;
import it.uniroma2.dspsim.utils.Geometry2DUtils;
import org.junit.Assert;
import org.junit.Test;

public class Geometry2DTests {

    @Test
    public void pointPointRadians45Degrees() {
        Coordinate2D p1 = new Coordinate2D(1.0, 1.0);
        Coordinate2D p2 = new Coordinate2D(2.0, 2.0);

        pointPointRadians(p1, p2, Math.PI / 4.0);
    }

    @Test
    public void pointPointRadians90Degrees() {
        Coordinate2D p1 = new Coordinate2D(1.0, 1.0);
        Coordinate2D p2 = new Coordinate2D(1.0, 2.0);

        pointPointRadians(p1, p2, Math.PI / 2.0);
    }

    @Test
    public void pointPointRadians225Degrees() {
        Coordinate2D p1 = new Coordinate2D(1.0, 1.0);
        Coordinate2D p2 = new Coordinate2D(-2.0, -2.0);

        pointPointRadians(p1, p2, (5.0 / 4.0) * Math.PI);
    }


    @Test
    public void pointPointRadians315Degrees() {
        Coordinate2D p1 = new Coordinate2D(1.0, 1.0);
        Coordinate2D p2 = new Coordinate2D(2.0, 0.0);

        pointPointRadians(p1, p2, (7.0 / 4.0) * Math.PI);
    }

    private void pointPointRadians(Coordinate2D p1, Coordinate2D p2, double radians) {
        double r = Geometry2DUtils.pointPointRadians(p1, p2);

        Assert.assertEquals(radians, r, 0.0);
    }

    @Test
    public void pointPointDistance() {
        Coordinate2D p1 = new Coordinate2D(1.0, 3.5);
        Coordinate2D p2 = new Coordinate2D(2.0, -2.6);

        double dist = Geometry2DUtils.pointPointDistance(p1, p2);

        Assert.assertEquals(6.19, dist, 0.01);
    }

    @Test
    public void findPointsFromPoint() {
        Coordinate2D p = new Coordinate2D(1.0, 1.0);

        Coordinate2D p1 = Geometry2DUtils.findPointFromPoint(p, 3, 0);

        Assert.assertEquals(4.0, p1.getX(), 0.0);
        Assert.assertEquals(1.0, p1.getY(), 0.0);

        Coordinate2D p2 = Geometry2DUtils.findPointFromPoint(p, Math.sqrt(18), (7.0 / 4.0) * Math.PI);

        Assert.assertEquals(4.0, p2.getX(), 0.0001);
        Assert.assertEquals(-2.0, p2.getY(), 0.0001);

        Coordinate2D p3 = Geometry2DUtils.findPointFromPoint(p, Math.sqrt(18), (1.0 / 4.0) * Math.PI);

        Assert.assertEquals(4.0, p3.getX(), 0.0001);
        Assert.assertEquals(4.0, p3.getY(), 0.0001);
    }

    @Test
    public void pointLineDistance() {
        Coordinate2D topLeftPoint = new Coordinate2D(3.0, 3.0);
        Coordinate2D bottomRightPoint = new Coordinate2D(7.0, -1.0);

        Coordinate2D p1 = new Coordinate2D(4.0, 2.0);
        // check p1
        Coordinate2D computedP1 = Geometry2DUtils.findPointFromPoint(topLeftPoint, Math.sqrt(2), (7.0 / 4.0) * Math.PI);
        Assert.assertEquals(computedP1.getX(), p1.getX(), 0.0001);
        Assert.assertEquals(computedP1.getY(), p1.getY(), 0.0001);

        Coordinate2D p2 = new Coordinate2D(5.0, 1.0);
        // check p1
        Coordinate2D computedP2 = Geometry2DUtils.findPointFromPoint(bottomRightPoint, Math.sqrt(8), (3.0 / 4.0) * Math.PI);
        Assert.assertEquals(computedP2.getX(), p2.getX(), 0.0001);
        Assert.assertEquals(computedP2.getY(), p2.getY(), 0.0001);

        Coordinate2D p3 = new Coordinate2D(6.0, 0.0);
        // check p1
        Coordinate2D computedP3 = Geometry2DUtils.findPointFromPoint(topLeftPoint, Math.sqrt(18), (7.0 / 4.0) * Math.PI);
        Assert.assertEquals(computedP3.getX(), p3.getX(), 0.0001);
        Assert.assertEquals(computedP3.getY(), p3.getY(), 0.0001);

        Coordinate2D point1 = new Coordinate2D(6.5, 1.5);
        Coordinate2D point2 = new Coordinate2D(3.0, 0.0);

        // point 1 line p2 distance and radians
        double d1 = Geometry2DUtils.pointLineDistance(point1, p2, (1.0 / 4.0) * Math.PI);
        double r1 = Geometry2DUtils.pointLineDirection(point1, p2, (1.0 / 4.0) * Math.PI);
        Assert.assertEquals(Math.sqrt(0.5), d1, 0.0001);
        Assert.assertEquals((3.0 / 4.0) * Math.PI, r1, 0.8);

        // point 1 line p3 distance and radians
        double d2 = Geometry2DUtils.pointLineDistance(point1, p3, (1.0 / 4.0) * Math.PI);
        double r2 = Geometry2DUtils.pointLineDirection(point1, p3, (1.0 / 4.0) * Math.PI);
        Assert.assertEquals(Math.sqrt(0.5), d2, 0.0001);
        Assert.assertEquals((7.0 / 4.0) * Math.PI, r2, 0.8);

        // point 2 line p1 distance and radians
        double d3 = Geometry2DUtils.pointLineDistance(point2, p1, (1.0 / 4.0) * Math.PI);
        double r3 = Geometry2DUtils.pointLineDirection(point2, p1, (1.0 / 4.0) * Math.PI);
        Assert.assertEquals(Math.sqrt(0.5), d3, 0.0001);
        Assert.assertEquals((3.0 / 4.0) * Math.PI, r3, 0.8);

        // point 2 line p2 distance and radians
        double d4 = Geometry2DUtils.pointLineDistance(point2, p2, (1.0 / 4.0) * Math.PI);
        double r4 = Geometry2DUtils.pointLineDirection(point2, p2, (1.0 / 4.0) * Math.PI);
        Assert.assertEquals(Math.sqrt(0.5), d4, 0.0001);
        Assert.assertEquals((7.0 / 4.0) * Math.PI, r4, 0.8);

        // point 1 line p1 distance and radians
        double d5 = Geometry2DUtils.pointLineDistance(point1, p1, (1.0 / 4.0) * Math.PI);
        double r5 = Geometry2DUtils.pointLineDirection(point1, p1, (1.0 / 4.0) * Math.PI);
        Assert.assertEquals(Math.sqrt(4.5), d5, 0.0001);
        Assert.assertEquals((3.0 / 4.0) * Math.PI, r5, 0.8);
    }
}
