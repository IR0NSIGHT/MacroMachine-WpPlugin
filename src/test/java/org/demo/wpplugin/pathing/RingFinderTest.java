package org.demo.wpplugin.pathing;

import org.demo.wpplugin.geometry.HeightDimension;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class RingFinderTest {

    @Test
    void ringSinglePoint() {
        HashMap<Point, Float> initialPoints = new HashMap<>();
        initialPoints.put(new Point(5, 15), 17f);
        HeightDimension dim = HeightDimension.getEmptyMutableDimension();
        RingFinder rf = new RingFinder(initialPoints, 3, dim);

        //initial points are the zero ring
        HashMap<Point, Float> zero = rf.ring(0);
        assertEquals(zero.size(), 1);
        assertTrue(zero.containsKey(new Point(5, 15)));

        //first layer
        HashMap<Point, Float> first = rf.ring(1);
        assertEquals(8, first.size());
        assertFalse(first.containsKey(new Point(5, 15)), "ring 1 contains stuff form ring 0");

        //straight all directions
        assertTrue(first.containsKey(new Point(4, 15)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(6, 15)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(5, 14)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(5, 16)), "ring 1 is missing a neighbour of ring 0");

        //diagonals
        assertTrue(first.containsKey(new Point(4, 14)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(4, 16)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(6, 14)), "ring 1 is missing a neighbour of ring 0");
        assertTrue(first.containsKey(new Point(6, 16)), "ring 1 is missing a neighbour of ring 0");

        //second layer
        HashMap<Point, Float> second = rf.ring(2);
        for (Point p : first.keySet())
            assertFalse(second.containsKey(p), "ring 2 contains stuff form ring 1");
        for (Point p : zero.keySet())
            assertFalse(second.containsKey(p), "ring 2 contains stuff form ring 0");

        //check sides of the square
        for (int x : new int[]{3, 7})
            for (int y = 13; y <= 17; y++) {
                assertTrue(second.containsKey(new Point(x, y)), "ring 2 is missing point " + x);
            }
    }
}