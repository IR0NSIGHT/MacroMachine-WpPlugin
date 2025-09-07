package org.ironsight.wpplugin.macromachine.Layers.RoadBuilder;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Point2iTest {

    @Test
    void distanceSquared() {
        {
            var p1 = new Point2i(10010, 10010);
            var p2 = new Point2i(10020, 10020);
            assertEquals(10 * 10 + 10 * 10, p1.distanceSquared(p2));
        }
        {
            var p1 = new Point2i(-10010, -10010);
            var p2 = new Point2i(-10020, -10020);
            assertEquals(10 * 10 + 10 * 10, p1.distanceSquared(p2));
        }
    }

    @Test
    void equality() {
        var p1 = new Point2i(10010, 10010);
        var p2 = new Point2i(10010, 10010);
        assertNotSame(p1,p2);
        assertEquals(p1,p2);
    }
}