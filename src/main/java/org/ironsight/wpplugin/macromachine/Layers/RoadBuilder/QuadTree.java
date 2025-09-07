package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class QuadTree {
    private static final int CAPACITY = 4; // max points per leaf before splitting
    private static final int MIN_SIZE = 3;
    private final float minX, minY, maxX, maxY;
    // If this is a leaf, points != null and children == null
    // If this is an internal node, children != null and points == null
    private List<Point2i> points;
    private QuadTree nw, ne, sw, se;

    public QuadTree(float minX, float minY, float maxX, float maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        this.points = new ArrayList<>(); // start as a leaf
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        var root = new QuadTree(-1000, -1000, 1000, 1000);
        List<Point2i> points =
                IntStream.range(0, 10000)
                        .mapToObj(i -> new Point2i((int) (Math.random() * 2000 - 1000),
                                (int) (Math.random() * 2000 - 1000)))
                        .toList();

        points.forEach(root::insert);
        long end = System.currentTimeMillis();
        System.out.printf("Setup took %f%n", (end - start) / 1000f);

        var closest = root.getClosest(new Point2i(3, 12));
        System.out.println("Closest to (3,12): (" + closest.x + "," + closest.y + ")");

        start = end;
        for (int i = 0; i < TILE_SIZE * TILE_SIZE; i++) {
            root.getClosest(new Point2i((int) (Math.random() * 2000 - 1000),
                    (int) (Math.random() * 2000 - 1000)));
        }
        end = System.currentTimeMillis();
        System.out.printf("Lookup took %f%n", (end - start) / 1000f);
    }

    // Insert a point into the quadtree
    public boolean insert(Point2i p) {
   //     System.out.println("attempt insert " + p + " into node " + this);
        if (!contains(p)) return false;

        if (isLeaf()) {
            if (points.size() < CAPACITY || !canSubDivide()) {
                points.add(p);
                return true;
            } else {
            //    System.out.println("subdivide node " + this);
                assert canSubDivide();
                subdivide();
                return insert(p); // reinsert into the correct child
            }
        } else {
            boolean insertedIntoChildren = (nw.insert(p) || ne.insert(p) || sw.insert(p) || se.insert(p));
            assert insertedIntoChildren;
            return insertedIntoChildren;
        }
    }

    private boolean isLeaf() {
        return points != null;
    }

    private boolean contains(Point2i p) {
        return p.x >= minX && p.x <= maxX && p.y >= minY && p.y <= maxY;
    }

    private boolean canSubDivide() {
        if (maxX - minX < MIN_SIZE || maxY - minY < MIN_SIZE)
            return false;
        return true;
    }

    private void subdivide() {

        int midX = Math.round((minX + maxX) / 2);
        int midY = Math.round((minY + maxY) / 2);

        nw = new QuadTree(minX, minY, midX, midY);
        ne = new QuadTree(midX + Float.MIN_VALUE, minY, maxX, midY);
        sw = new QuadTree(minX, midY + Float.MIN_VALUE, midX, maxY);
        se = new QuadTree(midX + Float.MIN_VALUE, midY + Float.MIN_VALUE, maxX, maxY);

        // redistribute points into children
        for (Point2i p : points) {
            if (!nw.insert(p) && !ne.insert(p) && !sw.insert(p) && !se.insert(p)) {
                throw new IllegalStateException("Point did not fit into any child: " + p);
            }
        }
        points = null; // clear — this is no longer a leaf
    }

    // Find the closest point to a given query
    public Point2i getClosest(Point2i query) {
        return getClosest(query, null, Float.MAX_VALUE);
    }

    /**
     * find tree node that fully encloses start -> end rect, and is the smallest treenode that does so.
     *
     * @param start
     * @param end
     * @return
     */
    public QuadTree getSmallestEnclosingTree(Point2i start, Point2i end) {
        return getSmallestEnclosingTree(start,end,this);
    }

    private QuadTree getSmallestEnclosingTree(Point2i start, Point2i end, QuadTree tree) {
        // i am a candidate, but maybe my children are better suited
        for (QuadTree child : new QuadTree[]{nw, ne, sw, se}) {
            if (child == null)
                continue;
            if (child.contains(start) && child.contains(end))
                return child.getSmallestEnclosingTree(start, end, this);
        }
        return tree;
    }

    private Point2i getClosest(Point2i query, Point2i best, float bestDistSq) {
        if (isLeaf()) {
            for (Point2i p : points) {
                float distSq = p.distanceSquared(query);
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = p;
                }
            }
            return best;
        }

        // Internal node → check children
        QuadTree[] children = {nw, ne, sw, se};
        for (QuadTree child : children) {
            float childDistSq = child.distanceToRegionSquared(query);
            //this childs BBX is closer than the current best distance, we must test its content
            if (childDistSq < bestDistSq) {
                best = child.getClosest(query, best, bestDistSq);
                if (best != null)
                    bestDistSq = best.distanceSquared(query);
            }
        }
        return best;
    }

    // Minimum distance from a point to this node's rectangle
    private float distanceToRegionSquared(Point2i p) {
        float dx = 0;
        if (p.x < minX) dx = minX - p.x;
        else if (p.x > maxX) dx = p.x - maxX;

        float dy = 0;
        if (p.y < minY) dy = minY - p.y;
        else if (p.y > maxY) dy = p.y - maxY;

        return dx * dx + dy * dy;
    }

    @Override
    public String toString() {
        return String.format("x: %.2f..%.2f y:%.2f..%.2f leaf:%s", minX, maxX,minY, maxY, isLeaf());
    }
}
