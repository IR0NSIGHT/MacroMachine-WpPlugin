package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A slider with two handles, a low and a high value between a minimum and a
 * maximum (whole numbers), in the overlay look: a quiet track, the chosen range
 * and the handles in lavender, each value above its handle. Dragging grabs the
 * handle nearest to the mouse; the handles cannot cross.
 */
public class RangeSlider extends JComponent
{
    private static final int PADDING = 8, HANDLE = 12, TRACK_Y = 22;

    private int minimum, maximum, low, high;
    private final List<ChangeListener> listeners = new ArrayList<>();
    /** The handle being dragged: -1 low, 1 high, 0 none. */
    private int dragged;
    private int hovered;

    public RangeSlider(int minimum, int maximum) {
        setOpaque(false);
        setForeground(OverlayStyle.TEXT);
        setFont(OverlayStyle.font(Font.PLAIN, 11.5f));
        setRange(minimum, maximum);
        low = minimum;
        high = maximum;
        var mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                dragged = nearestHandle(event.getX());
                moveHandle(event.getX());
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                moveHandle(event.getX());
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                dragged = 0;
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent event) {
                int handle = nearestHandle(event.getX());
                if (handle != hovered) {
                    hovered = handle;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = 0;
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    public int getMinimum() {
        return minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    public int getLow() {
        return low;
    }

    public int getHigh() {
        return high;
    }

    /** The values the handles can take; the handles are kept inside. */
    public void setRange(int minimum, int maximum) {
        this.minimum = minimum;
        this.maximum = Math.max(minimum, maximum);
        low = clamp(low);
        high = Math.max(low, clamp(high));
        repaint();
    }

    /**
     * Sets both handles (ordered, inside the range); notifies when they changed.
     */
    public void setValues(int low, int high) {
        int newLow = clamp(Math.min(low, high)), newHigh = clamp(Math.max(low, high));
        if (newLow == this.low && newHigh == this.high)
            return;
        this.low = newLow;
        this.high = newHigh;
        repaint();
        fire();
    }

    public void addChangeListener(ChangeListener listener) {
        listeners.add(listener);
    }

    private void fire() {
        var event = new ChangeEvent(this);
        for (ChangeListener listener : List.copyOf(listeners))
            listener.stateChanged(event);
    }

    private int clamp(int value) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private double xOf(int value) {
        double width = getWidth() - 2 * PADDING;
        return PADDING + (maximum == minimum ? 0 : width * (value - minimum) / (maximum - minimum));
    }

    private int valueAt(int x) {
        double width = Math.max(1, getWidth() - 2 * PADDING);
        return clamp((int) Math.round(minimum + (x - PADDING) / width * (maximum - minimum)));
    }

    /**
     * -1 for the low handle, 1 for the high one; on top of each other, the side of
     * the mouse decides.
     */
    private int nearestHandle(int x) {
        double toLow = Math.abs(x - xOf(low)), toHigh = Math.abs(x - xOf(high));
        if (toLow == toHigh)
            return x < xOf(low) ? -1 : 1;
        return toLow < toHigh ? -1 : 1;
    }

    private void moveHandle(int x) {
        int value = valueAt(x);
        if (dragged < 0)
            setValues(Math.min(value, high), high);
        else if (dragged > 0)
            setValues(low, Math.max(value, low));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(150, TRACK_Y + HANDLE / 2 + 4);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            double lowX = xOf(low), highX = xOf(high);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(OverlayStyle.BORDER);
            g.draw(new Line2D.Double(PADDING, TRACK_Y, getWidth() - PADDING, TRACK_Y));
            Color accent = isEnabled() ? OverlayStyle.ACCENT : OverlayStyle.TEXT_DISABLED;
            g.setColor(accent);
            g.draw(new Line2D.Double(lowX, TRACK_Y, highX, TRACK_Y));
            for (int side : new int[]{-1, 1}) {
                double x = side < 0 ? lowX : highX;
                boolean active = dragged == side || (dragged == 0 && hovered == side);
                double size = active ? HANDLE + 2 : HANDLE;
                g.setColor(accent);
                g.fill(new Ellipse2D.Double(x - size / 2, TRACK_Y - size / 2, size, size));
                if (active) {
                    g.setColor(OverlayStyle.TEXT);
                    g.setStroke(new BasicStroke(1.5f));
                    g.draw(new Ellipse2D.Double(x - size / 2, TRACK_Y - size / 2, size, size));
                }
            }
            // the values above their handles, pushed apart when the handles are close
            g.setFont(getFont());
            var metrics = g.getFontMetrics();
            String lowText = Integer.toString(low), highText = Integer.toString(high);
            double lowLeft = lowX - metrics.stringWidth(lowText) / 2.0;
            double highLeft = highX - metrics.stringWidth(highText) / 2.0;
            double gap = 4;
            if (lowLeft + metrics.stringWidth(lowText) + gap > highLeft) {
                double middle = (lowX + highX) / 2;
                lowLeft = middle - gap / 2 - metrics.stringWidth(lowText);
                highLeft = middle + gap / 2;
            }
            lowLeft = Math.max(0, lowLeft);
            highLeft = Math.min(getWidth() - metrics.stringWidth(highText), highLeft);
            g.setColor(getForeground());
            float textY = TRACK_Y - HANDLE / 2f - 4;
            g.drawString(lowText, (float) lowLeft, textY);
            if (high != low)
                g.drawString(highText, (float) highLeft, textY);
        } finally {
            g.dispose();
        }
    }
}
