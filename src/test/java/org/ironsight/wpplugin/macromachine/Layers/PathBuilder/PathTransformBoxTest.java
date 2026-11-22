package org.ironsight.wpplugin.macromachine.Layers.PathBuilder;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PathTransformBoxTest
{
    private static final double P = PathTransformBox.PADDING;

    @Test
    void boxSurroundsOnlySelectedNodesWithPadding() {
        var nodes = List.of(new Point2D.Double(10, 10), new Point2D.Double(50, 30), new Point2D.Double(500, 500));
        var box = PathTransformBox.box(nodes, Set.of(0, 1));
        assertEquals(10 - P, box.getMinX(), 1e-9);
        assertEquals(10 - P, box.getMinY(), 1e-9);
        assertEquals(50 + P, box.getMaxX(), 1e-9);
        assertEquals(30 + P, box.getMaxY(), 1e-9);
        assertNull(PathTransformBox.box(nodes, Set.of()));
        // a single node still gets a box to grab
        assertEquals(2 * P, PathTransformBox.box(nodes, Set.of(2)).getWidth(), 1e-9);
    }

    @Test
    void handlesSitOnCornersAndStems() {
        var box = PathTransformBox.box(List.of(new Point2D.Double(0, 0), new Point2D.Double(40, 20)), Set.of(0, 1));
        assertEquals(new Point2D.Double(-P, -P), PathTransformBox.corner(box, 0));
        assertEquals(new Point2D.Double(40 + P, 20 + P), PathTransformBox.corner(box, 2));
        assertEquals(new Point2D.Double(20, -P - PathTransformBox.STEM), PathTransformBox.rotateHandle(box));
        assertEquals(new Point2D.Double(-P - PathTransformBox.STEM, 10), PathTransformBox.moveHandle(box));
        assertEquals(new Point2D.Double(40 + P, 20 + P),
                PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_UNIFORM));
        assertEquals(new Point2D.Double(40 + P, 10), PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_X));
        assertEquals(new Point2D.Double(20, 20 + P), PathTransformBox.scaleHandle(box, PathTransformBox.SCALE_Y));
        assertEquals(new Point2D.Double(20, 10), PathTransformBox.centre(box));
    }

    @Test
    void turnedBoxFitsTheNodesInItsOwnFrame() {
        // a diagonal line of nodes in a box turned 45 degrees: thin box along the line
        var nodes = List.of(new Point2D.Double(0, 0), new Point2D.Double(30, -30));
        var box = PathTransformBox.box(nodes, Set.of(0, 1), 45);
        assertEquals(Math.hypot(30, 30) + 2 * P, box.getWidth(), 1e-9);
        assertEquals(2 * P, box.getHeight(), 1e-9);
        // its centre maps back onto the middle of the line
        var centre = PathTransformBox.frame(45).transform(PathTransformBox.centre(box), null);
        assertEquals(15, centre.getX(), 1e-9);
        assertEquals(-15, centre.getY(), 1e-9);
    }

    @Test
    void axisScalingStretchesAlongTheTurnedBox() {
        var pivot = new Point2D.Double(0, 0);
        // axis aligned box: x stretches only x
        assertEquals(2, PathTransformBox.axisScaleFactor(pivot, new Point2D.Double(10, 5), new Point2D.Double(20, 40),
                0, PathTransformBox.SCALE_X));
        assertEquals(new Point2D.Double(20, 5),
                PathTransformBox.axisScaling(pivot, 2, 1, 0).transform(new Point2D.Double(10, 5), null));
        // box turned a quarter counter-clockwise on screen: its x axis points up the
        // screen
        assertEquals(3, PathTransformBox.axisScaleFactor(pivot, new Point2D.Double(4, -10), new Point2D.Double(0, -30),
                90, PathTransformBox.SCALE_X));
        var stretched = PathTransformBox.axisScaling(pivot, 3, 1, 90).transform(new Point2D.Double(4, -10), null);
        assertEquals(4, stretched.getX(), 1e-9, "across the box axis nothing changes");
        assertEquals(-30, stretched.getY(), 1e-9);
        // grabbed level with the pivot along that axis: no scaling
        assertEquals(1, PathTransformBox.axisScaleFactor(pivot, new Point2D.Double(0, 10), new Point2D.Double(5, 20), 0,
                PathTransformBox.SCALE_X));
    }

    @Test
    void rotationIsCounterClockwiseOnScreenAndWrapped() {
        var pivot = new Point2D.Double(0, 0);
        // y points down: from above the pivot to its right is a clockwise quarter turn
        assertEquals(-90,
                PathTransformBox.rotationDegrees(pivot, new Point2D.Double(0, -10), new Point2D.Double(10, 0)));
        assertEquals(90,
                PathTransformBox.rotationDegrees(pivot, new Point2D.Double(10, 0), new Point2D.Double(0, -10)));
        assertEquals(180, Math
                .abs(PathTransformBox.rotationDegrees(pivot, new Point2D.Double(10, 0), new Point2D.Double(-10, 0))));
        // applying the transform moves the start point onto the mouse
        var moved = PathTransformBox.rotation(pivot, -90).transform(new Point2D.Double(0, -10), null);
        assertEquals(10, moved.getX(), 1e-9);
        assertEquals(0, moved.getY(), 1e-9);
    }

    @Test
    void scaleFollowsDistanceFromPivot() {
        var pivot = new Point2D.Double(10, 10);
        assertEquals(2, PathTransformBox.scaleFactor(pivot, new Point2D.Double(20, 10), new Point2D.Double(30, 10)));
        assertEquals(0.5, PathTransformBox.scaleFactor(pivot, new Point2D.Double(20, 10), new Point2D.Double(10, 15)));
        assertEquals(PathTransformBox.MIN_SCALE,
                PathTransformBox.scaleFactor(pivot, new Point2D.Double(20, 10), new Point2D.Double(10, 10)));
        assertEquals(1, PathTransformBox.scaleFactor(pivot, pivot, new Point2D.Double(30, 10)), "grabbed on the pivot");
        var scaled = PathTransformBox.scaling(pivot, 2).transform(new Point2D.Double(20, 15), null);
        assertEquals(new Point2D.Double(30, 20), scaled);
    }
}
