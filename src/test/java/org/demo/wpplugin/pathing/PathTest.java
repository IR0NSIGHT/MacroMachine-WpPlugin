package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedList;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.getValue;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.setValue;
import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    private Path newFilledPath(int length) {
        return newFilledPath(length, PointInterpreter.PointType.POSITION_2D);
    }

    private Path newFilledPath(int length, PointInterpreter.PointType type) {
        Path p = new Path(Collections.EMPTY_LIST, type);
        for (int i = 0; i < length; i++) {
            float[] newHandle = new float[type.size];
            newHandle[0] = 3.9f * i;
            newHandle[1] = -2.7f * i;
            p = p.addPoint(newHandle.clone());
        }
        return p;
    }

    @Test
    void addPoint() {
        Path p = new Path(Collections.EMPTY_LIST, PointInterpreter.PointType.POSITION_2D);
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
        float[] previousPoint = path.handleByIndex((length / 2) - 1).clone();
        float[] fromPath = path.getPreviousPoint(thisPoint);
        assertArrayEquals(previousPoint, fromPath);
    }

    @Test
    void insertPointAfter() {
        int length = 50;
        Path path = newFilledPath(length);
        assertEquals(length, path.amountHandles());

        float[] thisPoint = path.handleByIndex(length / 2).clone();
        float[] newPoint = new float[path.type.size];
        newPoint[0] = 12345f;
        newPoint[1] = -123456f;
        Path inserted = path.insertPointAfter(thisPoint, newPoint);
        assertEquals(length+1, inserted.amountHandles());
        assertArrayEquals(newPoint, inserted.handleByIndex(length / 2 + 1), "inserted point not as expected");
        assertArrayEquals(thisPoint, inserted.handleByIndex(length / 2), "this point is not untouched");
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
        {   //most basic path, position only
            Path p = newFilledPath(length);
            assertEquals(length, p.amountHandles());

            float[] somePoint = p.handleByIndex(length / 2).clone();
            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }

        {  //make sure meta data is ignored for indexOf
            Path p = newFilledPath(length, PointInterpreter.PointType.RIVER_2D);
            assertEquals(length, p.amountHandles());
            assertTrue(p.type == PointInterpreter.PointType.RIVER_2D);

            float[] somePoint = p.handleByIndex(length / 2).clone();
            somePoint = setValue(somePoint, RiverHandleInformation.RiverInformation.RIVER_RADIUS, 345f);
            assertEquals(345f, getValue(somePoint, RiverHandleInformation.RiverInformation.RIVER_RADIUS));

            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }
    }
}