package org.demo.wpplugin.pathing;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class RingFinder {
    HashMap<Integer, HashSet<Point>> rings = new HashMap<>();

    public RingFinder(HashSet<Point> initialPoints, int amountRings) {
        rings.put(0, initialPoints);
        rings.put(1, findRingAround(initialPoints, new HashSet<>()));
        for (int i = 2; i < amountRings; i++) {
            HashSet nextRing = findRingAround(rings.get(i - 1), rings.get(i - 2));
            rings.put(i, nextRing);
        }
    }

    private static HashSet<Point> findRingAround(HashSet<Point> points, HashSet<Point> ignore) {
        HashSet<Point> known = points;
        HashSet<Point> ring = new HashSet<>(points.size());
        for (Point p : points) {
            for (int x : new int[]{-1, 0, 1})
                for (int y : new int[]{-1, 0, 1}) {
                    Point thisP = new Point(p.x + x, p.y + y);
                    if (!known.contains(thisP) && !ignore.contains(thisP))
                        ring.add(thisP);
                }
        }
        return ring;
    }

    public HashSet<Point> ring(int idx) {
        return rings.get(idx);
    }
}
