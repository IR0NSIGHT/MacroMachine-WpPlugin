package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.ironsight.wpplugin.macromachine.Layers.PathBuilder.PathEditHandles.MIN_HANDLE_OFFSET;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathEditHandlesTest
{
    private static Point2D.Double p(double x, double y) {
        return new Point2D.Double(x, y);
    }

    @Test
    void radiusHandleSitsOnCircleButNeverUnderTheNode() {
        assertEquals(p(150, 10), PathEditHandles.radiusHandle(p(100, 10), 50));
        assertEquals(p(100 + MIN_HANDLE_OFFSET, 10), PathEditHandles.radiusHandle(p(100, 10), 3));
    }

    private static javax.vecmath.Point4f n(float x, float y) {
        return new javax.vecmath.Point4f(x, y, 64, 4);
    }

    @Test
    void segmentIndicesFollowRealSampledCurves() {
        var shapes = List.of(List.of(n(0, 0), n(10, 0)), List.of(n(0, 0), n(10, 0), n(20, 0)),
                List.of(n(0, 0), n(30, 5), n(60, -10), n(80, 40), n(40, 70)));
        for (var nodes : shapes)
            for (float strength : new float[]{0f, 0.3f, 1f}) {
                var curve = PathToolBackend.getPathFromNodes(nodes, strength).path;
                int[] segments = PathEditHandles.segmentIndices(nodes, curve);
                assertEquals(nodes.size() - 1, segments[segments.length - 1], "all nodes passed");
                for (int k = 1; k < segments.length; k++)
                    assertTrue(segments[k] >= segments[k - 1], "monotone");
                // every node is reached exactly at its own block
                for (int i = 0; i < nodes.size(); i++) {
                    int first = -1;
                    for (int k = 0; k < segments.length && first < 0; k++)
                        if (segments[k] >= i)
                            first = k;
                    assertEquals(Math.round(nodes.get(i).x), Math.round(curve.get(first).x));
                    assertEquals(Math.round(nodes.get(i).y), Math.round(curve.get(first).y));
                }
            }
    }

    @Test
    void straightSegmentsSplitAtMiddleNodeAndCoincidentNodesPassTogether() {
        var nodes = List.of(n(0, 0), n(10, 0), n(20, 0));
        var curve = PathToolBackend.getPathFromNodes(nodes, 0f).path;
        int[] segments = PathEditHandles.segmentIndices(nodes, curve);
        for (int k = 0; k < curve.size(); k++) {
            float x = curve.get(k).x;
            if (x >= 0 && x < 10)
                assertEquals(0, segments[k], "x=" + x);
            else if (x >= 10 && x < 20)
                assertEquals(1, segments[k], "x=" + x);
        }
        var stacked = List.of(n(0, 0), n(5, 0), n(5, 0), n(9, 0));
        int[] synthetic = PathEditHandles.segmentIndices(stacked,
                List.of(n(-1, 0), n(0, 0), n(3, 0), n(5, 0), n(7, 0), n(9, 0)));
        assertArrayEquals(new int[]{-1, 0, 0, 2, 2, 3}, synthetic);

        assertTrue(PathEditHandles.segmentSelected(1, java.util.Set.of(1, 2)));
        assertFalse(PathEditHandles.segmentSelected(1, java.util.Set.of(1, 3)));
        assertFalse(PathEditHandles.segmentSelected(-1, java.util.Set.of(-1, 0)));
    }

    @Test
    void heightHandleIsVerticalAboveForRaisedAndBelowForSunkenNodes() {
        double k = PathEditHandles.HEIGHT_PX_PER_BLOCK;
        // flush with the ground line at 0
        assertEquals(p(50, 50), PathEditHandles.heightHandle(p(50, 50), 0));
        assertEquals(p(50, 50 - 10 * k), PathEditHandles.heightHandle(p(50, 50), 10));
        assertEquals(p(50, 50 + 4 * k), PathEditHandles.heightHandle(p(50, 50), -4));
        for (float relative : new float[]{-20, -1, 0, 1, 7.5f, 30})
            assertEquals(relative,
                    PathEditHandles.relativeHeightAt(p(50, 50), PathEditHandles.heightHandle(p(50, 50), relative)),
                    1e-9);
        // locked to the vertical axis
        assertEquals(PathEditHandles.relativeHeightAt(p(50, 50), p(50, 0)),
                PathEditHandles.relativeHeightAt(p(50, 50), p(400, 0)));
        assertEquals(0, PathEditHandles.relativeHeightAt(p(50, 50), p(90, 50)));
        assertEquals("+5", PathEditHandles.signed(5));
        assertEquals("-2.5", PathEditHandles.signed(-2.5f));
        assertEquals("0", PathEditHandles.signed(0));
    }

    @Test
    void radiusScalesRelativeToDragStart() {
        // handle clamped to the minimum offset still scales continuously from where it
        // was grabbed
        assertEquals(2f, PathEditHandles.scaleFactor(p(0, 0), p(22, 0), p(44, 0)), 1e-6);
        assertEquals(0.5f, PathEditHandles.scaleFactor(p(0, 0), p(22, 0), p(0, 11)), 1e-6);
        assertEquals(1f, PathEditHandles.scaleFactor(p(0, 0), p(0, 0), p(5, 5)));
        assertEquals(28f, PathEditHandles.scaledRadius(14f, 2f, true));
        assertEquals(7.5f, PathEditHandles.scaledRadius(5f, 1.5f, false));
        assertEquals(1f, PathEditHandles.scaledRadius(4f, 0.01f, true));
    }

    @Test
    void tangentFollowsNeighboursAndEndSegments() {
        var nodes = List.of(p(0, 0), p(100, 0), p(100, 100));
        var middle = PathEditHandles.tangent(nodes, 1);
        assertEquals(Math.sqrt(0.5), middle.dirX(), 1e-9);
        assertEquals(Math.sqrt(0.5), middle.dirY(), 1e-9);
        assertEquals(50, middle.halfPrev(), 1e-9);
        assertEquals(50, middle.halfNext(), 1e-9);

        var first = PathEditHandles.tangent(nodes, 0);
        assertEquals(1, first.dirX(), 1e-9);
        assertEquals(0, first.dirY(), 1e-9);
        var last = PathEditHandles.tangent(nodes, 2);
        assertEquals(0, last.dirX(), 1e-9);
        assertEquals(1, last.dirY(), 1e-9);

        assertNull(PathEditHandles.tangent(List.of(p(0, 0)), 0));
        assertNull(PathEditHandles.tangent(List.of(p(5, 5), p(5, 5)), 0));
    }

    @Test
    void strengthHandlesRoundTripAlongTangent() {
        var tangent = PathEditHandles.tangent(List.of(p(0, 0), p(100, 0), p(300, 0)), 1);
        assertEquals(p(100 + MIN_HANDLE_OFFSET, 0), tangent.handle(1, 0f));
        assertEquals(p(100 - MIN_HANDLE_OFFSET, 0), tangent.handle(-1, 0f));
        // forward half = 100, backward half = 50
        assertEquals(p(100 + MIN_HANDLE_OFFSET + 100, 0), tangent.handle(1, 1f));
        assertEquals(p(100 - MIN_HANDLE_OFFSET - 50, 0), tangent.handle(-1, 1f));
        for (float strength : new float[]{-1f, -0.5f, 0f, 0.25f, 1f, 1.5f, 2f})
            for (int side : new int[]{-1, 1})
                assertEquals(strength, tangent.strengthAt(side, tangent.handle(side, strength)), 1e-6);
    }

    @Test
    void strengthDragIsLockedToTangentSnapsNearNodeAndClamps() {
        var tangent = PathEditHandles.tangent(List.of(p(0, 0), p(100, 0), p(300, 0)), 1);
        // perpendicular movement is ignored
        assertEquals(tangent.strengthAt(1, p(100 + MIN_HANDLE_OFFSET + 50, 0)),
                tangent.strengthAt(1, p(100 + MIN_HANDLE_OFFSET + 50, 80)));
        assertEquals(0f, tangent.strengthAt(1, p(100 + MIN_HANDLE_OFFSET / 2, 0)));
        assertEquals(PathEditHandles.MAX_STRENGTH, tangent.strengthAt(1, p(2000, 0)));
        assertEquals(PathEditHandles.MIN_STRENGTH, tangent.strengthAt(1, p(-2000, 0)));
        // backward handle dragged outward (towards previous node) also increases
        // strength
        assertEquals(1f, tangent.strengthAt(-1, p(100 - MIN_HANDLE_OFFSET - 50, 0)), 1e-6);
    }
}
