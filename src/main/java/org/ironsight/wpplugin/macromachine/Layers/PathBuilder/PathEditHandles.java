package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.geom.Point2D;
import java.util.List;
import javax.vecmath.Tuple4f;

/**
 * View-space geometry for the Path Tool's per-node edit handles (radius and
 * curve strength). Pure functions so the mapping between handle positions and
 * values can be tested without Swing.
 */
final class PathEditHandles
{
    /**
     * Minimum distance in pixels between a node centre and any of its handles, so
     * handles never hide under the node.
     */
    static final double MIN_HANDLE_OFFSET = 22;
    /**
     * Clickable radius of handles (px): a 24 x 24 px target, the common minimum for
     * mouse pointers (WCAG 2.5.8), larger than the drawn handle.
     */
    static final double HANDLE_HIT_RADIUS = 12;
    static final float MIN_STRENGTH = -1f, MAX_STRENGTH = 2f;

    private PathEditHandles() {
    }

    /**
     * For each curve sample, the index of the last node the curve has passed
     * through (-1 before the first node, e.g. on padding added for short paths).
     * The edge between samples {@code k-1} and {@code k} belongs to the segment
     * starting at node {@code result[k-1]}. The sampled curve runs through every
     * node's rounded position in order, so nodes are matched by exact block
     * position; several nodes on the same block are passed at once.
     */
    static int[] segmentIndices(List<? extends Tuple4f> nodes, List<? extends Tuple4f> curve) {
        int[] result = new int[curve.size()];
        int passed = -1;
        for (int k = 0; k < curve.size(); k++) {
            var sample = curve.get(k);
            int x = Math.round(sample.x), y = Math.round(sample.y);
            while (passed + 1 < nodes.size() && Math.round(nodes.get(passed + 1).x) == x
                    && Math.round(nodes.get(passed + 1).y) == y)
                passed++;
            result[k] = passed;
        }
        return result;
    }

    /** A segment is highlighted when the nodes at both of its ends are selected. */
    static boolean segmentSelected(int segment, java.util.Set<Integer> selectedNodes) {
        return segment >= 0 && selectedNodes.contains(segment) && selectedNodes.contains(segment + 1);
    }

    /**
     * Screen pixels per block of height above/below ground for the height handle.
     */
    static final double HEIGHT_PX_PER_BLOCK = 3;

    /**
     * Height handle on the gauge's vertical axis: on the ground line at 0, above it
     * for a node above ground, below it for a node below ground. The gauge sits
     * beside the node, so unlike the other handles it needs no minimum offset.
     */
    static Point2D.Double heightHandle(Point2D groundOnAxis, float relativeHeight) {
        return new Point2D.Double(groundOnAxis.getX(), groundOnAxis.getY() - relativeHeight * HEIGHT_PX_PER_BLOCK);
    }

    /**
     * Inverse of {@link #heightHandle}: relative height (unrounded) for a cursor
     * position; only the vertical component counts.
     */
    static double relativeHeightAt(Point2D groundOnAxis, Point2D point) {
        return (groundOnAxis.getY() - point.getY()) / HEIGHT_PX_PER_BLOCK;
    }

    static String signed(float value) {
        String text = PathTool.formatValue(Math.abs(value));
        return value > 0 ? "+" + text : value < 0 ? "-" + text : "0";
    }

    /**
     * Radius handle sits on the radius circle to the right of the node, but never
     * closer than the minimum offset.
     */
    static Point2D.Double radiusHandle(Point2D center, double radiusPx) {
        return new Point2D.Double(center.getX() + Math.max(radiusPx, MIN_HANDLE_OFFSET), center.getY());
    }

    /**
     * Scales a radius by how far the handle was dragged away from / towards the
     * node centre, relative to where the drag started. Relative scaling keeps the
     * drag continuous even when the handle was clamped to the minimum offset.
     */
    static float scaleFactor(Point2D center, Point2D dragStart, Point2D dragCurrent) {
        double start = center.distance(dragStart);
        if (start < 1e-6)
            return 1f;
        return (float) (center.distance(dragCurrent) / start);
    }

    static float scaledRadius(float radius, float factor, boolean roundToInteger) {
        float scaled = Math.max(1f, radius * factor);
        return roundToInteger ? Math.max(1, Math.round(scaled)) : Math.round(scaled * 10f) / 10f;
    }

    /**
     * Tangent handle geometry for one node; {@code side} is +1 for the forward
     * handle, -1 for the backward one.
     */
    record Tangent(Point2D.Double center, double dirX, double dirY, double halfPrev, double halfNext) {
        double half(int side) {
            return side > 0 ? halfNext : halfPrev;
        }

        /**
         * Signed offset of a handle along its outward direction for the given strength.
         */
        double offset(int side, float strength) {
            double length = Math.abs(strength) * half(side);
            return strength >= 0 ? MIN_HANDLE_OFFSET + length : -(MIN_HANDLE_OFFSET + length);
        }

        Point2D.Double handle(int side, float strength) {
            double offset = offset(side, strength) * side;
            return new Point2D.Double(center.x + dirX * offset, center.y + dirY * offset);
        }

        /**
         * Inverse of {@link #handle}: the strength whose handle lies at the projection
         * of {@code point} onto the tangent. Positions within the minimum offset of the
         * node snap to 0 (straight segments).
         */
        float strengthAt(int side, Point2D point) {
            double projection = ((point.getX() - center.x) * dirX + (point.getY() - center.y) * dirY) * side;
            double half = Math.max(half(side), 1);
            double strength;
            if (projection >= MIN_HANDLE_OFFSET)
                strength = (projection - MIN_HANDLE_OFFSET) / half;
            else if (projection <= -MIN_HANDLE_OFFSET)
                strength = (projection + MIN_HANDLE_OFFSET) / half;
            else
                strength = 0;
            strength = Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength));
            return Math.round(strength * 20) / 20f;
        }
    }

    /**
     * Tangent through node {@code index}: direction of the line between its
     * neighbours (or to its only neighbour at either end), matching the Catmull-Rom
     * tangent used to place the Bézier control points. Handle length per unit
     * strength is half the distance to the neighbour on that side, mirroring how
     * the curve scales control points.
     *
     * @return {@code null} when the node has no neighbour or all neighbours
     *         coincide with it.
     */
    static Tangent tangent(List<? extends Point2D> nodes, int index) {
        if (nodes.size() < 2 || index < 0 || index >= nodes.size())
            return null;
        Point2D center = nodes.get(index);
        Point2D prev = index > 0 ? nodes.get(index - 1) : null;
        Point2D next = index < nodes.size() - 1 ? nodes.get(index + 1) : null;
        Point2D from = prev != null ? prev : center;
        Point2D to = next != null ? next : center;
        double dx = to.getX() - from.getX(), dy = to.getY() - from.getY();
        double length = Math.hypot(dx, dy);
        if (length < 1e-6)
            return null;
        double halfPrev = prev != null ? center.distance(prev) / 2 : center.distance(next) / 2;
        double halfNext = next != null ? center.distance(next) / 2 : center.distance(prev) / 2;
        return new Tangent(new Point2D.Double(center.getX(), center.getY()), dx / length, dy / length, halfPrev,
                halfNext);
    }
}
