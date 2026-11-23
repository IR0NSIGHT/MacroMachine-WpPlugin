package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import javax.vecmath.Point4f;
import org.junit.jupiter.api.Test;

class PathOperationsTest
{
    private static Point4f n(float x, float y, float z, float w) {
        return new Point4f(x, y, z, w);
    }

    @Test
    void nodesOnTheSameBlockDoNotBreakTheCurve() {
        // "not strictly monotone" (-ea) when two neighbouring nodes share a block
        assertTrue(PathOperationsTest.class.desiredAssertionStatus(), "run with assertions like WorldPainter");
        var shapes = List.of(List.of(n(0, 0, 60, 4), n(0, 0, 60, 4), n(10, 0, 60, 4)),
                List.of(n(0, 0, 60, 4), n(10, 0, 60, 4), n(10, 0, 62, 6), n(20, 5, 60, 4)),
                List.of(n(5, 5, 60, 4), n(5, 5, 60, 4), n(5, 5, 60, 4)),
                List.of(n(0, 0, 60, 4), n(10, 0, 60, 4), n(10.3f, 0.2f, 60, 4), n(30, 10, 60, 4), n(40, 0, 60, 4)));
        for (var nodes : shapes)
            for (float strength : new float[]{0f, 0.3f, 1f}) {
                var curve = assertDoesNotThrow(() -> PathToolBackend.getPathFromNodes(nodes, strength).path);
                int[] segments = PathEditHandles.segmentIndices(nodes, curve);
                assertEquals(nodes.size() - 1, segments[segments.length - 1], "every node reached");
            }
        // subdividing a short segment into more cuts than it has blocks
        var subdivided = PathOperations.subdivide(List.of(n(0, 0, 60, 4), n(3, 0, 60, 4)), Set.of(0, 1), 10, 0f);
        assertDoesNotThrow(() -> PathToolBackend.getPathFromNodes(subdivided.path(), 0.3f));
    }

    @Test
    void neighbouringNodesOnTheSameBlockAreMergedOthersAreNot() {
        var path = new java.util.ArrayList<>(List.of(n(0, 0, 60, 4), n(10, 0, 70, 6), n(10.2f, 0.1f, 74, 10),
                n(10, 0, 66, 8), n(20, 0, 60, 4), n(0, 0, 60, 4)));
        var runs = PathOperations.coincidentRuns(path);
        assertEquals(1, runs.size(), "the loop back to node 1 is a legit self-crossing");
        assertArrayEquals(new int[]{1, 2, 3}, runs.get(0));
        var merged = PathOperations.mergeCoincident(new PathOperations.Result(path, Set.of(3, 4)));
        assertEquals(4, merged.path().size());
        assertEquals(70f, merged.path().get(1).z, 1e-4, "heights averaged");
        assertEquals(8f, merged.path().get(1).w, 1e-4, "radii averaged");
        assertEquals(Set.of(1, 2), merged.selection(), "merged node selected because node 4 was");
        // nothing to merge: the result is returned unchanged
        var apart = new PathOperations.Result(new java.util.ArrayList<>(List.of(n(0, 0, 1, 1), n(5, 0, 1, 1))),
                Set.of());
        assertSame(apart, PathOperations.mergeCoincident(apart));
    }

    @Test
    void onlyClumpsTheOperationCreatedAreMerged() {
        // nodes 2+3 already share a block before the operation
        var before = List.of(n(0, 0, 60, 4), n(10, 0, 60, 4), n(10, 0, 60, 4), n(20, 0, 60, 4), n(30, 0, 60, 4));
        var heightOnly = PathOperations.onlyDownhill(before, Set.of(0, 1, 2, 3, 4));
        assertEquals(List.of(), PathOperations.createdCoincidentRuns(before, heightOnly), "pre-existing: left alone");
        // moving node 5 onto node 4 creates a clump
        var moved = new java.util.ArrayList<>(before);
        moved.set(4, n(20, 0, 60, 4));
        var runs = PathOperations.createdCoincidentRuns(before, new PathOperations.Result(moved, Set.of()));
        assertEquals(1, runs.size());
        assertArrayEquals(new int[]{3, 4}, runs.get(0));
        var merged = PathOperations.mergeRuns(new PathOperations.Result(moved, Set.of()), runs);
        assertEquals(4, merged.path().size(), "only the created clump merged; nodes 2+3 stay separate");
        // inserted nodes count as created
        var subdivided = PathOperations.subdivide(List.of(n(0, 0, 60, 4), n(2, 0, 60, 4)), Set.of(0, 1), 6, 0f);
        assertFalse(PathOperations.createdCoincidentRuns(List.of(n(0, 0, 60, 4), n(2, 0, 60, 4)), subdivided).isEmpty(),
                "subdividing a 2-block segment into 6 cuts clumps the new nodes");
    }

    @Test
    void selectedSegmentsNeedBothEnds() {
        assertEquals(List.of(), PathOperations.selectedSegments(3, Set.of(0, 2)));
        assertEquals(List.of(0, 1), PathOperations.selectedSegments(3, Set.of(0, 1, 2)));
        assertEquals(List.of(1), PathOperations.selectedSegments(3, Set.of(1, 2)));
    }

    @Test
    void oneCutAddsTheHalfwayNodeWithMixedValues() {
        var result = PathOperations.subdivide(List.of(n(0, 0, 60, 4), n(10, 0, 70, 8)), Set.of(0, 1), 1, 0f);
        assertEquals(List.of(n(0, 0, 60, 4), n(5, 0, 65, 6), n(10, 0, 70, 8)), result.path());
        assertEquals(Set.of(0, 1, 2), result.selection(), "old and new nodes stay selected");
    }

    @Test
    void severalCutsAreEvenlySpacedAndMixByPosition() {
        var result = PathOperations.subdivide(List.of(n(0, 0, 60, 4), n(40, 0, 80, 8)), Set.of(0, 1), 3, 0f);
        assertEquals(5, result.path().size());
        float[] xs = {0, 10, 20, 30, 40};
        float[] zs = {60, 65, 70, 75, 80};
        float[] ws = {4, 5, 6, 7, 8};
        for (int i = 0; i < 5; i++) {
            assertEquals(xs[i], result.path().get(i).x, 1e-6, "x " + i);
            assertEquals(zs[i], result.path().get(i).z, 1e-4, "z " + i);
            assertEquals(ws[i], result.path().get(i).w, 1e-4, "w " + i);
        }
    }

    @Test
    void onlySelectedSegmentsAreSubdividedAndSelectionIsRemapped() {
        var nodes = List.of(n(0, 0, 60, 4), n(20, 0, 60, 4), n(40, 0, 60, 4), n(60, 0, 60, 4));
        var result = PathOperations.subdivide(nodes, Set.of(1, 2), 1, 0f);
        assertEquals(5, result.path().size());
        assertEquals(n(20, 0, 60, 4), result.path().get(1));
        assertEquals(30, result.path().get(2).x, 1e-6);
        assertEquals(n(40, 0, 60, 4), result.path().get(3));
        assertEquals(Set.of(1, 2, 3), result.selection());
        // nothing to subdivide without two selected neighbours
        assertEquals(nodes, PathOperations.subdivide(nodes, Set.of(0, 2), 2, 0f).path());
    }

    @Test
    void newNodesFollowTheCurveNotTheChord() {
        var nodes = List.of(n(0, 0, 60, 4), n(40, 40, 60, 4), n(80, 0, 60, 4), n(120, 40, 60, 4));
        float strength = 1f;
        var result = PathOperations.subdivide(nodes, Set.of(1, 2), 1, strength);
        var inserted = result.path().get(2);
        var curve = PathToolBackend.getPathFromNodes(nodes, strength).path;
        double nearest = curve.stream()
                .mapToDouble(p -> Math.hypot(p.x - inserted.x, p.y - inserted.y))
                .min()
                .orElseThrow();
        assertTrue(nearest <= 1.0, "on the sampled curve (distance " + nearest + ")");
    }

    @Test
    void randomizeIsSeededBoundedAndOnlyMovesSelectedNodes() {
        var nodes = List.of(n(100, 100, 60, 4), n(200, 100, 60, 10), n(300, 100, 60, 4));
        var a = PathOperations.randomize(nodes, Set.of(0, 1), 5, 42);
        var b = PathOperations.randomize(nodes, Set.of(0, 1), 5, 42);
        assertEquals(a.path(), b.path(), "same seed, same result");
        assertNotEquals(a.path(), PathOperations.randomize(nodes, Set.of(0, 1), 5, 43).path());
        assertEquals(nodes.get(2), a.path().get(2));
        for (int seed = 0; seed < 50; seed++) {
            var blocks = PathOperations.randomize(nodes, Set.of(0, 1), 5, seed).path();
            for (int i = 0; i < 2; i++)
                assertTrue(Math.hypot(blocks.get(i).x - nodes.get(i).x, blocks.get(i).y - nodes.get(i).y) <= 5.8);
        }
        assertEquals(nodes, PathOperations.randomize(nodes, Set.of(0, 1), 0, 7).path(), "amount 0");
        assertEquals(Set.of(0, 1), a.selection());
        // independent of the nodes' own radius (node 1 has radius 10, the range is 5
        // blocks)
        for (int seed = 0; seed < 50; seed++) {
            var moved = PathOperations.randomize(nodes, Set.of(1), 5, seed).path().get(1);
            assertTrue(Math.hypot(moved.x - 200, moved.y - 100) <= 5.8);
            assertEquals(10f, moved.w);
        }
    }

    @Test
    void randomizeRadiusStaysWithinMinMaxIsSeededAndKeepsPositions() {
        var nodes = List.of(n(10, 10, 60, 4), n(20, 10, 60, 4), n(30, 10, 60, 4));
        var a = PathOperations.randomizeRadius(nodes, Set.of(0, 1), 3, 12, 5);
        assertEquals(a.path(), PathOperations.randomizeRadius(nodes, Set.of(0, 1), 3, 12, 5).path(), "seeded");
        assertEquals(nodes.get(2), a.path().get(2), "unselected node unchanged");
        boolean varied = false;
        for (int seed = 0; seed < 50; seed++) {
            var path = PathOperations.randomizeRadius(nodes, Set.of(0, 1), 3, 12, seed).path();
            for (int i = 0; i < 2; i++) {
                assertTrue(path.get(i).w >= 3 && path.get(i).w <= 12, "w=" + path.get(i).w);
                assertEquals(nodes.get(i).x, path.get(i).x);
                assertEquals(nodes.get(i).y, path.get(i).y);
                assertEquals(nodes.get(i).z, path.get(i).z);
            }
            varied |= path.get(0).w != path.get(1).w;
        }
        assertTrue(varied, "nodes get individual radii");
        assertEquals(7f, PathOperations.randomizeRadius(nodes, Set.of(0), 7, 7, 1).path().get(0).w, "min = max");
        var swapped = PathOperations.randomizeRadius(nodes, Set.of(0), 12, 3, 1).path().get(0).w;
        assertTrue(swapped >= 3 && swapped <= 12, "min > max is tolerated");
    }

    @Test
    void onlyDownhillLowersSelectedNodesAlongThePath() {
        var nodes = List.of(n(0, 0, 80, 4), n(10, 0, 90, 4), n(20, 0, 85, 4), n(30, 0, 70, 4), n(40, 0, 75, 4));
        var all = PathOperations.onlyDownhill(nodes, Set.of(0, 1, 2, 3, 4)).path();
        float[] expected = {80, 80, 80, 70, 70};
        for (int i = 0; i < 5; i++)
            assertEquals(expected[i], all.get(i).z, "z " + i);
        // unselected nodes keep their height but still constrain the next node
        var some = PathOperations.onlyDownhill(nodes, Set.of(2, 4)).path();
        // only the path counts, not the terrain: a single node keeps its height
        assertEquals(80f, PathOperations.onlyDownhill(List.of(n(0, 0, 80, 4)), Set.of(0)).path().get(0).z,
                "a single node stays where it is");
        assertEquals(90f, some.get(1).z, "unselected");
        assertEquals(85f, some.get(2).z, "already below its predecessor (90)");
        assertEquals(70f, some.get(4).z, "lowered to node 4 (70)");
        assertEquals(nodes.get(0), some.get(0));
        // positions and radii never change
        for (int i = 0; i < 5; i++) {
            assertEquals(nodes.get(i).x, all.get(i).x);
            assertEquals(nodes.get(i).w, all.get(i).w);
        }
    }

    @Test
    void snapPicksLowestHighestOrMedianWithinRadiusPreferringNearest() {
        // terrain: 64 everywhere, a pit of 50 at (103, 104), a peak of 90 at (96, 100),
        // none beyond x=110
        PathOperations.HeightAt heights = (x, y) -> {
            if (x > 110)
                return null;
            if (x == 103 && y == 104)
                return 50f;
            if (x == 96 && y == 100)
                return 90f;
            return 64f;
        };
        var nodes = List.of(n(100, 100, 66, 4), n(0, 0, 60, 4));
        var lowest = PathOperations.snapToTerrain(nodes, Set.of(0), 8, PathOperations.SnapTarget.LOWEST, heights);
        assertEquals(n(103, 104, 66, 4), lowest.path().get(0));
        assertEquals(nodes.get(1), lowest.path().get(1), "unselected nodes stay");
        var highest = PathOperations.snapToTerrain(nodes, Set.of(0), 8, PathOperations.SnapTarget.HIGHEST, heights);
        assertEquals(n(96, 100, 66, 4), highest.path().get(0));
        // the median is 64 which the node already stands on: nearest wins
        var median = PathOperations.snapToTerrain(nodes, Set.of(0), 8, PathOperations.SnapTarget.MEDIAN, heights);
        assertEquals(n(100, 100, 66, 4), median.path().get(0));
        // out of reach: the pit is 5 blocks away
        var near = PathOperations.snapToTerrain(nodes, Set.of(0), 4, PathOperations.SnapTarget.LOWEST, heights);
        assertEquals(n(100, 100, 66, 4), near.path().get(0));
    }
}
