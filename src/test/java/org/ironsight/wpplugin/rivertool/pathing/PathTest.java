package org.ironsight.wpplugin.rivertool.pathing;

import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import static org.ironsight.wpplugin.rivertool.pathing.PointInterpreter.PointType.POSITION_2D;
import static org.ironsight.wpplugin.rivertool.pathing.PointInterpreter.PointType.RIVER_2D;
import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    @Test
    void clonePath() {
        Path p = Path.newFilledPath(4, RIVER_2D);
        Path clone = p.clone();
        assertEquals(p, clone);
        assertNotSame(p, clone);

        int idx = p.amountHandles() / 2;
        float[] someHandle = p.handleByIndex(idx).clone();
        p = p.setHandleByIdx(someHandle, idx);
        assertArrayEquals(p.handleByIndex(idx), someHandle);

        assertNotSame(p, clone);
        assertEquals(p, clone);

        float[] newHandle = someHandle.clone();
        newHandle = RiverHandleInformation.setValue(newHandle, RiverHandleInformation.RiverInformation.RIVER_RADIUS, RiverHandleInformation.getValue(newHandle, RiverHandleInformation.RiverInformation.RIVER_RADIUS) + 1);
        assertNotSame(newHandle, someHandle);
        p = p.setHandleByIdx(newHandle, idx);
        assertArrayEquals(p.handleByIndex(idx), newHandle);
        assertNotSame(p, clone);
        assertNotEquals(p, clone);

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
        ContinuousCurve curve = ContinuousCurve.fromPath(newEmpty, HeightDimension.getImmutableDimension62());
        assertEquals(0, curve.curveLength());
    }

    private Path newFilledPath(int length) {
        return Path.newFilledPath(length, PointInterpreter.PointType.POSITION_2D);
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
        Path moved = path.overwriteHandle(oldHandle, newHandle);

        assertEquals(length, moved.amountHandles(), "new path has a different length after shifting a point");
        assertArrayEquals(newHandle, moved.handleByIndex(length / 2),
                "the shifted point is not where its supposed " + "to" + " be");
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
        assertEquals(length + 1, inserted.amountHandles());
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
            Path p = Path.newFilledPath(length, RIVER_2D);
            assertEquals(length, p.amountHandles());
            assertSame(p.type, RIVER_2D);

            float[] somePoint = p.handleByIndex(length / 2).clone();
            somePoint = RiverHandleInformation.setValue(somePoint, RiverHandleInformation.RiverInformation.RIVER_RADIUS, 345f);
            Assertions.assertEquals(345f, RiverHandleInformation.getValue(somePoint, RiverHandleInformation.RiverInformation.RIVER_RADIUS));

            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }
    }

    @Test
    void getMappingFromTo() {
        ArrayList<float[]> handles = new ArrayList<>();
        handles.add(new float[]{5, 10});
        handles.add(new float[]{10, 20});
        Path oldP = new Path(handles, POSITION_2D);
        assertEquals(2, oldP.amountHandles());
        assertArrayEquals(new float[]{5, 10}, oldP.handleByIndex(0));
        assertArrayEquals(new float[]{10, 20}, oldP.handleByIndex(1));

        {
            Path newP = oldP.insertPointAfter(new float[]{5, 10}, new float[]{7, 7});
            assertArrayEquals(new int[]{0, -1, 1}, Path.getMappingFromTo(newP, oldP), "new 0 -> old 0, new 1 -> " +
                    "deleted, new 2 -> old 1");
            assertArrayEquals(new int[]{0, 2}, Path.getMappingFromTo(oldP, newP), "new 0 -> old 0, new 1 -> 2");
        }

        {
            Path newP = oldP.removePoint(new float[]{5, 10});
            assertArrayEquals(new int[]{1}, Path.getMappingFromTo(newP, oldP), "new 0 -> old 1");
            assertArrayEquals(new int[]{-1, 0}, Path.getMappingFromTo(oldP, newP), "old 0 -> deleted, old 1 -> new 0");
        }
    }

    @Test
    void mapPoints() {
        Path p = newFilledPath(10);
        Path mapped = p.mapPoints(new MapPointAction() {
            @Override
            public float[] map(float[] point, int index) {
                float[] out = point.clone();
                out[0] += 5;
                out[1] += 10;
                return out;
            }
        });

        assertEquals(p.amountHandles(), mapped.amountHandles());
        for (int i = 0; i < p.amountHandles(); i++) {
            assertEquals(p.handleByIndex(i)[0]+5, mapped.handleByIndex(i)[0]);
            assertEquals(p.handleByIndex(i)[1]+10, mapped.handleByIndex(i)[1]);

        }
    }
}