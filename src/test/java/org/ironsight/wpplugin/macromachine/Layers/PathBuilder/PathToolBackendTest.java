package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import org.junit.jupiter.api.Test;

import javax.vecmath.Point4f;

import java.util.ArrayList;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PathToolBackendTest
{
    @Test
    void enforceSlopeLimit() {
        { // negative slope
            var path = new ArrayList<>(IntStream.range(0, 100).mapToObj(i -> new Point4f(1, 2, -i, 4)).toList());
            var enforced = PathToolBackend.enforceSlopeLimit(path, 4);
            for (int i = 0; i < enforced.size(); i++) {
                assertEquals(i * -0.25f, enforced.get(i).z, 0.00001f);
            }
        }

        { // positive slope
            var path = new ArrayList<>(IntStream.range(0, 100).mapToObj(i -> new Point4f(1, 2, i, 4)).toList());
            var enforced = PathToolBackend.enforceSlopeLimit(path, 4);
            for (int i = 0; i < enforced.size(); i++) {
                assertEquals(i * 0.25f, enforced.get(i).z, 0.00001f);
            }
        }

        { // untouched slope
            var path = new ArrayList<>(IntStream.range(0, 100).mapToObj(i -> new Point4f(1, 2, i, 4)).toList());
            var enforced = PathToolBackend.enforceSlopeLimit(path, 200);
            for (int i = 0; i < enforced.size(); i++) {
                assertEquals(i, enforced.get(i).z, 0.00001f);
            }
        }
    }

    @Test
    void plotPathBetween() {
        Point4f start = new Point4f(10, 20, 0, 0);
        Point4f end = new Point4f(20, 20, 0, 0);
        var path = PathToolBackend.plotPathBetween(start, end);
        assertFalse(path.contains(start));
        assertEquals(10, path.size());
        assertEquals(end, path.get(path.size() - 1));
    }

    @Test
    void forcePathToMinPos() {
        var path = new ArrayList<Point4f>();
        path.add(new Point4f(12, 14, 100, 0));
        path.add(new Point4f(12, 15, 100, 0));
        path.add(new Point4f(12, 16, 100, 0));
        path.add(new Point4f(12, 17, 100, 0));

        PathToolBackend.forcePathToMinPos(path, p -> p.y);
        assertEquals(path.get(0), new Point4f(12, 14, 14, 0));
        assertEquals(path.get(1), new Point4f(12, 15, 15, 0));
        assertEquals(path.get(2), new Point4f(12, 16, 16, 0));
        assertEquals(path.get(3), new Point4f(12, 17, 17, 0));
    }

    @Test
    void cloneHeightMapData() {
    }

    @Test
    void writeHeightMapDataToTile() {
    }

    @Test
    void forcePathOnlyDownhill() {
        var path = new ArrayList<Point4f>();
        path.add(new Point4f(12, 14, 100, 0));
        path.add(new Point4f(12, 15, 101, 0));
        path.add(new Point4f(12, 16, 90, 0));
        path.add(new Point4f(12, 17, 100, 0));

        PathToolBackend.forcePathOnlyDownhill(path);
        assertEquals(path.get(0), new Point4f(12, 14, 100, 0));
        assertEquals(path.get(1), new Point4f(12, 15, 100, 0));
        assertEquals(path.get(2), new Point4f(12, 16, 90, 0));
        assertEquals(path.get(3), new Point4f(12, 17, 90, 0));
    }

    @Test
    void forcePathToHeight() {
        var path = new ArrayList<Point4f>();
        path.add(new Point4f(12, 14, 100, 0));
        path.add(new Point4f(12, 15, 101, 0));
        path.add(new Point4f(12, 16, 90, 0));
        path.add(new Point4f(12, 17, 100, 0));

        PathToolBackend.forcePathToHeight(path, 17);
        assertEquals(path.get(0), new Point4f(12, 14, 17, 0));
        assertEquals(path.get(1), new Point4f(12, 15, 17, 0));
        assertEquals(path.get(2), new Point4f(12, 16, 17, 0));
        assertEquals(path.get(3), new Point4f(12, 17, 17, 0));
    }

    @Test
    void collectTilesAroundPath() {
    }

    @Test
    void xyDistSq() {
        var d = PathToolBackend.xyDistSq(new Point4f(12, 14, 17, 0), new Point4f(22, 34, -17, 100));
        assertEquals(10 * 10 + 20 * 20, d);
    }

    @Test
    void applyToTile() {

    }

    @Test
    void filterStrengthFor() {
    }

    @Test
    void getSubPathFor() {
        {
            var path = new ArrayList<Point4f>();
            path.add(new Point4f(12, 14, 100, 0));
            path.add(new Point4f(12, 15, 101, 0));
            path.add(new Point4f(5, -5, 90, 0)); // inside BBX
            path.add(new Point4f(12, 17, 100, 10));

            var subPath = PathToolBackend.getSubPathFor(new Point2i(-10, -10), new Point2i(10, 10), path, 1);
            assertTrue(subPath.contains(path.get(2)));
            assertTrue(subPath.contains(path.get(3)));

        }
        {
            var path = new ArrayList<Point4f>();
            path.add(new Point4f(12, 14, 100, 0));
            path.add(new Point4f(12, 15, 101, 0));
            path.add(new Point4f(5, -5, 90, 0)); // inside BBX
            path.add(new Point4f(12, 17, 100, 0));

            var subPath = PathToolBackend.getSubPathFor(new Point2i(-10, -10), new Point2i(10, 10), path, 1);
            assertTrue(subPath.contains(path.get(2)));
        }
        {
            var path = new ArrayList<Point4f>();
            path.add(new Point4f(12, 14, 100, 0));
            path.add(new Point4f(12, 15, 101, 0));
            path.add(new Point4f(12, 16, 90, 0));
            path.add(new Point4f(12, 17, 100, 0));

            var subPath = PathToolBackend.getSubPathFor(new Point2i(-10, -10), new Point2i(10, 10), path, 1);
            assertTrue(subPath.isEmpty());
        }
    }
}
