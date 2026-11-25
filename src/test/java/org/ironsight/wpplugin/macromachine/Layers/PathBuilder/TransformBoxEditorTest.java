package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.geom.Point2D;

import org.junit.jupiter.api.Test;

class TransformBoxEditorTest
{
    private static final Object SELECTION = new Object();
    private static final Point2D.Double CENTRE = new Point2D.Double(50, 50);

    private static TransformBoxEditor.Hit hit(TransformBoxEditor.Handle handle) {
        return new TransformBoxEditor.Hit(handle, 0);
    }

    /** Drags the pivot from where it is by (dx, dy) and lets go. */
    private static void dragPivot(TransformBoxEditor editor, double dx, double dy) {
        var pivot = editor.pivot(SELECTION, () -> CENTRE);
        editor.start(hit(TransformBoxEditor.Handle.PIVOT), pivot, pivot, false);
        editor.update(new Point2D.Double(pivot.x + dx, pivot.y + dy), 0, value -> value);
        editor.finish(SELECTION, true, true);
    }

    @Test
    void aPlacedPivotStaysThroughRotatingAndScaling() {
        var editor = new TransformBoxEditor();
        dragPivot(editor, 10, -5);
        assertEquals(new Point2D.Double(60, 45), editor.pivot(SELECTION, () -> CENTRE));

        var pivot = editor.pivot(SELECTION, () -> CENTRE);
        editor.start(hit(TransformBoxEditor.Handle.ROTATE), pivot, new Point2D.Double(pivot.x + 10, pivot.y), false);
        editor.update(new Point2D.Double(pivot.x, pivot.y - 10), 0, value -> value); // 90 degrees
        assertEquals(90, editor.shownAngle(SELECTION));
        editor.finish(SELECTION, true, true);

        assertTrue(editor.customPivot(SELECTION), "rotating keeps the placed pivot");
        assertEquals(new Point2D.Double(60, 45), editor.pivot(SELECTION, () -> CENTRE));
        assertEquals(90, editor.boxAngle(SELECTION), "the box keeps the turn");
        assertEquals(0, editor.boxAngle(new Object()), "a new selection starts upright");
        assertFalse(editor.customPivot(new Object()));
    }

    @Test
    void aPlacedPivotTravelsWithAMoveWhileDragging() {
        var editor = new TransformBoxEditor();
        dragPivot(editor, 10, 0);
        var grab = new Point2D.Double(0, 0);
        editor.start(hit(TransformBoxEditor.Handle.MOVE), editor.pivot(SELECTION, () -> CENTRE), grab, false);
        editor.update(new Point2D.Double(7, 3), 0, value -> value);
        // shown along with the selection during the drag, not only after it
        assertEquals(new Point2D.Double(67, 53), editor.displayedPivot(SELECTION, () -> CENTRE));
        editor.finish(SELECTION, true, true);
        assertEquals(new Point2D.Double(67, 53), editor.pivot(SELECTION, () -> CENTRE));
    }

    @Test
    void aRefusedTransformLeavesBoxAndPivotAlone() {
        var editor = new TransformBoxEditor();
        dragPivot(editor, 10, 0);
        editor.start(hit(TransformBoxEditor.Handle.MOVE), editor.pivot(SELECTION, () -> CENTRE),
                new Point2D.Double(0, 0), false);
        editor.update(new Point2D.Double(7, 3), 0, value -> value);
        editor.finish(SELECTION, true, false);
        assertEquals(new Point2D.Double(60, 50), editor.pivot(SELECTION, () -> CENTRE));
    }

    @Test
    void aDoubleClickOnThePivotPutsItBackOnTheCentre() {
        var editor = new TransformBoxEditor();
        dragPivot(editor, 10, 0);
        var pivot = editor.pivot(SELECTION, () -> CENTRE);
        editor.start(hit(TransformBoxEditor.Handle.PIVOT), pivot, pivot, false);
        assertFalse(editor.finish(SELECTION, false, false), "one click keeps it");
        editor.start(hit(TransformBoxEditor.Handle.PIVOT), pivot, pivot, false);
        assertTrue(editor.finish(SELECTION, false, false), "the second click resets it");
        assertEquals(CENTRE, editor.pivot(SELECTION, () -> CENTRE));
    }

    @Test
    void quarterTurnsSnapTheRotation() {
        var editor = new TransformBoxEditor();
        editor.start(hit(TransformBoxEditor.Handle.ROTATE), CENTRE, new Point2D.Double(60, 50), true);
        // 60 degrees counter-clockwise on screen snaps to 90
        editor.update(
                new Point2D.Double(50 + 10 * Math.cos(Math.toRadians(60)), 50 - 10 * Math.sin(Math.toRadians(60))), 0,
                value -> value);
        assertEquals(90, editor.degrees());
    }
}
