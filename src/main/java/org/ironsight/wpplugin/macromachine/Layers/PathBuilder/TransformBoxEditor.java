package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Locale;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.ironsight.wpplugin.macromachine.Gui.OverlayStyle;

/**
 * The transform editor shared by the map tools (Path Tool nodes, City Tool
 * houses), like Inkscape's selection box: a box around the selection with a
 * move handle, a rotate handle, scale handles (uniform, along the box's x and
 * y) and a draggable pivot. The geometry is {@link PathTransformBox}; this
 * class holds the behaviour:
 * <ul>
 * <li>the box angle and a placed pivot belong to the selection they were made
 * for (an owner object the tool hands in, compared with {@code equals}); a new
 * selection starts with an upright box and the pivot on its centre;</li>
 * <li>rotating turns the box with the selection, and it keeps that angle;</li>
 * <li>a placed pivot travels with a move (also while dragging) and stays put
 * through rotating and scaling; a double click on it puts it back on the
 * centre;</li>
 * <li>the transform of a drag is kept in world terms (pivot, degrees, factors,
 * offset), so panning or zooming mid-drag keeps box, pivot and selection
 * together.</li>
 * </ul>
 * The tool turns the transform into its own data (nodes on whole blocks, houses
 * facing quarter turns) and draws the selection; this class draws the box and
 * its handles.
 */
public final class TransformBoxEditor
{
    public enum Handle {
        MOVE, ROTATE, SCALE, PIVOT
    }

    /**
     * @param axis
     *            scale handles: {@link PathTransformBox#SCALE_UNIFORM},
     *            {@link PathTransformBox#SCALE_X} or
     *            {@link PathTransformBox#SCALE_Y}; otherwise 0
     */
    public record Hit(Handle handle, int axis) {
    }

    /**
     * The box as drawn: {@code box} in the frame that {@code frame} maps to the
     * view.
     */
    public record ShownBox(AffineTransform frame, Rectangle2D.Double box) {
    }

    /**
     * What the running drag shows in the viewport header: {@code typeable} values
     * (rotate, scale) can be typed in the Path Tool.
     */
    public record Readout(String label, String value, boolean typeable) {
    }

    // ---- per selection ----
    private double angle;
    private Object angleOwner;
    private Point2D.Double pivot;
    private Object pivotOwner;
    private long lastPivotClickAt;

    // ---- the running drag ----
    private Handle kind;
    private int axis;
    private boolean quarterTurns;
    private Point2D.Double pivotStart;
    private Point2D.Double grab;
    private Handle op;
    private double degrees;
    private double scaleX = 1, scaleY = 1;
    private int moveX, moveY;
    private Point2D.Double draggedPivot;

    /**
     * Angle of the box (degrees, counter-clockwise on screen): 0 for a new
     * selection.
     */
    public double boxAngle(Object selection) {
        return Objects.equals(angleOwner, selection) ? angle : 0;
    }

    /** The box angle as drawn: turned along while rotating. */
    public double shownAngle(Object selection) {
        return boxAngle(selection) + (op == Handle.ROTATE ? degrees : 0);
    }

    /**
     * After a transform of {@code selection}: keeps the box angle, turned by
     * {@code by} degrees.
     */
    public void turnBox(Object selection, double by) {
        double turned = boxAngle(selection) + by;
        angle = turned - 360 * Math.floor((turned + 180) / 360);
        angleOwner = selection;
    }

    /**
     * The selection is now {@code after} (e.g. its keys changed by an edit that is
     * no new selection): box angle and placed pivot stay with it.
     */
    public void carryOver(Object before, Object after) {
        if (Objects.equals(angleOwner, before))
            angleOwner = after;
        if (Objects.equals(pivotOwner, before))
            pivotOwner = after;
    }

    /** The pivot was placed by the user for {@code selection}. */
    public boolean customPivot(Object selection) {
        return pivot != null && Objects.equals(pivotOwner, selection);
    }

    /**
     * Pivot for rotating and scaling (world): placed by the user, else
     * {@code centre}.
     */
    public Point2D.Double pivot(Object selection, Supplier<Point2D.Double> centre) {
        return customPivot(selection) ? new Point2D.Double(pivot.x, pivot.y) : centre.get();
    }

    /** Moves a placed pivot along with its selection (keyboard moves). */
    public void movePivot(Object selection, double deltaX, double deltaY) {
        if (customPivot(selection))
            pivot = new Point2D.Double(pivot.x + deltaX, pivot.y + deltaY);
    }

    public void resetPivot() {
        pivot = null;
    }

    /**
     * Pivot as shown (world): while dragged, where it is dragged to; during a move,
     * carried along with the selection; otherwise where it is.
     */
    public Point2D.Double displayedPivot(Object selection, Supplier<Point2D.Double> centre) {
        if (draggedPivot != null)
            return draggedPivot;
        if (pivotStart == null)
            return pivot(selection, centre);
        if (op == Handle.MOVE)
            return new Point2D.Double(pivotStart.x + moveX, pivotStart.y + moveY);
        return pivotStart;
    }

    // ---- dragging ----

    /**
     * A handle was grabbed.
     *
     * @param pivotNow
     *            the pivot when the drag starts (world)
     * @param grabWorld
     *            where the handle was grabbed (world, not rounded to blocks)
     * @param quarterTurns
     *            rotations snap to quarter turns (a single house)
     */
    public void start(Hit hit, Point2D.Double pivotNow, Point2D.Double grabWorld, boolean quarterTurns) {
        cancel();
        kind = hit.handle();
        axis = hit.axis();
        this.quarterTurns = quarterTurns;
        pivotStart = pivotNow;
        grab = grabWorld;
    }

    /**
     * A rotation typed without a drag (Path Tool: number keys in the Transform
     * editor).
     */
    public void typedRotation(Point2D.Double pivotNow, double typedDegrees, boolean edited) {
        pivotStart = pivotNow;
        degrees = edited ? typedDegrees : 0;
        op = edited ? Handle.ROTATE : null;
    }

    public boolean dragging() {
        return kind != null;
    }

    /** The grabbed handle, or {@code null}. */
    public Handle kind() {
        return kind;
    }

    /** Kind of transform applied so far (rotate, scale, move), or {@code null}. */
    public Handle op() {
        return op;
    }

    public double degrees() {
        return degrees;
    }

    public double scaleX() {
        return scaleX;
    }

    public double scaleY() {
        return scaleY;
    }

    public int moveX() {
        return moveX;
    }

    public int moveY() {
        return moveY;
    }

    /** Where the handle was grabbed (world), or {@code null}. */
    public Point2D.Double grab() {
        return grab;
    }

    /** The pivot where the drag started (world), or {@code null}. */
    public Point2D.Double pivotStart() {
        return pivotStart;
    }

    /**
     * The cursor is at {@code nowWorld} (the virtual, possibly precise cursor, in
     * world terms). {@code typed} turns the mouse value into a typed one (identity
     * when nothing is typed); {@code boxAngle} is the box's angle for the
     * selection. Returns what the header shows.
     */
    public Readout update(Point2D.Double nowWorld, double boxAngle, DoubleUnaryOperator typed) {
        switch (kind) {
            case PIVOT -> {
                draggedPivot = new Point2D.Double(Math.round(pivotStart.x + nowWorld.x - grab.x),
                        Math.round(pivotStart.y + nowWorld.y - grab.y));
                return new Readout("Pivot", Math.round(draggedPivot.x) + ", " + Math.round(draggedPivot.y), false);
            }
            case MOVE -> {
                moveX = (int) Math.round(nowWorld.x - grab.x);
                moveY = (int) Math.round(nowWorld.y - grab.y);
                op = Handle.MOVE;
                return new Readout("Move", moveX + ", " + moveY, false);
            }
            case ROTATE -> {
                double mouse = PathTransformBox.rotationDegrees(pivotStart, grab, nowWorld);
                if (quarterTurns)
                    mouse = 90 * Math.round(mouse / 90);
                degrees = typed.applyAsDouble(mouse);
                op = Handle.ROTATE;
                return new Readout("Rotate", PathTool.formatValue((float) mouse) + "°", true);
            }
            case SCALE -> {
                double mouse = axis == PathTransformBox.SCALE_UNIFORM
                        ? PathTransformBox.scaleFactor(pivotStart, grab, nowWorld)
                        : PathTransformBox.axisScaleFactor(pivotStart, grab, nowWorld, boxAngle, axis);
                double factor = PathTransformBox.clampScale(typed.applyAsDouble(mouse));
                scaleX = axis == PathTransformBox.SCALE_Y ? 1 : factor;
                scaleY = axis == PathTransformBox.SCALE_X ? 1 : factor;
                op = Handle.SCALE;
                String label = switch (axis) {
                    case PathTransformBox.SCALE_X -> "Scale X";
                    case PathTransformBox.SCALE_Y -> "Scale Y";
                    default -> "Scale";
                };
                return new Readout(label, String.format(Locale.ROOT, "%.2f", mouse) + "×", true);
            }
        }
        return null;
    }

    /**
     * The transform so far in current view coordinates (rebuilt from its world
     * terms on every use), or {@code null} before anything was transformed.
     */
    public AffineTransform viewTransform(Function<Point2D, Point2D.Double> toView, double boxAngle) {
        if (op == null || pivotStart == null)
            return null;
        var pivotView = toView.apply(pivotStart);
        return switch (op) {
            case ROTATE -> PathTransformBox.rotation(pivotView, degrees);
            case SCALE -> PathTransformBox.axisScaling(pivotView, scaleX, scaleY, boxAngle);
            case MOVE -> {
                var from = toView.apply(new Point2D.Double(0, 0));
                var to = toView.apply(new Point2D.Double(moveX, moveY));
                yield AffineTransform.getTranslateInstance(to.x - from.x, to.y - from.y);
            }
            default -> null;
        };
    }

    /**
     * The transform so far in world coordinates (the map view only scales and
     * shifts, so angles are the same), or {@code null}.
     */
    public AffineTransform worldTransform(double boxAngle) {
        return viewTransform(point -> new Point2D.Double(point.getX(), point.getY()), boxAngle);
    }

    /**
     * The drag ended. {@code applied}: the tool applied the transform to
     * {@code selection}. Returns {@code true} when a click on the pivot (the second
     * of a double click) put it back on the centre.
     */
    public boolean finish(Object selection, boolean dragged, boolean applied) {
        try {
            if (kind == Handle.PIVOT) {
                if (!dragged)
                    return pivotClicked();
                if (draggedPivot != null) {
                    pivot = draggedPivot;
                    pivotOwner = selection;
                }
                return false;
            }
            if (applied) {
                // a placed pivot travels with a moved selection
                if (kind == Handle.MOVE && customPivot(selection))
                    pivot = new Point2D.Double(pivot.x + moveX, pivot.y + moveY);
                // the box keeps its orientation (rotating turns it further)
                turnBox(selection, op == Handle.ROTATE ? degrees : 0);
            }
            return false;
        } finally {
            cancel();
        }
    }

    /** Ends the drag without changing anything. */
    public void cancel() {
        kind = null;
        op = null;
        axis = 0;
        quarterTurns = false;
        pivotStart = null;
        grab = null;
        degrees = 0;
        scaleX = scaleY = 1;
        moveX = moveY = 0;
        draggedPivot = null;
    }

    /**
     * A click on the pivot; the second of a double click puts it back on the
     * centre.
     */
    private boolean pivotClicked() {
        long now = System.currentTimeMillis();
        Object interval = Toolkit.getDefaultToolkit().getDesktopProperty("awt.multiClickInterval");
        long limit = interval instanceof Integer value ? value : 500;
        if (now - lastPivotClickAt <= limit) {
            lastPivotClickAt = 0;
            pivot = null;
            return true;
        }
        lastPivotClickAt = now;
        return false;
    }

    // ---- hit testing and painting ----

    /**
     * The handle at {@code point} (view): the nearest within
     * {@code bestDistanceSq}; a handle exactly over a selected item wins the tie. A
     * {@code single} item only gets the move handle below it unless
     * {@code singleRotates} (a house turns, a path node does not).
     */
    public Hit hitTest(Point2D point, ShownBox shown, boolean single, boolean singleRotates, Point2D pivotView,
            double bestDistanceSq) {
        if (shown == null)
            return null;
        var box = shown.box();
        UnaryOperator<Point2D> at = p -> shown.frame().transform(p, null);
        Hit hit = null;
        double best = bestDistanceSq;
        if (single && !singleRotates) {
            if (at.apply(PathTransformBox.singleMoveHandle(box)).distanceSq(point) <= best)
                hit = new Hit(Handle.MOVE, 0);
            return hit;
        }
        var candidates = new java.util.ArrayList<Hit>();
        var points = new java.util.ArrayList<Point2D>();
        if (!single) {
            for (int scaleAxis : new int[]{PathTransformBox.SCALE_UNIFORM, PathTransformBox.SCALE_X,
                    PathTransformBox.SCALE_Y}) {
                candidates.add(new Hit(Handle.SCALE, scaleAxis));
                points.add(at.apply(PathTransformBox.scaleHandle(box, scaleAxis)));
            }
        }
        candidates.add(new Hit(Handle.ROTATE, 0));
        points.add(at.apply(PathTransformBox.rotateHandle(box)));
        candidates.add(new Hit(Handle.MOVE, 0));
        points.add(at.apply(PathTransformBox.moveHandle(box)));
        if (!single) {
            candidates.add(new Hit(Handle.PIVOT, 0));
            points.add(pivotView);
        }
        for (int k = 0; k < candidates.size(); k++) {
            double distance = points.get(k).distance(point);
            distance *= distance;
            if (distance <= best + 2e-9) {
                best = distance;
                hit = candidates.get(k);
            }
        }
        return hit;
    }

    /**
     * Draws the box and its handles (Lucide icons turned with the box; scale arrows
     * point along their axis). {@code hovered}: the handle under the mouse, or
     * {@code null}.
     */
    public static void paint(Graphics2D g, ShownBox shown, boolean single, boolean singleRotates, Point2D pivotView,
            Hit hovered) {
        if (shown == null)
            return;
        var box = shown.box();
        var frame = shown.frame();
        UnaryOperator<Point2D> at = p -> frame.transform(p, null);
        if (single && !singleRotates) {
            // a single node only moves: no box, rotate, scale or pivot
            PathTool.paintIconHandle(g, "move", at.apply(PathTransformBox.singleMoveHandle(box)), 0,
                    is(hovered, Handle.MOVE, 0));
            return;
        }
        g.setStroke(new BasicStroke(1.5f)); // solid, not dashed
        g.setColor(OverlayStyle.TEXT);
        g.draw(frame.createTransformedShape(box));
        g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(at.apply(PathTransformBox.topCentre(box)),
                at.apply(PathTransformBox.rotateHandle(box))));
        g.draw(new Line2D.Double(at.apply(PathTransformBox.leftCentre(box)),
                at.apply(PathTransformBox.moveHandle(box))));
        double turned = -Math.toDegrees(Math.atan2(frame.getShearY(), frame.getScaleX()));
        PathTool.paintIconHandle(g, "rotate-ccw", at.apply(PathTransformBox.rotateHandle(box)), turned,
                is(hovered, Handle.ROTATE, 0));
        PathTool.paintIconHandle(g, "move", at.apply(PathTransformBox.moveHandle(box)), turned,
                is(hovered, Handle.MOVE, 0));
        if (single)
            return;
        PathTool.paintIconHandle(g, "move-diagonal-2",
                at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_UNIFORM)), turned,
                is(hovered, Handle.SCALE, PathTransformBox.SCALE_UNIFORM));
        PathTool.paintIconHandle(g, "move-horizontal",
                at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_X)), turned,
                is(hovered, Handle.SCALE, PathTransformBox.SCALE_X));
        PathTool.paintIconHandle(g, "move-vertical",
                at.apply(PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_Y)), turned,
                is(hovered, Handle.SCALE, PathTransformBox.SCALE_Y));
        PathTool.paintIconHandle(g, "crosshair", pivotView, 0, is(hovered, Handle.PIVOT, 0));
    }

    private static boolean is(Hit hovered, Handle handle, int axis) {
        return hovered != null && hovered.handle() == handle && hovered.axis() == axis;
    }
}
