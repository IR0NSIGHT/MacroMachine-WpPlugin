package org.demo.wpplugin.pathing;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;

import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    private Path newFilledPath(int length) {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.POSITION);
        for (int i = 0; i < length; i++) {
            float[] newHandle = new float[]{3.9f * i, -2.7f * i};
            p = p.addPoint(newHandle.clone());
        }
        return p;
    }

    @Test
    void addPoint() {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.POSITION);
        assertEquals(0, p.amountHandles());
        LinkedList<float[]> addedHandles = new LinkedList<>();
        for (int i = 0; i < 50; i++) {
            float[] newHandle = new float[]{3.9f * i, -2.7f * i};
            p = p.addPoint(newHandle.clone());
            addedHandles.add(newHandle);
        }
        assertEquals(50, p.amountHandles());
        for (int i = 0; i < 50; i++) {
            float[] iHandle = addedHandles.get(i);
            float[] path_iHandle = p.handleByIndex(i);
            assertArrayEquals(iHandle, path_iHandle);
        }
    }

    @Test
    void newEmpty() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());
        Path newEmpty = path.newEmpty();
        assertEquals(0, newEmpty.amountHandles());
        assertTrue(newEmpty.continousCurve().isEmpty());
    }

    @Test
    void removePoint() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        LinkedList<float[]> addedHandles = new LinkedList<>();
        for (int i = 0; i < length; i++) {
            addedHandles.add(path.handleByIndex(i));
        }
        assertEquals(addedHandles.size(), path.amountHandles());
        for (int i = 0; i < path.amountHandles(); i++) {
            assertArrayEquals(addedHandles.get(i), path.handleByIndex(i));
        }

        while (!addedHandles.isEmpty()) {
            float[] toBeRemoved = addedHandles.remove(0);
            Path removed = path.removePoint(toBeRemoved);
            for (int i = 0; i < removed.amountHandles(); i++) {
                assertArrayEquals(addedHandles.get(i), removed.handleByIndex(i));
            }
            path = removed;
        }
    }

    @Test
    void movePoint() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] oldHandle = path.handleByIndex(length / 2).clone();
        float[] newHandle = new float[]{123, 456};
        Path moved = path.movePoint(oldHandle, newHandle);

        assertEquals(length, moved.amountHandles(), "new path has a different length after shifting a point");
        assertArrayEquals(newHandle, moved.handleByIndex(length / 2), "the shifted point is not where its supposed to" +
                " be");
    }

    @Test
    void getPreviousPoint() throws IllegalAccessException {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] thisPoint = path.handleByIndex(length / 2).clone();
        float[] previousPoint = path.handleByIndex((length / 2) -1 ).clone();
        float[] fromPath = path.getPreviousPoint(thisPoint);
        assertArrayEquals(previousPoint, fromPath);
    }

    @Test
    void insertPointAfter() {
    }

    @Test
    void handleByIndex() {
    }

    @Test
    void getClosestHandleTo() {
    }

    @Test
    void indexOf() {
        int length = 10;
        Path p = newFilledPath(length);
        assertEquals(length, p.amountHandles());

        float[] somePoint = p.handleByIndex(length/2).clone();
        int idxFound = p.indexOf(somePoint);
        assertEquals(length/2, idxFound);
    }
}