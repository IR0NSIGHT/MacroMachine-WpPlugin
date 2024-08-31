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
                new Point(155,-255),
                new Point(-9,9)
        ));
        PathGeometryHelper o = new PathGeometryHelper(p,  p.continousCurve(point -> true), 0);

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
        BoundingBox bbx = new AxisAlignedBoundingBox2d(new Point(-100,-200),new Point(200,500));
        assertTrue(bbx.contains(new Point(-100,-200)));
        assertTrue(bbx.contains(new Point(200,500)));

        assertFalse(bbx.contains(new Point(-101,-200)));
        assertFalse(bbx.contains(new Point(-100,-201)));
        assertFalse(bbx.contains(new Point(-101,-201)));

        assertFalse(bbx.contains(new Point(201,500)));
        assertFalse(bbx.contains(new Point(200,501)));
        assertFalse(bbx.contains(new Point(201,501)));

        bbx = bbx.expand(1);
        assertTrue(bbx.contains(new Point(-101,-200)));
        assertTrue(bbx.contains(new Point(-100,-201)));
        assertTrue(bbx.contains(new Point(-101,-201)));

        assertTrue(bbx.contains(new Point(201,500)));
        assertTrue(bbx.contains(new Point(200,501)));
        assertTrue(bbx.contains(new Point(201,501)));

        bbx = bbx.expand(-2);
        assertFalse(bbx.contains(new Point(-101,-200)));
        assertFalse(bbx.contains(new Point(-100,-201)));
        assertFalse(bbx.contains(new Point(-101,-201)));

        assertFalse(bbx.contains(new Point(201,500)));
        assertFalse(bbx.contains(new Point(200,501)));
        assertFalse(bbx.contains(new Point(201,501)));

        assertFalse(bbx.contains(new Point(-100,-200)));
        assertFalse(bbx.contains(new Point(200,500)));
    }

    @Test
    public void parentageTest() {
        // Arrange
        Path p = new Path(Arrays.asList(new Point(0, 0),
                new Point(-2, 5),
                new Point(4, 4),
                new Point(5, 5),
                new Point(155,-255),
                new Point(-9,9)
        ));
        double radius = 5;
        ArrayList<Point> curve = p.continousCurve(point -> true);
        PathGeometryHelper geo = new PathGeometryHelper(p,  curve, radius);
        HashMap<Point, Collection<Point>> parentage = geo.getParentage(radius);
        assertEquals(parentage.size(), curve.size());
        assertTrue(parentage.get(new Point(-2,5)).contains(new Point(-2,5)));

    }
}
