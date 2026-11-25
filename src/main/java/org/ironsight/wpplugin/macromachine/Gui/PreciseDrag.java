package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * Precise drag, the standard across the plugin's map tools (Path Tool, City
 * Tool), like Blender: while Shift is held, a drag moves its virtual cursor at
 * {@link #SCALE} of the mouse's speed. The virtual cursor follows the mouse's
 * movement, not its position, so pressing or releasing Shift mid-drag never
 * makes the dragged item jump.
 */
public final class PreciseDrag
{
    public static final double SCALE = 0.1;

    private Point2D.Double cursor;
    private Point last;
    private boolean precise;

    /** A drag starts at {@code point} (view coordinates). */
    public void start(Point point) {
        cursor = new Point2D.Double(point.x, point.y);
        last = new Point(point);
        precise = false;
    }

    /**
     * The mouse moved to {@code point}; returns the virtual cursor (view
     * coordinates).
     */
    public Point2D.Double update(Point point, boolean precise) {
        double scale = precise ? SCALE : 1;
        cursor.x += (point.x - last.x) * scale;
        cursor.y += (point.y - last.y) * scale;
        last = new Point(point);
        this.precise = precise;
        return cursor();
    }

    public Point2D.Double cursor() {
        return new Point2D.Double(cursor.x, cursor.y);
    }

    /** Whether the last update was precise (to announce switching into it). */
    public boolean precise() {
        return precise;
    }
}
