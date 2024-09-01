package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.BoundingBox;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PathGeometryHelper implements BoundingBox {
    private final Path path;
    private final ArrayList<BoundingBox> boundingBoxes;
    private final ArrayList<Point> curve;
    private final int[] segmentStartIdcs;
    private final double radius;
    private PathGeometryHelper(Path path, ArrayList<BoundingBox> boundingBoxes, ArrayList<Point> curve, double radius) {
        this.path = path;
        this.boundingBoxes = boundingBoxes;
        this.curve = curve;
        this.segmentStartIdcs = calculateSegmentStartIdcs(path, curve);
        this.radius = radius;
    }

    public PathGeometryHelper(Path path, ArrayList<Point> curve, double radius) {
        this.path = path;
        boundingBoxes = new ArrayList<>(Math.max(0, path.amountHandles() - 3));
        for (int i = 0; i < path.amountHandles() - 3; i++) {
            BoundingBox box = CubicBezierSpline.boundingBoxCurveSegment(path.handleByIndex(i),
                    path.handleByIndex(i + 1), path.handleByIndex(i + 2), path.handleByIndex(i + 3));
            if (radius > 0) {
                box = box.expand(radius);
            }
            boundingBoxes.add(box);
        }
        this.curve = curve;
        this.segmentStartIdcs = calculateSegmentStartIdcs(path, curve);
        this.radius = radius;
    }

    int[] calculateSegmentStartIdcs(Path p, ArrayList<Point> curve) {
        Iterator<Point> handles = p.iterator();
        int[] startIdcs = new int[p.amountHandles()-2];
        int startIdcIndex = 0;

        handles.next(); //ignore the very first handle, its not on the curve
        Point controlPoint = handles.next();
        for (int i = 0; i < curve.size(); i++) {
            Point curvePoint = curve.get(i);
            if (controlPoint.equals(curvePoint)) {
                //curve reached this curvepoint, a new segment starts
                startIdcs[startIdcIndex++] = i;
                controlPoint = handles.next();
            }
        }
        return startIdcs;
    }

    private ArrayList<Point> getCurveSegmentForIdx(int bbxIdx) {
        assert bbxIdx >= 0 && bbxIdx < path.amountHandles() - 3;
        List<Point> segmentHandles = Arrays.asList(
                path.handleByIndex(bbxIdx),
                path.handleByIndex(bbxIdx + 1),
                path.handleByIndex(bbxIdx + 2),
                path.handleByIndex(bbxIdx + 3 )
                );
        Path segment = new Path(segmentHandles);
        return segment.continousCurve(p -> true);
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

        Collection<Point> allNearby = collectPointsAroundPath();// allPointsInsideChildBbxs();
        assert new HashSet<>(allNearby).size() == allNearby.size(); //all points are unique
        for (Point point : allNearby) {
            assert this.contains(point);
            Point parentOnCurve = closestCurvePointFor(point);
            if (point.distanceSq(parentOnCurve) < maxRadiusSquared) {
                assert parentage.containsKey(parentOnCurve);
                parentage.get(parentOnCurve).add(point);
            }
        }

        return parentage;
    }

    Collection<Point> collectPointsAroundPath() {
        double radiusSq = radius * radius;
        HashSet<Point> visited = new HashSet<>();
        for (Point p : curve) {
            for (int x = (int) -radius; x < radius; x++) {
                for (int y = (int) -radius; y < radius; y++) {
                    Point nearby = new Point(p.x + x, p.y + y);
                    if (nearby.distanceSq(p) < radiusSq)
                        visited.add(nearby);
                }
            }
        }
        return visited;
    }

    Collection<Point> allPointsInsideChildBbxs() {
        //iterate all points for all bounding boxes
        LinkedList<Point> allNearby = new LinkedList<>();
        LinkedList<BoundingBox> remainingBoxs = new LinkedList<>(boundingBoxes);
        while (!remainingBoxs.isEmpty()) {
            BoundingBox box = remainingBoxs.removeFirst();
            Iterator<Point> pointsInBox = box.areaIterator();

            //we iterate all points in the box, and add all those that are not inside the remaining boxes
            while (pointsInBox.hasNext()) {
                Point point = pointsInBox.next();
                if (isPointInside(point, remainingBoxs))    //will be handled later by that box.
                    continue;
                allNearby.add(point);
            }
        }
        return allNearby;
    }

    boolean isPointInside(Point point, Collection<BoundingBox> boundingBoxes) {
        for (BoundingBox boundingBox : boundingBoxes) {
            if (boundingBox.contains(point)) {
                return true;
            }
        }
        return false;
    }

    Point closestCurvePointFor(Point nearby) {
        Point closestPoint = null;
        double minDistSq = Double.MAX_VALUE;
        Collection<Integer> containingBoxIdcs = getContainingIdcs(nearby);
        for (int idx : containingBoxIdcs) {
            for (int i = segmentStartIdcs[idx]; i < segmentStartIdcs[idx+1]; i++) {
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
        int i = 0;
        for (BoundingBox boundingBox : boundingBoxes) {
            if (boundingBox.contains(nearby)) {
                insideIdcs.add(i);
            }
            i++;
        }
        return insideIdcs;
    }

    @Override
    public boolean contains(Point p) {
        for (BoundingBox bb : boundingBoxes) {
            if (bb.contains(p))
                return true;
        }
        return false;
    }

    @Override
    public BoundingBox expand(double size) {
        ArrayList<BoundingBox> newBoundingBoxes = new ArrayList<>(boundingBoxes.size());
        for (BoundingBox bb : boundingBoxes) {
            newBoundingBoxes.add(bb.expand(size));
        }

        return new PathGeometryHelper(path, newBoundingBoxes, curve, radius);
    }

    @Override
    public Iterator<Point> areaIterator() {
        return allPointsInsideChildBbxs().iterator();
    }
}
