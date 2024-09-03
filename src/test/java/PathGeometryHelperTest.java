import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.geometry.BoundingBox;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathGeometryHelper;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class PathGeometryHelperTest {
    @Test
    public void testPathGeometry() {
        // Arrange
        Path p = new Path(Arrays.asList(new Point(0, 0),
                new Point(-2, 5),
                new Point(4, 4),
                new Point(5, 5),
                new Point(155, -255),
                new Point(-9, 9)
        ));
        PathGeometryHelper o = new PathGeometryHelper(p, p.continousCurve(point -> true), 0);

        // Act
        ArrayList<Point> curve = p.continousCurve(point -> true);
        BoundingBox curveBox = AxisAlignedBoundingBox2d.fromPoints(curve);

        // Assert
        for (Point point : curve) {
            assertTrue(curveBox.contains(point), "BoundingBox does not contain point: " + point);
            assertTrue(o.contains(point), "PathGeometryHelper does not contain point: " + point);
        }
    }

    @Test
    public void testExpanding() {
        BoundingBox bbx = new AxisAlignedBoundingBox2d(new Point(-100, -200), new Point(200, 500));
        assertTrue(bbx.contains(new Point(-100, -200)));
        assertTrue(bbx.contains(new Point(200, 500)));

        assertFalse(bbx.contains(new Point(-101, -200)));
        assertFalse(bbx.contains(new Point(-100, -201)));
        assertFalse(bbx.contains(new Point(-101, -201)));

        assertFalse(bbx.contains(new Point(201, 500)));
        assertFalse(bbx.contains(new Point(200, 501)));
        assertFalse(bbx.contains(new Point(201, 501)));

        bbx = bbx.expand(1);
        assertTrue(bbx.contains(new Point(-101, -200)));
        assertTrue(bbx.contains(new Point(-100, -201)));
        assertTrue(bbx.contains(new Point(-101, -201)));

        assertTrue(bbx.contains(new Point(201, 500)));
        assertTrue(bbx.contains(new Point(200, 501)));
        assertTrue(bbx.contains(new Point(201, 501)));

        bbx = bbx.expand(-2);
        assertFalse(bbx.contains(new Point(-101, -200)));
        assertFalse(bbx.contains(new Point(-100, -201)));
        assertFalse(bbx.contains(new Point(-101, -201)));

        assertFalse(bbx.contains(new Point(201, 500)));
        assertFalse(bbx.contains(new Point(200, 501)));
        assertFalse(bbx.contains(new Point(201, 501)));

        assertFalse(bbx.contains(new Point(-100, -200)));
        assertFalse(bbx.contains(new Point(200, 500)));
    }

    @Test
    public void parentageTest() {
        // Arrange
        Path p = new Path(Arrays.asList(new Point(0, 0),
                new Point(0, 0),
                new Point(10, 0),
                new Point(20, 0),
                new Point(30, 0),
                new Point(40, 0)


        ));
        double radius = 5;
        ArrayList<Point> curve = p.continousCurve(point -> true);
        PathGeometryHelper geo = new PathGeometryHelper(p, curve, radius);
        HashMap<Point, Collection<Point>> parentage = geo.getParentage(radius);
        assertEquals(parentage.size(), curve.size());
        for (Point point : curve) {
            assertTrue(parentage.containsKey(point));
            assertTrue(parentage.get(point).contains(point));
        }
    }

    @Test
    public void testSubotpimalAABBXPath() {
        int size = 100;
        // Arrange
        Path p = new Path(Arrays.asList(
                new Point(-1, 0),
                new Point(0, 0),
                new Point(size, 0),
                new Point(2 * size, 0),
                new Point(3 * size, 0),
                new Point(3 * size + 1, 0)
        ));
        ArrayList<Point> curve = p.continousCurve(point -> true);
        assertEquals(3 * size + 1, curve.size());
        int radius = 50;
        PathGeometryHelper geo = new PathGeometryHelper(p, curve, radius);
        HashMap<Point, Collection<Point>> parentage = geo.getParentage(radius);
        assertEquals(parentage.size(), curve.size());
        int totalNearby = 0;
        for (Point point : curve) {
            totalNearby += parentage.get(point).size();
        }
        assertTrue(totalNearby < 2f * 2 * radius * (3 * size + 1));
    }

    @Test
    public void treeBoundingBoxTest() {
        Path p = new Path(Arrays.asList(
                new Point(-1, 0),
                new Point(0, 0),
                new Point(500, 500),
                new Point(-500, 500),
                new Point(250, 250),
                new Point(500, -250)
        ));

        Collection<AxisAlignedBoundingBox2d> boxes =
                PathGeometryHelper.toBoundingBoxes(p.continousCurve(point -> true), 100, 50);
        BoundingBox treeBox = PathGeometryHelper.constructTree(boxes);

        for (int i = 0; i < 1000; i++) {
            Point point = new Point((int) (Math.random() * 1000), (int) (Math.random() * 1000));
            boolean insideList = false;
            for (BoundingBox box : boxes)
                insideList = insideList || box.contains(point);
            System.out.println("inside:" + insideList);
            assertEquals(insideList, treeBox.contains(point));
        }
    }
}
