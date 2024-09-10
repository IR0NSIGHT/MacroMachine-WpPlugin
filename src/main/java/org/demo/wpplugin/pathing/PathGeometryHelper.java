package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.geometry.BoundingBox;
import org.demo.wpplugin.geometry.TreeBoundingBox;

import java.awt.*;
import java.util.*;

import static org.demo.wpplugin.pathing.CubicBezierSpline.point2DfromNVectorArr;

public class PathGeometryHelper implements BoundingBox {
    private final Path path;
    private final ArrayList<Point> curve;
    private final int[] segmentStartIdcs;
    private final double radius;
    private TreeBoundingBox treeBoundingBox;

    private PathGeometryHelper(Path path, TreeBoundingBox boundingBoxes, ArrayList<Point> curve,
                               double radius
            , int[] segmentStartIdcs) {
        this.path = path;
        this.curve = curve;
        this.treeBoundingBox = boundingBoxes;
        this.segmentStartIdcs = segmentStartIdcs;
        this.radius = radius;
    }

    public PathGeometryHelper(Path path, ArrayList<float[]> curve, double radius) {
        this.path = path;
        this.curve = point2DfromNVectorArr(curve);
        this.radius = radius;

        int boxSizeFacctor = 10; //no zero divisor
        int amountBoxes = Math.max(0, curve.size() / boxSizeFacctor + 1);
        segmentStartIdcs = new int[amountBoxes + 1];
        int segmentIdx = 0;
        for (int i = 0; i < curve.size(); i += boxSizeFacctor) {
            segmentStartIdcs[segmentIdx++] = i;
        }
        segmentStartIdcs[segmentIdx] = curve.size();

        ArrayList<AxisAlignedBoundingBox2d> boundingBoxes = new ArrayList<>(PointUtils.toBoundingBoxes(this.curve, boxSizeFacctor, radius));
        treeBoundingBox = TreeBoundingBox.constructTree(boundingBoxes);
        for (Point p : this.curve) {
            assert this.contains(p);
            assert this.contains(new Point(p.x + (int) radius, p.y + (int) radius));
        }
    }

    /**
     * get all points closer than radius mapped to the point on curve they are closest to
     *
     * @param radius
     * @return map: key=points on continuous curve, value: list of points withint radius that are closest to the key
     */
    public HashMap<Point, Collection<Point>> getParentage(double radius) {
        double maxRadiusSquared = radius * radius;
        HashMap<Point, Collection<Point>> parentage = new HashMap<>();
        for (Point point : curve) {
            parentage.put(point, new LinkedList<>());
        }

        Collection<Point> allNearby = allPointsInsideChildBbxs();

        assert new HashSet<>(allNearby).containsAll(curve) : "some curvepoints are missing";

        for (Point point : allNearby) {
            assert this.contains(point);
            Point parentOnCurve = closestCurvePointFor(point);
            assert !curve.contains(point) || point.equals(parentOnCurve) : "cant assign a curvepoint to another " +
                    "parent than himself";
            if (point.distanceSq(parentOnCurve) < maxRadiusSquared) {
                assert parentage.containsKey(parentOnCurve);
                parentage.get(parentOnCurve).add(point);
            }
        }

        return parentage;
    }

    Collection<Point> allPointsInsideChildBbxs() {
        //iterate all points for all bounding boxes
        HashSet<Point> allNearby = new HashSet<>(curve);
        int[] factors = {1, -1};
        for (Point c : curve) {
            allNearby.add(c);
            for (Integer f : factors)
                expandX(c, (int) radius, f, allNearby, false);
        }

        for (Point c : new HashSet<>(allNearby))
            for (Integer f : factors)
                expandX(c, (int) radius, f, allNearby, true);
        return allNearby;
    }

    private void expandX(Point base, int radius, int f, HashSet<Point> allNearby, boolean onY) {
        for (int i = 1; i < radius; i++) {
            Point nearby = onY ? new Point(base.x, base.y + i * f) : new Point(base.x + i * f, base.y);
            if (allNearby.contains(nearby)) {
                return;
            }
            allNearby.add(nearby);
        }
    }

    public Point closestCurvePointFor(Point nearby) {
        Point closestPoint = null;
        double minDistSq = Double.MAX_VALUE;
        Collection<Integer> containingBoxIdcs = getContainingIdcs(nearby);
        for (int idx : containingBoxIdcs) {
            assert idx >= 0;
            assert idx < segmentStartIdcs.length: "box idx is out of bounds";
            for (int i = segmentStartIdcs[idx]; i < segmentStartIdcs[idx + 1]; i++) {
                Point curveP = curve.get(i);
                double distSq = curveP.distanceSq(nearby);
                if (distSq < minDistSq) {
                    minDistSq = distSq;
                    closestPoint = curveP;
                }
            }
        }
        assert closestPoint != null;
        return closestPoint;
    }

    Collection<Integer> getContainingIdcs(Point nearby) {
        //find bbxs nearby belongs to
        LinkedList<Integer> insideIdcs = new LinkedList<>();
        treeBoundingBox.collectContainingAABBxsIds(nearby, insideIdcs);
        return insideIdcs;
    }

    @Override
    public boolean contains(Point p) {
        return treeBoundingBox.contains(p);
    }

    @Override
    public BoundingBox expand(double size) {
        return new PathGeometryHelper(path, treeBoundingBox.expand(size), curve, radius, segmentStartIdcs);
    }

    @Override
    public Iterator<Point> areaIterator() {
        return allPointsInsideChildBbxs().iterator();
    }
}
