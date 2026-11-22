package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.vecmath.Point4f;

/**
 * Node and segment operations of the Path Tool's "Node" and "Segment" menus.
 * Pure functions on node lists (x, y, height, radius) so they can be previewed
 * and tested without Swing or a WorldPainter dimension. They only move nodes
 * horizontally; the caller keeps each moved node's height above ground.
 */
final class PathOperations
{
    private PathOperations() {
    }

    /**
     * New node list and the selection (indices into it) after an operation;
     * {@code inserted} marks nodes the operation added (drawn distinctly until
     * applied).
     */
    record Result(ArrayList<Point4f> path, Set<Integer> selection, Set<Integer> inserted) {
        Result(ArrayList<Point4f> path, Set<Integer> selection) {
            this(path, selection, Set.of());
        }
    }

    /** Terrain height at a block, or {@code null} where the map has no tile. */
    interface HeightAt
    {
        Float at(int x, int y);
    }

    enum SnapTarget {
        LOWEST("Lowest"), HIGHEST("Highest"), MEDIAN("Median");

        private final String label;

        SnapTarget(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /**
     * Runs of two or more neighbouring nodes on the same block (indices into
     * {@code path}). Operations like snapping clump nodes together; such neighbours
     * are merged on apply. Non-neighbouring nodes on one block are a legitimate
     * self-crossing and are left alone.
     */
    static List<int[]> coincidentRuns(List<Point4f> path) {
        var runs = new ArrayList<int[]>();
        int start = 0;
        for (int i = 1; i <= path.size(); i++) {
            boolean same = i < path.size() && Math.round(path.get(i).x) == Math.round(path.get(start).x)
                    && Math.round(path.get(i).y) == Math.round(path.get(start).y);
            if (same)
                continue;
            if (i - start >= 2) {
                int[] run = new int[i - start];
                for (int k = 0; k < run.length; k++)
                    run[k] = start + k;
                runs.add(run);
            }
            start = i;
        }
        return runs;
    }

    /**
     * Merges every run of neighbouring nodes on the same block into one node
     * (height and radius averaged); the merged node is selected if any of its nodes
     * was.
     */
    static Result mergeCoincident(Result result) {
        return mergeRuns(result, coincidentRuns(result.path()));
    }

    /**
     * The coincident runs an operation created: runs containing a node it moved to
     * a different block or a node it inserted. Neighbours that already shared a
     * block before the operation are left alone (the curve copes with them), so
     * e.g. a height-only operation never merges anything.
     */
    static List<int[]> createdCoincidentRuns(List<Point4f> before, Result after) {
        var created = new ArrayList<int[]>();
        boolean sameNodes = before.size() == after.path().size();
        for (int[] run : coincidentRuns(after.path()))
            for (int index : run) {
                boolean inserted = after.inserted().contains(index);
                boolean moved = sameNodes && (Math.round(before.get(index).x) != Math.round(after.path().get(index).x)
                        || Math.round(before.get(index).y) != Math.round(after.path().get(index).y));
                if (inserted || moved) {
                    created.add(run);
                    break;
                }
            }
        return created;
    }

    /** Merges the given runs (from {@link #coincidentRuns}) into one node each. */
    static Result mergeRuns(Result result, List<int[]> runs) {
        var path = result.path();
        if (runs.isEmpty())
            return result;
        var merged = new ArrayList<Point4f>();
        var selection = new LinkedHashSet<Integer>();
        var inserted = new LinkedHashSet<Integer>();
        int run = 0;
        for (int i = 0; i < path.size();) {
            int end = i;
            if (run < runs.size() && runs.get(run)[0] == i)
                end = runs.get(run++)[runs.get(run - 1).length - 1];
            var node = new Point4f(path.get(i));
            float z = 0, w = 0;
            boolean selected = false, isNew = true;
            for (int k = i; k <= end; k++) {
                z += path.get(k).z;
                w += path.get(k).w;
                selected |= result.selection().contains(k);
                isNew &= result.inserted().contains(k);
            }
            node.z = z / (end - i + 1);
            node.w = w / (end - i + 1);
            if (selected)
                selection.add(merged.size());
            if (isNew)
                inserted.add(merged.size());
            merged.add(node);
            i = end + 1;
        }
        return new Result(merged, selection, inserted);
    }

    /** Segments (by start node) whose both end nodes are selected. */
    static List<Integer> selectedSegments(int pathSize, Set<Integer> selection) {
        var segments = new ArrayList<Integer>();
        for (int i = 0; i + 1 < pathSize; i++)
            if (selection.contains(i) && selection.contains(i + 1))
                segments.add(i);
        return segments;
    }

    /**
     * Inserts {@code cuts} nodes into every selected segment, evenly spaced by
     * length along the sampled curve (one cut = the segment's halfway point), so
     * the path keeps its shape. New nodes mix their neighbours' height and radius
     * by position. The selection keeps the old nodes and adds the new ones.
     */
    static Result subdivide(List<Point4f> nodes, Set<Integer> selection, int cuts, float curveStrength) {
        var segments = new LinkedHashSet<>(selectedSegments(nodes.size(), selection));
        List<Point4f> curve = nodes.size() < 2
                ? List.of()
                : PathToolBackend.getPathFromNodes(nodes, curveStrength).path;
        int[] segmentOf = PathEditHandles.segmentIndices(nodes, curve);
        var path = new ArrayList<Point4f>();
        var newSelection = new LinkedHashSet<Integer>();
        var inserted = new LinkedHashSet<Integer>();
        for (int i = 0; i < nodes.size(); i++) {
            if (selection.contains(i))
                newSelection.add(path.size());
            path.add(new Point4f(nodes.get(i)));
            if (!segments.contains(i) || cuts <= 0)
                continue;
            Point4f from = nodes.get(i), to = nodes.get(i + 1);
            var polyline = new ArrayList<double[]>();
            for (int k = 0; k < curve.size(); k++)
                if (segmentOf[k] == i)
                    polyline.add(new double[]{curve.get(k).x, curve.get(k).y});
            if (polyline.isEmpty())
                polyline.add(new double[]{from.x, from.y});
            polyline.add(new double[]{to.x, to.y});
            for (int cut = 1; cut <= cuts; cut++) {
                float t = cut / (float) (cuts + 1);
                double[] position = pointAtFraction(polyline, t);
                inserted.add(path.size());
                newSelection.add(path.size());
                path.add(new Point4f(Math.round(position[0]), Math.round(position[1]), lerp(from.z, to.z, t),
                        lerp(from.w, to.w, t)));
            }
        }
        return new Result(path, newSelection, inserted);
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    /** Point at a fraction of the polyline's total length. */
    static double[] pointAtFraction(List<double[]> polyline, double fraction) {
        double total = 0;
        for (int k = 1; k < polyline.size(); k++)
            total += distance(polyline.get(k - 1), polyline.get(k));
        if (total <= 0)
            return polyline.get(0).clone();
        double target = fraction * total, walked = 0;
        for (int k = 1; k < polyline.size(); k++) {
            double[] a = polyline.get(k - 1), b = polyline.get(k);
            double step = distance(a, b);
            if (walked + step >= target && step > 0) {
                double t = (target - walked) / step;
                return new double[]{a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t};
            }
            walked += step;
        }
        return polyline.get(polyline.size() - 1).clone();
    }

    private static double distance(double[] a, double[] b) {
        return Math.hypot(b[0] - a[0], b[1] - a[1]);
    }

    /**
     * Moves every selected node to a random position within {@code radius} blocks,
     * uniformly over that disk. The randomize radius is independent of the nodes'
     * own radius. The same seed gives the same result, so the preview is stable
     * while other settings change.
     */
    static Result randomize(List<Point4f> nodes, Set<Integer> selection, double radius, long seed) {
        var random = new Random(seed);
        var path = new ArrayList<Point4f>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            var node = new Point4f(nodes.get(i));
            if (selection.contains(i)) {
                double range = Math.max(0, radius);
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = range * Math.sqrt(random.nextDouble());
                node.x = Math.round(node.x + Math.cos(angle) * distance);
                node.y = Math.round(node.y + Math.sin(angle) * distance);
            }
            path.add(node);
        }
        return new Result(path, new LinkedHashSet<>(selection));
    }

    /**
     * Gives every selected node a random radius between {@code min} and {@code max}
     * blocks (uniform, to 0.1). The same seed gives the same result.
     */
    static Result randomizeRadius(List<Point4f> nodes, Set<Integer> selection, double min, double max, long seed) {
        var random = new Random(seed);
        double low = Math.max(1, Math.min(min, max)), high = Math.max(low, Math.max(min, max));
        var path = new ArrayList<Point4f>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            var node = new Point4f(nodes.get(i));
            if (selection.contains(i))
                node.w = Math.round((low + random.nextDouble() * (high - low)) * 10) / 10f;
            path.add(node);
        }
        return new Result(path, new LinkedHashSet<>(selection));
    }

    /**
     * Lowers selected nodes so the path never rises along its direction and never
     * floats above the terrain: walking from node 1 to n, each selected node
     * becomes {@code min(previous node, itself, terrain height here)} (the previous
     * node may itself just have been lowered). Unselected nodes keep their height
     * but still count as the previous node. Blocks without terrain skip the terrain
     * term.
     */
    static Result onlyDownhill(List<Point4f> nodes, Set<Integer> selection, HeightAt terrain) {
        var path = new ArrayList<Point4f>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            var node = new Point4f(nodes.get(i));
            if (selection.contains(i)) {
                if (i > 0)
                    node.z = Math.min(node.z, path.get(i - 1).z);
                Float ground = terrain.at(Math.round(node.x), Math.round(node.y));
                if (ground != null)
                    node.z = Math.min(node.z, ground);
            }
            path.add(node);
        }
        return new Result(path, new LinkedHashSet<>(selection));
    }

    /**
     * Moves every selected node to the lowest, highest or median terrain position
     * within {@code radius} blocks; among equally good positions the one nearest to
     * the node wins. Blocks without terrain are ignored.
     */
    static Result snapToTerrain(List<Point4f> nodes, Set<Integer> selection, int radius, SnapTarget target,
            HeightAt heights) {
        var path = new ArrayList<Point4f>(nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            var node = new Point4f(nodes.get(i));
            if (selection.contains(i)) {
                int[] best = snapPosition(Math.round(node.x), Math.round(node.y), Math.max(0, radius), target, heights);
                if (best != null) {
                    node.x = best[0];
                    node.y = best[1];
                }
            }
            path.add(node);
        }
        return new Result(path, new LinkedHashSet<>(selection));
    }

    private static int[] snapPosition(int cx, int cy, int radius, SnapTarget target, HeightAt heights) {
        var xs = new ArrayList<Integer>();
        var ys = new ArrayList<Integer>();
        var values = new ArrayList<Float>();
        for (int dx = -radius; dx <= radius; dx++)
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx * dx + dy * dy > radius * radius)
                    continue;
                Float height = heights.at(cx + dx, cy + dy);
                if (height == null || !Float.isFinite(height))
                    continue;
                xs.add(cx + dx);
                ys.add(cy + dy);
                values.add(height);
            }
        if (values.isEmpty())
            return null;
        float goal;
        switch (target) {
            case LOWEST -> goal = values.stream().min(Float::compare).orElseThrow();
            case HIGHEST -> goal = values.stream().max(Float::compare).orElseThrow();
            default -> {
                float[] sorted = new float[values.size()];
                for (int k = 0; k < sorted.length; k++)
                    sorted[k] = values.get(k);
                Arrays.sort(sorted);
                goal = sorted[sorted.length / 2];
            }
        }
        int best = -1;
        double bestScore = Double.MAX_VALUE, bestDistance = Double.MAX_VALUE;
        for (int k = 0; k < values.size(); k++) {
            double score = Math.abs(values.get(k) - goal);
            double distance = Math.hypot(xs.get(k) - cx, ys.get(k) - cy);
            if (score < bestScore - 1e-6 || (Math.abs(score - bestScore) <= 1e-6 && distance < bestDistance)) {
                best = k;
                bestScore = score;
                bestDistance = distance;
            }
        }
        return new int[]{xs.get(best), ys.get(best)};
    }
}
