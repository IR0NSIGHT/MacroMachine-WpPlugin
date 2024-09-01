package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
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

    private PathGeometryHelper(Path path, ArrayList<BoundingBox> boundingBoxes, ArrayList<Point> curve, double radius
            , int[] segmentStartIdcs) {
        this.path = path;
        this.boundingBoxes = boundingBoxes;
        this.curve = curve;
        this.segmentStartIdcs = segmentStartIdcs;
        this.radius = radius;
    }

    public PathGeometryHelper(Path path, ArrayList<Point> curve, double radius) {
        this.path = path;
        this.curve = curve;
        this.radius = radius;

        int boxSizeFacctor = 100;
        int amountBoxes = Math.max(0, curve.size() / boxSizeFacctor + 1);
        boundingBoxes = new ArrayList<>(amountBoxes);
        segmentStartIdcs = new int[amountBoxes + 1];
        int segmentIdx = 0;
        for (int i = 0; i < curve.size(); i += boxSizeFacctor) {
            List<Point> subcurve = curve.subList(i, Math.min(i + boxSizeFacctor, curve.size()));
            boundingBoxes.add(AxisAlignedBoundingBox2d.fromPoints(subcurve).expand(radius));
            segmentStartIdcs[segmentIdx++] = i;
        }
        segmentStartIdcs[segmentIdx] = curve.size() - 1;
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
        assert new HashSet<>(allNearby).size() == allNearby.size(); //all points are unique

        assert new HashSet<>(allNearby).containsAll(curve) : "some curvepoints are missing";
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
        int segmentIdx = 0;
        while (!remainingBoxs.isEmpty()) {
            BoundingBox box = remainingBoxs.removeFirst();
            Collection<Point> curveSegment = curveSegment(segmentIdx++);
            HashSet<Point> visited = new HashSet<>();

            //we iterate the curvesegment in the bbx and add all those that are not inside the remaining boxes
            for (Point p : curveSegment) {
                for (int x = (int) -radius; x <= radius; x++) {
                    for (int y = (int) -radius; y <= radius; y++) {
                        Point nearby = new Point(p.x + x, p.y + y);
                        if (!isPointInside(nearby, remainingBoxs))
                            visited.add(nearby);
                    }
                }
            }
            allNearby.addAll(visited);
        }
        Collection<Point> leftover = new ArrayList<>(curve);
        leftover.removeAll(allNearby);
        assert leftover.size() == 0;
        return allNearby;
    }

    Collection<Point> curveSegment(int segmentIdx) {
        return curve.subList(segmentStartIdcs[segmentIdx], segmentStartIdcs[segmentIdx + 1]);
    }

    Iterator<Point> curveSegmentIterator(int segmentIdx) {
        return new Iterator<Point>() {
            final int startIdx = segmentStartIdcs[segmentIdx];
            final int endIdx = segmentStartIdcs[segmentIdx + 1];
            int currentIdx = startIdx;

            @Override
            public boolean hasNext() {
                return currentIdx < endIdx;
            }

            @Override
            public Point next() {
                return curve.get(currentIdx++);
            }
        };
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

        return new PathGeometryHelper(path, newBoundingBoxes, curve, radius, segmentStartIdcs);
    }

    @Override
    public Iterator<Point> areaIterator() {
        return allPointsInsideChildBbxs().iterator();
    }
}
