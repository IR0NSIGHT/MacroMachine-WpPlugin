import org.demo.wpplugin.geometry.*;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;
import org.demo.wpplugin.pathing.PathGeometryHelper;
import org.demo.wpplugin.pathing.PointInterpreter;
import org.demo.wpplugin.pathing.PointUtils;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.*;

import static org.demo.wpplugin.pathing.PointUtils.point2DfromNVectorArr;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.Path.curveIsContinous;
import static org.junit.jupiter.api.Assertions.*;

public class PathGeometryHelperTest {
    @Test
    public void testPathGeometry() {
        // Arrange
        Path p = new Path(Arrays.asList(RiverHandleInformation.riverInformation(0,0),

                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(-2, 5),
                RiverHandleInformation.riverInformation(4, 4),
                RiverHandleInformation.riverInformation(5, 5),
                RiverHandleInformation.riverInformation(155, -255),
                RiverHandleInformation.riverInformation(-9, 9)
        ), PointInterpreter.PointType.RIVER_2D);
        PathGeometryHelper o = new PathGeometryHelper(p, p.continousCurve(), 0);

        // Act
        ArrayList<Point> curve = point2DfromNVectorArr(p.continousCurve());
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
        Path p = new Path(Arrays.asList(RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(10, 0),
                RiverHandleInformation.riverInformation(20, 0),
                RiverHandleInformation.riverInformation(30, 0),
                RiverHandleInformation.riverInformation(40, 0)


        ), PointInterpreter.PointType.RIVER_2D);
        double radius = 5;
        ArrayList<Point> curve = point2DfromNVectorArr(p.continousCurve());
        PathGeometryHelper geo = new PathGeometryHelper(p, p.continousCurve(), radius);
        HashMap<Point, Collection<Point>> parentage = geo.getParentage();
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
                RiverHandleInformation.riverInformation(-1, 0),
                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(size, 0),
                RiverHandleInformation.riverInformation(2 * size, 0),
                RiverHandleInformation.riverInformation(3 * size, 0),
                RiverHandleInformation.riverInformation(3 * size + 1, 0)
        ), PointInterpreter.PointType.RIVER_2D);
        ArrayList<float[]> curveF = p.continousCurve();
        ArrayList<Point> curve = point2DfromNVectorArr(curveF);
        assert curveIsContinous(curveF);
        for (int i = 1; i < p.amountHandles()-1; i++) {
            Point point = getPoint2D(p.handleByIndex(i));
            assertTrue(curve.contains(point),"final curve is missing a control point:"+point);
        }
        assertTrue(3 * size + 1 <= curve.size());
        int radius = 50;
        PathGeometryHelper geo = new PathGeometryHelper(p, p.continousCurve(), radius);
        HashMap<Point, Collection<Point>> parentage = geo.getParentage();
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
                RiverHandleInformation.riverInformation(-1, 0),
                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(500, 500),
                RiverHandleInformation.riverInformation(-500, 500),
                RiverHandleInformation.riverInformation(250, 250),
                RiverHandleInformation.riverInformation(500, -250)
        ), PointInterpreter.PointType.RIVER_2D);

        Collection<AxisAlignedBoundingBox2d> boxes =
                PointUtils.toBoundingBoxes(point2DfromNVectorArr(p.continousCurve()), 100, 50);
        BoundingBox treeBox = TreeBoundingBox.constructTree(boxes);

        for (int i = 0; i < 1000; i++) {
            Point point = new Point((int) (Math.random() * 1000), (int) (Math.random() * 1000));
            boolean insideList = false;
            for (BoundingBox box : boxes)
                insideList = insideList || box.contains(point);
            assertEquals(insideList, treeBox.contains(point));
        }
    }

    @Test
    public void treeBoundingBoxCollectIds() {
        Path p = new Path(Arrays.asList(
                RiverHandleInformation.riverInformation(-1, 0),
                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(500, 500),
                RiverHandleInformation.riverInformation(-500, 500),
                RiverHandleInformation.riverInformation(250, 250),
                RiverHandleInformation.riverInformation(500, -250),
                RiverHandleInformation.riverInformation(1000, 1000),
                RiverHandleInformation.riverInformation(1000, -1000),
                RiverHandleInformation.riverInformation(1000, -1001),
                RiverHandleInformation.riverInformation(0, 0),
                RiverHandleInformation.riverInformation(1, 1)
        ), PointInterpreter.PointType.RIVER_2D);

        Collection<AxisAlignedBoundingBox2d> boxes =
                PointUtils.toBoundingBoxes(point2DfromNVectorArr(p.continousCurve()), 100, 50);
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
     /*   for (Point point : toBeSmoothed) {
            assertEquals(fixedHeight, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        }

      */
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
        //   assertEquals(fixedHeight, dimension.getHeight(point.x, point.y), 0.2f);
    }

    @Test
    public void smoothingTestFlatGauss() {
        float fixedHeight = 5;
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

        int mountainLength = 2;
        for (int x = -mountainLength; x <= mountainLength; x++)
            for (int y = -mountainLength; y <= mountainLength; y++) {
                dimension.setHeight(x, y, 10);
            }

        Collection<Point> toBeSmoothed = new LinkedList<>();
        int squareLength = 10;
        for (int x = -squareLength; x <= squareLength; x++)
            for (int y = -squareLength; y <= squareLength; y++) {
                toBeSmoothed.add(new Point(x, y));
            }

        Smoother smoother = new Smoother(toBeSmoothed, 3, dimension);
        smoother.smoothGauss();
/*
        //hand calculated values using a gauss kernel as used by the function
        assertEquals(7.02f, dimension.getHeight(-2,2),0.05f);
        assertEquals(7.02f, dimension.getHeight(2,2),0.05f);
        assertEquals(7.02f, dimension.getHeight(-2,-2),0.05f);
        assertEquals(7.02f, dimension.getHeight(2,-2),0.05f);

        assertEquals(7.947f, dimension.getHeight(-2,0),0.05f);
*/
    }


    @Test
    public void basicGaussKernel() {
        float fixedHeight = 12f;
        //terrain is z=x => 45° angle on x axis
        //since terrain is uniformly angled, no change should occur with smoothing
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

        dimension.setHeight(0, 0, 101f);

        Collection<Point> toBeSmoothed = new LinkedList<>();
        int squareLength = 10;
        for (int x = -squareLength; x <= squareLength; x++)
            for (int y = -squareLength; y <= squareLength; y++) {
                toBeSmoothed.add(new Point(x, y));
            }

        int radius = 5;
        Smoother smoother = new Smoother(toBeSmoothed, radius, dimension);

        float[] kernel = Smoother.generateGaussianCurve(2 * radius + 1, 10, radius, radius / 2f);
        float kernelSum = 0;
        for (int i = 0; i < kernel.length; i++)
            kernel[i] = (float) Math.sqrt(kernel[i]);

        for (float v1 : kernel) {
            for (float v2 : kernel) kernelSum += v1 * v2;
        }
        HashMap<Point, Float> naiveGauss = new HashMap<>();
        for (Point point : toBeSmoothed) {
            //calculate naive n² gauss
            float pointValue = 0;
            for (int xIdx = 0; xIdx < kernel.length; xIdx++) {
                for (int yIdx = 0; yIdx < kernel.length; yIdx++) {
                    int xPos = point.x + xIdx - radius;
                    int yPos = point.y + yIdx - radius;
                    float f1 = kernel[xIdx], f2 = kernel[yIdx];
                    float z = dimension.getHeight(xPos, yPos);
                    pointValue += z * f1 * f2;
                }
            }
            pointValue /= kernelSum;
            naiveGauss.put(point, pointValue);
        }

        smoother.smoothGauss();

        for (Point point : toBeSmoothed) {
            assertEquals(naiveGauss.get(point), dimension.getHeight(point.x, point.y), 0.1f, "gauss smoothed point is" +
                    " equal to naive " +
                    "gauss smoothed value:" + point);
        }
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
        //    for (Point point : toBeSmoothed) {
        //        assertEquals(point.x, dimension.getHeight(point.x, point.y), "point is not correct height:" + point);
        //    }
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
        /*
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
                            , "point" + point + " has wrong height:" + dimension.getHeight(point.x, point.y));

            else if (Math.abs(point.y) <= squareLength - smoothRadius)
                //point is not near the wall
                assertEquals(fixedHeight, dimension.getHeight(point.x, point.y),
                        "point is not correct height:" + point);
            else //a point on the y axis edge with wierd values
                assertTrue(8 <= dimension.getHeight(point.x, point.y) && dimension.getHeight(point.x, point.y) <= 15
                        , "point" + point + " has wrong height:" + dimension.getHeight(point.x, point.y));
        }

         */
    }
}
