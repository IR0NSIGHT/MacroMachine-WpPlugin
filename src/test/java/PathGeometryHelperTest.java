import org.demo.wpplugin.geometry.*;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathGeometryHelper;
import org.demo.wpplugin.pathing.PointUtils;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.*;

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
        BoundingBox bbx = new AxisAlignedBoundingBox2d(new Point(-100, -200), new Point(200, 500), 0);
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
        int size = 1000;
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
        assertTrue(radius * (3 * size + 1) < totalNearby);
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
                PointUtils.toBoundingBoxes(p.continousCurve(point -> true), 100, 50);
        BoundingBox treeBox = TreeBoundingBox.constructTree(boxes);

        for (int i = 0; i < 1000; i++) {
            Point point = new Point((int) (Math.random() * 1000), (int) (Math.random() * 1000));
            boolean insideList = false;
            for (BoundingBox box : boxes)
                insideList = insideList || box.contains(point);
            System.out.println("inside:" + insideList);
            assertEquals(insideList, treeBox.contains(point));
        }
    }

    @Test
    public void treeBoundingBoxCollectIds() {
        Path p = new Path(Arrays.asList(
                new Point(-1, 0),
                new Point(0, 0),
                new Point(500, 500),
                new Point(-500, 500),
                new Point(250, 250),
                new Point(500, -250),
                new Point(1000, 1000),
                new Point(1000, -1000),
                new Point(1000, -1001),
                new Point(0, 0),
                new Point(1, 1)
        ));

        Collection<AxisAlignedBoundingBox2d> boxes =
                PointUtils.toBoundingBoxes(p.continousCurve(point -> true), 100, 50);
        TreeBoundingBox treeBox = TreeBoundingBox.constructTree(boxes);

        for (int i = 0; i < 1000; i++) {
            Point point = new Point((int) (Math.random() * 1000), (int) (Math.random() * 1000));
            Collection<Integer> treeOut = new LinkedList<>(), loopOut = new LinkedList<>();
            for (AxisAlignedBoundingBox2d box : boxes)
                if (box.contains(point))
                    loopOut.add(box.id);
            treeBox.collectContainingAABBxsIds(point, treeOut);
            assertIterableEquals(loopOut, treeOut);
        }
    }

    @Test
    public void smoothingTestFlatArea() {
        float fixedHeight = 7.57f;
        HeightDimension dimension = new HeightDimension() {
            final HashMap<Point, Float> heights = new HashMap<>();

            @Override
            public float getHeight(int x, int y) {
                return heights.getOrDefault(new Point(x, y), fixedHeight);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                heights.put(new Point(x, y), z);
            }
        };

        Collection<Point> toBeSmoothed = new LinkedList<>();
        int squareLength = 4;
        for (int x = -squareLength; x < squareLength; x++)
            for (int y = -squareLength; y < squareLength; y++) {
                toBeSmoothed.add(new Point(x, y));
            }

        Smoother smoother = new Smoother(toBeSmoothed, 3, dimension);
        for (Point point : toBeSmoothed) {
            assertEquals(fixedHeight, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        }
        smoother.smoothAverage();
        for (Point point : toBeSmoothed) {
            assertEquals(fixedHeight, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        }
    }

    @Test
    public void smoothingTest() {
        float fixedHeight = 7.57f;
        HeightDimension dimension = new HeightDimension() {
            final HashMap<Point, Float> heights = new HashMap<>();

            @Override
            public float getHeight(int x, int y) {
                return heights.getOrDefault(new Point(x, y), fixedHeight);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                heights.put(new Point(x, y), z);
            }
        };

        Point point = new Point(3, 3);
        float pointHeightOriginal = 10.3f;
        dimension.setHeight(point.x, point.y, pointHeightOriginal);
        assertEquals(pointHeightOriginal, dimension.getHeight(point.x, point.y),
                "point is not correct height:" + point);

        Smoother smoother = new Smoother(Collections.singleton(new Point(3, 3)), 50, dimension);
        smoother.smoothAverage();

        //single towering point on map was smoothed out and is now roughly the height as the fixedHeight of the terrain
        assertEquals(fixedHeight, dimension.getHeight(point.x, point.y), 0.2f);
    }

    @Test
    public void smoothingTestAngledUniform() {
        float fixedHeight = 7.57f;
        //terrain is z=x => 45° angle on x axis
        //since terrain is uniformly angled, no change should occur with smoothing
        HeightDimension dimension = new HeightDimension() {
            final HashMap<Point, Float> heights = new HashMap<>();

            @Override
            public float getHeight(int x, int y) {
                return heights.getOrDefault(new Point(x, y), (float) x);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                heights.put(new Point(x, y), z);
            }
        };

        Collection<Point> toBeSmoothed = new LinkedList<>();
        int squareLength = 4;
        for (int x = -squareLength; x < squareLength; x++)
            for (int y = -squareLength; y < squareLength; y++) {
                toBeSmoothed.add(new Point(x, y));
            }

        Smoother smoother = new Smoother(toBeSmoothed, 3, dimension);
        for (Point point : toBeSmoothed) {
            assertEquals(point.x, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        }
        smoother.smoothAverage();
        for (Point point : toBeSmoothed) {
            assertEquals(point.x, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        }
    }

    @Test
    public void smoothingTestLine() {
        float fixedHeight = 8f;
        //terrain is flat but has a 15 tall wall on x=5
        //wall will be smmoothed out
        HeightDimension dimension = new HeightDimension() {
            final HashMap<Point, Float> heights = new HashMap<>();

            @Override
            public float getHeight(int x, int y) {
                return heights.getOrDefault(new Point(x, y), x == 5 ? 15 : fixedHeight);
            }

            @Override
            public void setHeight(int x, int y, float z) {
                heights.put(new Point(x, y), z);
            }
        };

        Collection<Point> toBeSmoothed = new LinkedList<>();
        int squareLength = 10;
        for (int x = -squareLength; x <= squareLength; x++)
            for (int y = -squareLength; y <= squareLength; y++) {
                toBeSmoothed.add(new Point(x, y));
            }

        int smoothRadius = 3;

        Smoother smoother = new Smoother(toBeSmoothed, smoothRadius, dimension);
        for (Point point : toBeSmoothed) {
            if (point.x == 5) //sharp line at x = 5
                assertEquals(15, dimension.getHeight(point.x, point.y));
            else
                assertEquals(fixedHeight, dimension.getHeight(point.x, point.y),
                        "point is not correct height:" + point);
        }

        smoother.smoothAverage();

        float heightAtX5 = 9;
        for (Point point : toBeSmoothed) {

            if (2 <= point.x && point.x <= 8)
                //near wall
                if (Math.abs(point.y) <= squareLength - smoothRadius)
                    //not near y edge
                    assertEquals(heightAtX5, dimension.getHeight(point.x, point.y), "point" + point + " has wrong " +
                            "height");
                else
                    //near y edge
                    assertTrue(8 <= dimension.getHeight(point.x, point.y) && dimension.getHeight(point.x, point.y) < 15
                            , "point" + point + " has wrong height:"+dimension.getHeight(point.x, point.y));

            else if (Math.abs(point.y) <= squareLength - smoothRadius)
                //point is not near the wall
                assertEquals(fixedHeight, dimension.getHeight(point.x, point.y),
                        "point is not correct height:" + point);
            else //a point on the y axis edge with wierd values
                assertTrue(8 <= dimension.getHeight(point.x, point.y) && dimension.getHeight(point.x, point.y) <= 15
                        , "point" + point + " has wrong height:"+dimension.getHeight(point.x, point.y));        }
    }
}
