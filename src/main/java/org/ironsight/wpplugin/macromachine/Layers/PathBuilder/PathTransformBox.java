package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

/**
 * View-space geometry of the Transform editor (like Inkscape's selection box):
 * a box around the selected nodes with scale handles on its corners, a rotate
 * handle above it, a move handle below it and a draggable pivot for rotating
 * and scaling. Pure functions so the mapping between mouse and transform can be
 * tested without Swing. Angles are in view space, where y points down.
 */
final class PathTransformBox
{
    /** Space between the outermost selected nodes and the box (px). */
    static final double PADDING = 16;
    /** Length of the stems carrying the rotate and move handles (px). */
    static final double STEM = 28;
    /** Smallest scale factor (a factor of 0 would put all nodes on the pivot). */
    static final double MIN_SCALE = 0.01;

    private PathTransformBox() {
    }

    /**
     * Box around the selected nodes, padded so it never collapses onto a single
     * node or a straight line of nodes; {@code null} without a selection.
     */
    static Rectangle2D.Double box(List<? extends Point2D> viewNodes, Collection<Integer> selected) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        boolean any = false;
        for (int i : selected) {
            if (i < 0 || i >= viewNodes.size())
                continue;
            Point2D node = viewNodes.get(i);
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX());
            maxY = Math.max(maxY, node.getY());
            any = true;
        }
        if (!any)
            return null;
        return new Rectangle2D.Double(minX - PADDING, minY - PADDING, maxX - minX + 2 * PADDING,
                maxY - minY + 2 * PADDING);
    }

    /**
     * Box around the selected nodes in the frame of a box turned by {@code degrees}
     * (counter-clockwise on screen): the nodes are turned back first;
     * {@link #frame} maps the box and its handles to the view.
     */
    static Rectangle2D.Double box(List<? extends Point2D> viewNodes, Collection<Integer> selected, double degrees) {
        if (degrees == 0)
            return box(viewNodes, selected);
        var back = frame(-degrees);
        var turned = new java.util.ArrayList<Point2D>(viewNodes.size());
        for (Point2D node : viewNodes)
            turned.add(back.transform(node, null));
        return box(turned, selected);
    }

    /** From the frame of a box turned by {@code degrees} to the view. */
    static AffineTransform frame(double degrees) {
        return rotation(new Point2D.Double(), degrees);
    }

    /**
     * Corner {@code index}: 0 top left, 1 top right, 2 bottom right, 3 bottom left.
     */
    static Point2D.Double corner(Rectangle2D box, int index) {
        return switch (index) {
            case 0 -> new Point2D.Double(box.getMinX(), box.getMinY());
            case 1 -> new Point2D.Double(box.getMaxX(), box.getMinY());
            case 2 -> new Point2D.Double(box.getMaxX(), box.getMaxY());
            case 3 -> new Point2D.Double(box.getMinX(), box.getMaxY());
            default -> throw new IllegalArgumentException("corner " + index);
        };
    }

    static Point2D.Double topCentre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMinY());
    }

    static Point2D.Double bottomCentre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMaxY());
    }

    /** The rotate handle, on a stem above the box. */
    static Point2D.Double rotateHandle(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMinY() - STEM);
    }

    /** The move handle, on a stem below the box. */
    static Point2D.Double moveHandle(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMaxY() + STEM);
    }

    static Point2D.Double centre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getCenterY());
    }

    /**
     * Rotation the mouse asks for: degrees counter-clockwise on screen from
     * {@code start} to {@code now} around {@code pivot}, in (-180, 180], rounded to
     * whole degrees.
     */
    static double rotationDegrees(Point2D pivot, Point2D start, Point2D now) {
        double from = Math.atan2(start.getY() - pivot.getY(), start.getX() - pivot.getX());
        double to = Math.atan2(now.getY() - pivot.getY(), now.getX() - pivot.getX());
        double degrees = -Math.toDegrees(to - from); // y points down: negate for counter-clockwise
        while (degrees <= -180)
            degrees += 360;
        while (degrees > 180)
            degrees -= 360;
        return Math.round(degrees);
    }

    /**
     * Uniform scale factor the mouse asks for: how much further from (or closer to)
     * the pivot the cursor is than where the drag started; to 0.01, at least
     * {@link #MIN_SCALE}.
     */
    static double scaleFactor(Point2D pivot, Point2D start, Point2D now) {
        double from = pivot.distance(start);
        if (from < 1e-9)
            return 1;
        return clampScale(Math.round(pivot.distance(now) / from * 100) / 100.0);
    }

    static double clampScale(double factor) {
        return Math.max(MIN_SCALE, factor);
    }

    /**
     * Rotation by {@code degrees} counter-clockwise on screen around {@code pivot}.
     */
    static AffineTransform rotation(Point2D pivot, double degrees) {
        return AffineTransform.getRotateInstance(-Math.toRadians(degrees), pivot.getX(), pivot.getY());
    }

    /** Uniform scaling by {@code factor} around {@code pivot}. */
    static AffineTransform scaling(Point2D pivot, double factor) {
        var transform = AffineTransform.getTranslateInstance(pivot.getX(), pivot.getY());
        transform.scale(factor, factor);
        transform.translate(-pivot.getX(), -pivot.getY());
        return transform;
    }
}
