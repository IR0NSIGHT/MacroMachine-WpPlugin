package org.demo.wpplugin.pathing;

import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;
import static org.demo.wpplugin.operations.River.RiverHandleInformation.RiverInformation.*;
import static org.demo.wpplugin.pathing.PointInterpreter.PointType.RIVER_2D;
import static org.demo.wpplugin.pathing.PointUtils.getPoint2D;
import static org.demo.wpplugin.pathing.PointUtils.setPosition2D;
import static org.junit.jupiter.api.Assertions.*;

class PathTest {
    private Path newFilledPath(int length) {
        return newFilledPath(length, PointInterpreter.PointType.POSITION_2D);
    }

    private Path newFilledPath(int length, PointInterpreter.PointType type) {
        Path p = new Path(Collections.EMPTY_LIST, type);
        for (int i = 0; i < length; i++) {
            float[] newHandle = new float[type.size];
            newHandle[0] = 3 * i;
            newHandle[1] = 4 * i;
            p = p.addPoint(newHandle.clone());
        }
        return p;
    }

    @Test
    void clonePath() {
        Path p = newFilledPath(4, RIVER_2D);
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
        newHandle = setValue(newHandle, RIVER_RADIUS, getValue(newHandle, RIVER_RADIUS) + 1);
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
            Path p = newFilledPath(length, RIVER_2D);
            assertEquals(length, p.amountHandles());
            assertSame(p.type, RIVER_2D);

            float[] somePoint = p.handleByIndex(length / 2).clone();
            somePoint = setValue(somePoint, RIVER_RADIUS, 345f);
            assertEquals(345f, getValue(somePoint, RIVER_RADIUS));

            int idxFound = p.indexOfPosition(somePoint);
            assertEquals(length / 2, idxFound);
        }
    }

    @Test
    void continuousCurve() {
        Path p = newFilledPath(10 + 2 + 1, RIVER_2D);

        float startV = 3;
        p = p.setHandleByIdx(setValue(p.handleByIndex(0), RIVER_RADIUS, startV), 0);
        assertEquals(startV, getValue(p.handleByIndex(0), RIVER_RADIUS));

        p = p.setHandleByIdx(setValue(p.handleByIndex(1), RIVER_RADIUS, startV), 1);
        assertEquals(startV, getValue(p.handleByIndex(1), RIVER_RADIUS));


        p = p.setHandleByIdx(setValue(p.handleByIndex(p.amountHandles() - 1), RIVER_RADIUS, 17), p.amountHandles() - 1);
        assertEquals(17, getValue(p.handleByIndex(p.amountHandles() - 1), RIVER_RADIUS));
        p = p.setHandleByIdx(setValue(p.handleByIndex(p.amountHandles() - 2), RIVER_RADIUS, 17), p.amountHandles() - 2);
        assertEquals(17, getValue(p.handleByIndex(p.amountHandles() - 2), RIVER_RADIUS));

        for (int i = 2; i < p.amountHandles() - 2; i++) {
            p = p.setHandleByIdx(setValue(p.handleByIndex(i), RIVER_RADIUS, INHERIT_VALUE), i);
            assertEquals(INHERIT_VALUE, getValue(p.handleByIndex(i), RIVER_RADIUS));
        }

        //set position for all points
        for (int i = 0; i < p.amountHandles(); i++) {
            float[] newHandle = setPosition2D(p.handleByIndex(i), new float[]{i * 10, 37});
            p = p.setHandleByIdx(newHandle, i);

            assertEquals(new Point(i * 10, 37), getPoint2D(p.handleByIndex(i)));
        }

        ArrayList<float[]> curve = p.continousCurve();
        ArrayList<Point> curveP = PointUtils.point2DfromNVectorArr(curve);

        int curvePointIdx = 0;
        for (int curveX = getPoint2D(p.handleByIndex(1)).x; curveX <= getPoint2D(p.handleByIndex(p.amountHandles() - 2)).x; curveX++) {
            //iterate from first handle where curve starts to last handle where curve ends
            assertEquals(curveX, curveP.get(curvePointIdx).x, "Point " + curveP.get(curvePointIdx));
            assertEquals(37, curveP.get(curvePointIdx).y, "Point " + curveP.get(curvePointIdx));

            curvePointIdx++;
        }
        for (float[] point : curve) {
            System.out.println("#".repeat(Math.round(getValue(point, RIVER_RADIUS))));
        }
    }

    @Test
    void prepareEmptyHandlesForInterpolation() {
        float[] incompleteHandles;
        float[] completeHandles;
        float[] expectedHandles;

        //only one is set
        incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, 7, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = Path.prepareEmptyHandlesForInterpolation(incompleteHandles, INHERIT_VALUE);
        expectedHandles = new float[]{7, 7, 7, INHERIT_VALUE, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //two are set
        incompleteHandles = new float[]{5, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, 7};
        completeHandles = Path.prepareEmptyHandlesForInterpolation(incompleteHandles, INHERIT_VALUE);
        expectedHandles = new float[]{5, 5, INHERIT_VALUE, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //two are set but in wierd positions
        incompleteHandles = new float[]{INHERIT_VALUE, 5, 7, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = Path.prepareEmptyHandlesForInterpolation(incompleteHandles, INHERIT_VALUE);
        expectedHandles = new float[]{5, 5, 7, 7, 7};
        assertArrayEquals(expectedHandles, completeHandles);

        //three or more are set but the first and last two are missing
        incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, 5, INHERIT_VALUE, 6, INHERIT_VALUE, 7,
                INHERIT_VALUE, 8,
                INHERIT_VALUE, 9, INHERIT_VALUE, 10, INHERIT_VALUE, INHERIT_VALUE};
        completeHandles = Path.prepareEmptyHandlesForInterpolation(incompleteHandles, INHERIT_VALUE);
        expectedHandles = new float[]{5, 5, 5, INHERIT_VALUE, 6, INHERIT_VALUE, 7, INHERIT_VALUE, 8,
                INHERIT_VALUE, 9, INHERIT_VALUE, 10, 10, 10};
        assertArrayEquals(expectedHandles, completeHandles);


        {        //not a single value is known
            incompleteHandles = new float[]{INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE,
                    INHERIT_VALUE};
            float[] finalIncompleteHandles = incompleteHandles;
            assertThrows(IllegalArgumentException.class,
                    () -> Path.prepareEmptyHandlesForInterpolation(finalIncompleteHandles, INHERIT_VALUE));
        }

        {    //handles are not long enough
            incompleteHandles = new float[]{1, 2, 3};
            float[] finalIncompleteHandles = incompleteHandles;
            assertThrows(IllegalArgumentException.class,
                    () -> Path.prepareEmptyHandlesForInterpolation(finalIncompleteHandles, INHERIT_VALUE));
        }
    }

    @Test
    void interpolateHandles() {
        float[] incompleteHandles;
        int[] curveByHandleIdx = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        float[] completeHandles;
        float[] expectedHandles;

        //simplest flat
        incompleteHandles = new float[]{1, 1, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE,
                INHERIT_VALUE, INHERIT_VALUE, 1, 1};
        assertTrue(Path.canBeInterpolated(incompleteHandles));
        completeHandles = Path.interpolateHandles(incompleteHandles, curveByHandleIdx);
        expectedHandles = new float[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1};
        assertArrayEquals(expectedHandles, completeHandles, 0.01f);

        //minimum length
        incompleteHandles = new float[]{1, 4, 7, 19};
        curveByHandleIdx = new int[]{0, 1, 2, 3};
        assertTrue(Path.canBeInterpolated(incompleteHandles));
        completeHandles = Path.interpolateHandles(incompleteHandles, curveByHandleIdx);
        expectedHandles = new float[]{1, 4, 7, 19};
        assertArrayEquals(expectedHandles, completeHandles, 0.01f);

        //classic 4 point interpolation that satisfys convex hull property
        incompleteHandles = new float[]{10, 10, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, INHERIT_VALUE, 3, 3};
        curveByHandleIdx = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
        assertTrue(Path.canBeInterpolated(incompleteHandles));
        completeHandles = Path.interpolateHandles(incompleteHandles, curveByHandleIdx);
        expectedHandles = new float[]{1, 4, 7, 19};

        for (int i = 2; i < incompleteHandles.length - 2; i++) {
            //satisfies convex hull property
            assertTrue(completeHandles[i] < 10);
            assertTrue(completeHandles[i] > 3);
        }
    }
}