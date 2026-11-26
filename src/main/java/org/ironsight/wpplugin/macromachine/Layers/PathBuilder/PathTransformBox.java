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
public final class PathTransformBox
{
    /** Space between the outermost selected nodes and the box (px). */
    public static final double PADDING = 16;
    /** Length of the stems carrying the rotate and move handles (px). */
    public static final double STEM = 28;
    /** Smallest scale factor (a factor of 0 would put all nodes on the pivot). */
    public static final double MIN_SCALE = 0.01;

    private PathTransformBox() {
    }

    /**
     * Box around the selected nodes, padded so it never collapses onto a single
     * node or a straight line of nodes; {@code null} without a selection.
     */
    public static Rectangle2D.Double box(List<? extends Point2D> viewNodes, Collection<Integer> selected) {
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
    public static Rectangle2D.Double box(List<? extends Point2D> viewNodes, Collection<Integer> selected,
            double degrees) {
        if (degrees == 0)
            return box(viewNodes, selected);
        var back = frame(-degrees);
        var turned = new java.util.ArrayList<Point2D>(viewNodes.size());
        for (Point2D node : viewNodes)
            turned.add(back.transform(node, null));
        return box(turned, selected);
    }

    /** From the frame of a box turned by {@code degrees} to the view. */
    public static AffineTransform frame(double degrees) {
        return rotation(new Point2D.Double(), degrees);
    }

    /**
     * Corner {@code index}: 0 top left, 1 top right, 2 bottom right, 3 bottom left.
     */
    public static Point2D.Double corner(Rectangle2D box, int index) {
        return switch (index) {
            case 0 -> new Point2D.Double(box.getMinX(), box.getMinY());
            case 1 -> new Point2D.Double(box.getMaxX(), box.getMinY());
            case 2 -> new Point2D.Double(box.getMaxX(), box.getMaxY());
            case 3 -> new Point2D.Double(box.getMinX(), box.getMaxY());
            default -> throw new IllegalArgumentException("corner " + index);
        };
    }

    public static Point2D.Double topCentre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMinY());
    }

    public static Point2D.Double bottomCentre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMaxY());
    }

    /** The rotate handle, on a stem above the box. */
    public static Point2D.Double rotateHandle(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getMinY() - STEM);
    }

    public static Point2D.Double leftCentre(Rectangle2D box) {
        return new Point2D.Double(box.getMinX(), box.getCenterY());
    }

    public static Point2D.Double rightCentre(Rectangle2D box) {
        return new Point2D.Double(box.getMaxX(), box.getCenterY());
    }

    /** The move handle, on a stem left of the box. */
    public static Point2D.Double moveHandle(Rectangle2D box) {
        return new Point2D.Double(box.getMinX() - STEM, box.getCenterY());
    }

    /**
     * Scale handles: uniform (bottom right corner), along the box's x and y axes.
     */
    public static final int SCALE_UNIFORM = 0, SCALE_X = 1, SCALE_Y = 2;

    /**
     * Where the scale handle {@code kind} sits: corner, right edge, bottom edge.
     */
    public static Point2D.Double scaleHandle(Rectangle2D box, int kind) {
        return switch (kind) {
            case SCALE_UNIFORM -> corner(box, 2);
            case SCALE_X -> rightCentre(box);
            case SCALE_Y -> bottomCentre(box);
            default -> throw new IllegalArgumentException("scale handle " + kind);
        };
    }

    /** Distance of the move handle below a single selected node (px). */
    public static final double SINGLE_MOVE_OFFSET = 26;

    /**
     * A single selected node only gets the move handle: just below the node (no
     * box, no stem).
     */
    public static Point2D.Double singleMoveHandle(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getCenterY() + SINGLE_MOVE_OFFSET);
    }

    public static Point2D.Double centre(Rectangle2D box) {
        return new Point2D.Double(box.getCenterX(), box.getCenterY());
    }

    /**
     * Rotation the mouse asks for: degrees counter-clockwise on screen from
     * {@code start} to {@code now} around {@code pivot}, in (-180, 180], rounded to
     * whole degrees.
     */
    public static double rotationDegrees(Point2D pivot, Point2D start, Point2D now) {
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
    public static double scaleFactor(Point2D pivot, Point2D start, Point2D now) {
        double from = pivot.distance(start);
        if (from < 1e-9)
            return 1;
        return clampScale(Math.round(pivot.distance(now) / from * 100) / 100.0);
    }

    public static double clampScale(double factor) {
        return Math.max(MIN_SCALE, factor);
    }

    /**
     * Rotation by {@code degrees} counter-clockwise on screen around {@code pivot}.
     */
    public static AffineTransform rotation(Point2D pivot, double degrees) {
        return AffineTransform.getRotateInstance(-Math.toRadians(degrees), pivot.getX(), pivot.getY());
    }

    /** Uniform scaling by {@code factor} around {@code pivot}. */
    /**
     * Scale factor along one axis of a box turned by {@code degrees}
     * ({@link #SCALE_X} or {@link #SCALE_Y}): how much further from the pivot the
     * cursor is along that axis than where the drag started; to 0.01, at least
     * {@link #MIN_SCALE}; 1 when grabbed level with the pivot.
     */
    public static double axisScaleFactor(Point2D pivot, Point2D start, Point2D now, double degrees, int axis) {
        var back = frame(-degrees);
        var from = back.deltaTransform(new Point2D.Double(start.getX() - pivot.getX(), start.getY() - pivot.getY()),
                null);
        var to = back.deltaTransform(new Point2D.Double(now.getX() - pivot.getX(), now.getY() - pivot.getY()), null);
        double grabbed = axis == SCALE_X ? from.getX() : from.getY();
        double current = axis == SCALE_X ? to.getX() : to.getY();
        if (Math.abs(grabbed) < 1e-9)
            return 1;
        return clampScale(Math.round(current / grabbed * 100) / 100.0);
    }

    /**
     * Scaling by {@code sx} and {@code sy} along the axes of a box turned by
     * {@code degrees}, around {@code pivot}.
     */
    public static AffineTransform axisScaling(Point2D pivot, double sx, double sy, double degrees) {
        var transform = AffineTransform.getTranslateInstance(pivot.getX(), pivot.getY());
        transform.concatenate(frame(degrees));
        transform.scale(sx, sy);
        transform.concatenate(frame(-degrees));
        transform.translate(-pivot.getX(), -pivot.getY());
        return transform;
    }

    public static AffineTransform scaling(Point2D pivot, double factor) {
        var transform = AffineTransform.getTranslateInstance(pivot.getX(), pivot.getY());
        transform.scale(factor, factor);
        transform.translate(-pivot.getX(), -pivot.getY());
        return transform;
    }
}
