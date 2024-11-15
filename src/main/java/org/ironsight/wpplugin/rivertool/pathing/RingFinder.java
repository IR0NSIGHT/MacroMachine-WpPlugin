package org.ironsight.wpplugin.rivertool.pathing;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class RingFinder {
    public Set<Point> restriction = new HashSet<>();
    HashMap<Integer, HashMap<Point, Float>> rings = new HashMap<>();

    public RingFinder(HashMap<Point, Float> initialPoints, int amountRings) {
        rings.put(0, initialPoints);
        rings.put(1, findRingAround(initialPoints, initialPoints));
        for (int i = 2; i < amountRings; i++) {
            HashMap<Point, Float> nextRing = findRingAround(rings.get(i - 1), rings.get(i - 2));
            rings.put(i, nextRing);
        }
    }

    public RingFinder(HashMap<Point, Float> initialPoints, int amountRings, Set restrictions) {
        restriction = restrictions;
        rings.put(0, initialPoints);
        rings.put(1, findRingAround(initialPoints, initialPoints));
        for (int i = 2; i < amountRings; i++) {
            HashMap<Point, Float> nextRing = findRingAround(rings.get(i - 1), rings.get(i - 2));
            rings.put(i, nextRing);
        }
    }

    private HashMap<Point, Float> findRingAround(HashMap<Point, Float> points, HashMap<Point, Float> ignore) {
        HashMap<Point, Float> ring = new HashMap<Point, Float>(points.size());
        for (Point parent : points.keySet()) {
            for (int x : new int[]{-1, 0, 1})
                for (int y : new int[]{-1, 0, 1}) {
                    Point thisP = new Point(parent.x + x, parent.y + y);
                    if (!ignore.containsKey(thisP) && !points.containsKey(thisP) && !restriction.contains(thisP)) {
                        Float z = points.get(parent);
                        ring.put(thisP, z);
                    }
                }
        }
        return ring;
    }

    public HashMap<Point, Float> ring(int idx) {
        return rings.get(idx);
    }
}
