package org.demo.wpplugin;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.kernel.Fixify;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixifyTest {

    @Test
    void fixDimDoNothing() {
        HashMap<Point, Float> map = new HashMap<>();
        HeightDimension dim = new HeightDimension() {
            @Override
            public float getHeight(int x, int y) {
                return 7;
            }

            @Override
            public void setHeight(int x, int y, float z) {
                map.put(new Point(x, y), z);
            }
        };
        int startX = -10, startY = -200, width = 500, height = 900;
        Fixify.fixDim(dim, new Rectangle(startX, startY, width, height));
        assertEquals(width * height, map.size());
        for (Point p : map.keySet()) {
            assertTrue(p.x >= startX && p.x < startX + width && p.y >= startY && p.y < startY + height);
            assertEquals(7, map.get(p), " Point " + p);
        }
    }

    @Test
    void fixDim() {
        HashMap<Point, Float> map = new HashMap<>();
        HeightDimension dim = new HeightDimension() {
            @Override
            public float getHeight(int x, int y) {
                if (x == y) {
                    return 19;
                }
                return 7;
            }

            @Override
            public void setHeight(int x, int y, float z) {
                map.put(new Point(x, y), z);
            }
        };
        int startX = -10, startY = -200, width = 500, height = 900;
        Fixify.fixDim(dim, new Rectangle(startX, startY, width, height));

        assertEquals(width * height, map.size());
        for (Point p : map.keySet()) {
            assertTrue(p.x >= startX && p.x < startX + width && p.y >= startY && p.y < startY + height);
            assertEquals(7, map.get(p), " Point " + p);
        }
    }

    @Test
    void fixDimNoExposed() {
        HashMap<Point, Float> map = new HashMap<>();
        HeightDimension dim = new HeightDimension() {
            @Override
            public float getHeight(int x, int y) {
                return x;
            }

            @Override
            public void setHeight(int x, int y, float z) {
                if (z != x)
                    System.out.println("wrong values was tried to be set: " + x + "," + y + "," + z);
                map.put(new Point(x, y), z);
            }
        };
        int startX = -10, startY = -200, width = 500, height = 900;
        Fixify.fixDim(dim, new Rectangle(startX, startY, width, height));

        assertEquals(width * height, map.size());
        for (Point p : map.keySet()) {
            assertTrue(p.x >= startX && p.x < startX + width && p.y >= startY && p.y < startY + height);
            assertEquals(p.x, map.get(p));
        }
    }
}