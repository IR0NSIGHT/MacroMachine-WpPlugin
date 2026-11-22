package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MapToolOverlayTest
{
    @Test
    void introductionSettlesAndClearsImmediately() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapToolOverlay overlay = new MapToolOverlay(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
            overlay.setBounds(0, 0, 1000, 600);
            JLabel label = (JLabel) overlay.getComponent(0);

            overlay.showOverlayText("City Tool");
            assertEquals(new Rectangle(350, label.getY(), 300, label.getHeight()), label.getBounds());
            assertEquals(Color.WHITE, label.getForeground());
            assertTrue(label.isVisible());
            assertNotNull(label.getIcon());
            assertFalse(overlay.contains(1, 1));
            assertFalse(label.contains(1, 1));
            Timer introduction = timer(overlay, "labelPositionTimer");
            assertEquals(300, introduction.getInitialDelay());
            assertTrue(introduction.isRunning());

            overlay.setOverlayText("City Tool");
            assertFalse(introduction.isRunning());
            assertEquals(842, label.getX());
            assertEquals(8, label.getY());
            assertEquals(150, label.getWidth());
            assertEquals(OverlayStyle.TEXT_MUTED, label.getForeground());

            overlay.showOverlayText("City Tool");
            Timer restarted = timer(overlay, "labelPositionTimer");
            overlay.clearOverlayText();
            assertFalse(restarted.isRunning());
            assertNull(timer(overlay, "labelPositionTimer"));
            assertFalse(label.isVisible());
        });
    }

    @Test
    void animationCallbacksSettleAndIgnoreStoppedTimers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapToolOverlay overlay = new MapToolOverlay(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB));
            overlay.setBounds(0, 0, 1000, 600);
            JLabel label = (JLabel) overlay.getComponent(0);
            overlay.showOverlayText("Path Tool");
            Timer timer = timer(overlay, "labelPositionTimer");
            var callback = timer.getActionListeners()[0];
            callback.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "tick"));
            try {
                Field start = MapToolOverlay.class.getDeclaredField("labelAnimationStartNanos");
                start.setAccessible(true);
                start.setLong(overlay, System.nanoTime() - 1_000_000_000L);
            } catch (ReflectiveOperationException exception) {
                throw new AssertionError(exception);
            }
            callback.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "tick"));
            assertEquals(842, label.getX());
            assertEquals(8, label.getY());
            assertEquals(OverlayStyle.TEXT_MUTED, label.getForeground());
            assertFalse(timer.isRunning());
            overlay.clearOverlayText();
            callback.actionPerformed(new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "tick"));
            assertFalse(label.isVisible());
        });
    }

    @Test
    void transientInfoReplacementAndClearStopTimers() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapToolOverlay overlay = new MapToolOverlay(null);
            overlay.setBounds(0, 0, 1000, 600);
            JLabel label = (JLabel) overlay.getComponent(1);
            overlay.showTransientInfoText("First", 3000);
            Timer first = timer(overlay, "transientInfoTimer");
            assertTrue(first.isRunning());
            assertTrue(label.isVisible());
            assertFalse(label.contains(1, 1));

            overlay.showTransientInfoText("Second", 3000);
            assertFalse(first.isRunning());
            Timer second = timer(overlay, "transientInfoTimer");
            assertTrue(second.isRunning());
            assertEquals("Second", label.getText());
            overlay.clearInfoText();
            assertFalse(second.isRunning());
            assertNull(timer(overlay, "transientInfoTimer"));
            assertFalse(label.isVisible());
        });
    }

    @Test
    void dragWithoutViewKeepsOriginalSinglePointUntilEnded() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            MapToolOverlay overlay = new MapToolOverlay(null);
            overlay.startDrag(new Point(8, 4));
            overlay.updateDrag(new Point(12, 10));
            assertTrue(overlay.isDragging());
            assertEquals(new Rectangle(8, 4, 1, 1), overlay.getDragBounds());
            assertFalse(overlay.isBoxSelection());
            overlay.endDrag();
            assertFalse(overlay.isDragging());
            assertNull(overlay.getDragBounds());
        });
    }

    private static Timer timer(MapToolOverlay overlay, String name) {
        try {
            Field field = MapToolOverlay.class.getDeclaredField(name);
            field.setAccessible(true);
            return (Timer) field.get(overlay);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
