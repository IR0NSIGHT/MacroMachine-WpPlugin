package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.AxisAlignedBoundingBox2d;
import org.demo.wpplugin.geometry.BoundingBox;
import org.demo.wpplugin.geometry.NeverBoundingBox;
import org.demo.wpplugin.geometry.TreeBoundingBox;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PathGeometryHelper implements BoundingBox {
    private final Path path;
    private final ArrayList<AxisAlignedBoundingBox2d> boundingBoxes;
    private TreeBoundingBox treeBoundingBox;
    private final ArrayList<Point> curve;
    private final int[] segmentStartIdcs;
    private final double radius;

    private PathGeometryHelper(Path path, ArrayList<AxisAlignedBoundingBox2d> boundingBoxes, ArrayList<Point> curve,
                               double radius
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

        int boxSizeFacctor = 100; //no zero divisor
        int amountBoxes = Math.max(0, curve.size() / boxSizeFacctor + 1);
        segmentStartIdcs = new int[amountBoxes + 1];
        int segmentIdx = 0;
        for (int i = 0; i < curve.size(); i += boxSizeFacctor) {
            segmentStartIdcs[segmentIdx++] = i;
        }
        segmentStartIdcs[segmentIdx] = curve.size();

        boundingBoxes = new ArrayList<>(toBoundingBoxes(curve, boxSizeFacctor, radius));
        treeBoundingBox = constructTree(boundingBoxes);
        for (Point p : curve) {
            assert this.contains(p);
            assert this.contains(new Point(p.x + (int) radius, p.y + (int) radius));
        }
    }

    public static ArrayList<AxisAlignedBoundingBox2d> toBoundingBoxes(ArrayList<Point> curve, int boxSizeFactor,
                                                                      double radius) {
        ArrayList<AxisAlignedBoundingBox2d> bbxs = new ArrayList<>(curve.size() / boxSizeFactor + 1);
        for (int i = 0; i < curve.size(); i += boxSizeFactor) {
            int boxId = i/boxSizeFactor;
            List<Point> subcurve = curve.subList(i, Math.min(i + boxSizeFactor, curve.size()));
            AxisAlignedBoundingBox2d box = new AxisAlignedBoundingBox2d(subcurve, boxId).expand(radius);
            bbxs.add(box);
            assert box.id == boxId;
        }
        return bbxs;
    }

    public static TreeBoundingBox constructTree(Collection<AxisAlignedBoundingBox2d> neighbouringBoxes) {
        if (neighbouringBoxes.size() == 1 ) {
            neighbouringBoxes.add(new NeverBoundingBox(-1));
        } else if (neighbouringBoxes.size() == 0) {
            throw new IllegalArgumentException("will not construct tree for zero length list");
        }
        assert neighbouringBoxes.size() >= 2;
        List<AxisAlignedBoundingBox2d> oldList = new ArrayList<>(neighbouringBoxes);
        while (oldList.size() > 1) {
            List<AxisAlignedBoundingBox2d> newList = new ArrayList<>(oldList.size() / 2 + 1);
            for (int i = 0; i < oldList.size(); i++) {
                if (i < oldList.size() - 1) {
                    TreeBoundingBox parent = new TreeBoundingBox(oldList.get(i), oldList.get(i + 1));
                    newList.add(parent);
                    i++;
                } else {
                    newList.add(oldList.get(i));
                }
            }
            oldList = newList;
        }
        assert oldList.size() == 1;
        return (TreeBoundingBox) oldList.get(0);
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

    void expandX(Point base, int radius, int f, HashSet<Point> allNearby, boolean onY) {
        for (int i = 1; i < radius; i++) {
            Point nearby = onY ? new Point(base.x, base.y + i * f) : new Point(base.x + i * f, base.y);
            if (allNearby.contains(nearby)) {
                return;
            }
            allNearby.add(nearby);
        }
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
            assert idx >= 0;
            assert idx < boundingBoxes.size();
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
        ArrayList<AxisAlignedBoundingBox2d> newBoundingBoxes = new ArrayList<>(boundingBoxes.size());
        for (BoundingBox bb : boundingBoxes) {
            newBoundingBoxes.add((AxisAlignedBoundingBox2d) bb.expand(size));
        }

        return new PathGeometryHelper(path, newBoundingBoxes, curve, radius, segmentStartIdcs);
    }

    @Override
    public Iterator<Point> areaIterator() {
        return allPointsInsideChildBbxs().iterator();
    }
}
