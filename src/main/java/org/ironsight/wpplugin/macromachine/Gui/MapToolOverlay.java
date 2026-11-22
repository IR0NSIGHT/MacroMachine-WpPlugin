package org.ironsight.wpplugin.macromachine.Gui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import org.pepsoft.worldpainter.WorldPainterView;

public class MapToolOverlay extends JComponent
{
    private static final Color OVERLAY_ICON_COLOR = Color.LIGHT_GRAY;

    private static final class MouseTransparentLabel extends JLabel
    {
        @Override
        public boolean contains(int x, int y) {
            return false;
        }
    }

    private static final boolean DRAW_CHECKERBOARD = false;
    private static final int CELL_SIZE = 10;
    private static final int LABEL_MARGIN = 8;
    private static final double LABEL_WIDTH_RATIO = 0.15;
    private static final float LABEL_TEXT_SCALE = 0.9f;
    private static final float LABEL_BASE_FONT_SIZE = 14f;
    private static final float LABEL_INITIAL_SCALE = 2f;
    private static final int LABEL_ANIMATION_START_DELAY_MS = 300;
    private static final int LABEL_ANIMATION_DURATION_MS = 500;
    private static final int LABEL_ANIMATION_TICK_MS = 16;
    private static final int LABEL_ICON_GAP = 4;
    private static final int MESSAGE_FADE_DURATION_MS = 500;
    private static final int MESSAGE_FADE_TICK_MS = 30;
    private static final float INFO_LABEL_VERTICAL_POSITION = 0.9f;
    private static final int INFO_LABEL_HORIZONTAL_MARGIN = 16;
    private static final int INFO_LABEL_PADDING = 8;
    private static final Color LABEL_BACKGROUND = new Color(0, 0, 0, 26);
    private static final Color INFO_LABEL_BACKGROUND = new Color(96, 96, 96, 180);
    private static final Color LIGHT_CELL = new Color(255, 255, 255, 80);
    private static final Color DARK_CELL = new Color(255, 0, 0, 80);
    private static final Color BORDER = new Color(255, 255, 255, 180);

    private WorldPainterView mapView;
    private final JLabel overlayLabel = new MouseTransparentLabel();
    private final JLabel infoLabel = new MouseTransparentLabel();
    private final Font overlayLabelBaseFont = overlayLabel.getFont().deriveFont(Font.BOLD, LABEL_BASE_FONT_SIZE);
    private final Font infoLabelBaseFont = infoLabel.getFont().deriveFont(Font.BOLD, LABEL_BASE_FONT_SIZE);
    private Point dragStartWorld;
    private Point dragEndWorld;
    private final Map<Long, Rectangle> outlines = new HashMap<>();
    private long nextOutlineId;
    private Timer labelPositionTimer;
    private float labelPositionProgress = 1f;
    private long labelAnimationStartNanos;
    private boolean labelAnimationStarted;
    private Timer transientInfoTimer;
    private final BufferedImage overlayIcon;
    private Color overlayIconColor;
    private Image tintedOverlayIcon;

    public MapToolOverlay(BufferedImage overlayIcon) {
        this.overlayIcon = overlayIcon;
        setOpaque(false);
        setLayout(null);
        overlayLabel.setOpaque(true);
        overlayLabel.setBackground(LABEL_BACKGROUND);
        overlayLabel.setBorder(null);
        overlayLabel.setFocusable(false);
        overlayLabel.setForeground(OVERLAY_ICON_COLOR);
        overlayLabel.setFont(overlayLabelBaseFont);
        overlayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        overlayLabel.setHorizontalTextPosition(SwingConstants.RIGHT);
        overlayLabel.setIconTextGap(LABEL_ICON_GAP);
        overlayLabel.setVisible(false);
        add(overlayLabel);

        infoLabel.setOpaque(true);
        infoLabel.setBackground(INFO_LABEL_BACKGROUND);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(INFO_LABEL_PADDING, INFO_LABEL_PADDING, INFO_LABEL_PADDING,
                INFO_LABEL_PADDING));
        infoLabel.setFocusable(false);
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setFont(infoLabelBaseFont);
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        infoLabel.setVisible(false);
        add(infoLabel);
    }

    @Override
    public boolean contains(int x, int y) {
        return false;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        layoutOverlayText();
        layoutInfoText();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        layoutOverlayText();
        layoutInfoText();
    }

    public void startDrag(Point viewPoint) {
        dragStartWorld = mapView != null ? mapView.viewToWorld(viewPoint) : viewPoint;
        dragEndWorld = dragStartWorld;
        repaint();
    }

    public void updateDrag(Point viewPoint) {
        if (mapView != null)
            dragEndWorld = mapView.viewToWorld(viewPoint);
        repaint();
    }

    public void endDrag() {
        dragStartWorld = null;
        dragEndWorld = null;
        repaint();
    }

    public boolean isDragging() {
        return dragStartWorld != null;
    }

    public void setMapView(WorldPainterView mapView) {
        this.mapView = mapView;
        repaint();
    }

    public void setOverlayText(String text) {
        stopLabelPositionTimer();
        stopTransientInfoTimer();
        labelPositionProgress = 1f;
        overlayLabel.setText(text);
        overlayLabel.setForeground(OVERLAY_ICON_COLOR);
        overlayLabel.setBackground(LABEL_BACKGROUND);
        overlayLabel.setVisible(text != null && !text.isBlank());
        layoutOverlayText();
        revalidate();
        repaint();
    }

    public void showTransientInfoText(String text, int durationMs) {
        stopTransientInfoTimer();
        infoLabel.setText(text);
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setBackground(INFO_LABEL_BACKGROUND);
        infoLabel.setVisible(text != null && !text.isBlank());
        layoutInfoText();
        revalidate();
        repaint();
        if (!infoLabel.isVisible())
            return;

        long fadeStartNanos = System.nanoTime() + Math.max(0, durationMs - MESSAGE_FADE_DURATION_MS) * 1_000_000L;
        transientInfoTimer = new Timer(MESSAGE_FADE_TICK_MS, event -> {
            if (transientInfoTimer != event.getSource())
                return;
            long now = System.nanoTime();
            long endNanos = fadeStartNanos + MESSAGE_FADE_DURATION_MS * 1_000_000L;
            if (now >= endNanos) {
                transientInfoTimer = null;
                ((Timer) event.getSource()).stop();
                clearInfoText();
                return;
            }
            if (now >= fadeStartNanos) {
                float alpha = (float) (endNanos - now) / (MESSAGE_FADE_DURATION_MS * 1_000_000L);
                int textAlpha = Math.max(0, Math.min(255, Math.round(Color.WHITE.getAlpha() * alpha)));
                int backgroundAlpha = Math.max(0, Math.min(255, Math.round(INFO_LABEL_BACKGROUND.getAlpha() * alpha)));
                infoLabel.setForeground(
                        new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(), textAlpha));
                infoLabel.setBackground(new Color(INFO_LABEL_BACKGROUND.getRed(), INFO_LABEL_BACKGROUND.getGreen(),
                        INFO_LABEL_BACKGROUND.getBlue(), backgroundAlpha));
                repaint();
            }
        });
        transientInfoTimer.start();
    }

    public void showOverlayText(String text) {
        stopLabelPositionTimer();
        stopTransientInfoTimer();
        overlayLabel.setText(text);
        overlayLabel.setForeground(Color.WHITE);
        overlayLabel.setBackground(LABEL_BACKGROUND);
        overlayLabel.setVisible(text != null && !text.isBlank());
        labelPositionProgress = 0f;
        layoutOverlayText();
        revalidate();
        repaint();
        if (!overlayLabel.isVisible()) {
            labelPositionProgress = 1f;
            return;
        }

        labelAnimationStarted = false;
        Timer timer = new Timer(LABEL_ANIMATION_TICK_MS, event -> {
            if (labelPositionTimer != event.getSource())
                return;
            if (!labelAnimationStarted) {
                labelAnimationStarted = true;
                labelAnimationStartNanos = System.nanoTime();
            }
            double elapsedMillis = (System.nanoTime() - labelAnimationStartNanos) / 1_000_000.0;
            labelPositionProgress = (float) Math.min(1.0, elapsedMillis / LABEL_ANIMATION_DURATION_MS);
            layoutOverlayText();
            repaint();
            if (labelPositionProgress >= 1f) {
                ((Timer) event.getSource()).stop();
                labelPositionTimer = null;
            }
        });
        timer.setInitialDelay(LABEL_ANIMATION_START_DELAY_MS);
        labelPositionTimer = timer;
        timer.start();
    }

    public void clearOverlayText() {
        setOverlayText(null);
    }

    public void clearInfoText() {
        stopTransientInfoTimer();
        infoLabel.setText(null);
        infoLabel.setForeground(Color.WHITE);
        infoLabel.setBackground(INFO_LABEL_BACKGROUND);
        infoLabel.setVisible(false);
        repaint();
    }

    private void stopLabelPositionTimer() {
        if (labelPositionTimer != null) {
            labelPositionTimer.stop();
            labelPositionTimer = null;
        }
    }

    private void stopTransientInfoTimer() {
        if (transientInfoTimer != null) {
            transientInfoTimer.stop();
            transientInfoTimer = null;
        }
    }

    /**
     * Space kept free at the bottom (e.g. for a profile strip); info messages stay
     * above it.
     */
    private int bottomReserve;

    public void setBottomReserve(int pixels) {
        if (bottomReserve == pixels)
            return;
        bottomReserve = pixels;
        layoutInfoText();
    }

    private void layoutInfoText() {
        if (!infoLabel.isVisible() || getWidth() <= 0 || getHeight() <= 0)
            return;
        Insets insets = infoLabel.getInsets();
        FontMetrics metrics = infoLabel.getFontMetrics(infoLabelBaseFont);
        int labelWidth = metrics.stringWidth(infoLabel.getText()) + insets.left + insets.right;
        int maxWidth = Math.max(1, getWidth() - 2 * INFO_LABEL_HORIZONTAL_MARGIN);
        labelWidth = Math.min(maxWidth, Math.max(1, labelWidth));
        int labelHeight = metrics.getHeight() + insets.top + insets.bottom;
        int x = Math.max(0, (getWidth() - labelWidth) / 2);
        int y = Math.round(getHeight() * INFO_LABEL_VERTICAL_POSITION - labelHeight / 2f);
        y = Math.max(0, Math.min(Math.max(0, getHeight() - labelHeight - bottomReserve), y));
        infoLabel.setBounds(x, y, labelWidth, labelHeight);
    }

    void layoutOverlayText() {
        if (!overlayLabel.isVisible() || getWidth() <= 0)
            return;
        float easedProgress = labelPositionProgress * labelPositionProgress * (3f - 2f * labelPositionProgress);
        Color overlayColor = interpolateColor(Color.WHITE, OVERLAY_ICON_COLOR, easedProgress);
        overlayLabel.setForeground(overlayColor);
        float labelScale = LABEL_INITIAL_SCALE - (LABEL_INITIAL_SCALE - 1f) * easedProgress;
        int baseLabelWidth = Math.max(1, (int) Math.round(getWidth() * LABEL_WIDTH_RATIO));
        int labelWidth = Math.max(1, Math.round(baseLabelWidth * labelScale));
        FontMetrics baseMetrics = overlayLabel.getFontMetrics(overlayLabelBaseFont);
        int baseTextWidth = Math.max(1, baseMetrics.stringWidth(overlayLabel.getText()));
        int baseIconWidth = overlayIcon == null ? 0 : baseMetrics.getHeight() + LABEL_ICON_GAP;
        float fontScale = (float) labelWidth / (baseTextWidth + baseIconWidth) * LABEL_TEXT_SCALE;
        Font scaledFont = overlayLabelBaseFont.deriveFont(overlayLabelBaseFont.getSize2D() * fontScale);
        overlayLabel.setFont(scaledFont);
        FontMetrics scaledMetrics = overlayLabel.getFontMetrics(scaledFont);
        int labelHeight = scaledMetrics.getHeight();
        if (overlayIcon != null) {
            Image scaledIcon = getTintedOverlayIcon(overlayColor).getScaledInstance(labelHeight, labelHeight,
                    Image.SCALE_SMOOTH);
            overlayLabel.setIcon(new ImageIcon(scaledIcon));
        }
        int centeredX = Math.max(0, (getWidth() - labelWidth) / 2);
        int edgeX = Math.max(0, getWidth() - labelWidth - LABEL_MARGIN);
        int centeredY = Math.max(0, Math.round((getHeight() * 0.3333f) - labelHeight / 2f));
        int edgeY = LABEL_MARGIN;
        int labelX = Math.round(centeredX + (edgeX - centeredX) * easedProgress);
        int labelY = Math.round(centeredY + (edgeY - centeredY) * easedProgress);
        overlayLabel.setBounds(labelX, labelY, labelWidth, labelHeight);
    }

    private Image getTintedOverlayIcon(Color color) {
        if (!color.equals(overlayIconColor)) {
            BufferedImage tintedIcon = new BufferedImage(overlayIcon.getWidth(), overlayIcon.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            int rgb = color.getRGB() & 0x00ffffff;
            for (int y = 0; y < overlayIcon.getHeight(); y++) {
                for (int x = 0; x < overlayIcon.getWidth(); x++) {
                    int alpha = overlayIcon.getRGB(x, y) & 0xff000000;
                    tintedIcon.setRGB(x, y, alpha | rgb);
                }
            }
            overlayIconColor = color;
            tintedOverlayIcon = tintedIcon;
        }
        return tintedOverlayIcon;
    }

    private static Color interpolateColor(Color from, Color to, float progress) {
        int red = Math.round(from.getRed() + (to.getRed() - from.getRed()) * progress);
        int green = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * progress);
        int blue = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * progress);
        return new Color(red, green, blue);
    }

    public Rectangle getDragBounds() {
        if (dragStartWorld == null || dragEndWorld == null)
            return null;
        return new Rectangle(Math.min(dragStartWorld.x, dragEndWorld.x), Math.min(dragStartWorld.y, dragEndWorld.y),
                Math.abs(dragEndWorld.x - dragStartWorld.x) + 1, Math.abs(dragEndWorld.y - dragStartWorld.y) + 1);
    }

    public boolean isBoxSelection() {
        Rectangle bounds = getDragBounds();
        return bounds != null && bounds.width >= 2 && bounds.height >= 2;
    }

    public long addOutline(Rectangle worldRectangle) {
        long outlineId = nextOutlineId++;
        outlines.put(outlineId, new Rectangle(worldRectangle));
        repaint();
        return outlineId;
    }

    public void removeOutline(long outlineId) {
        if (outlines.remove(outlineId) != null)
            repaint();
    }

    public void clearOutlines() {
        if (outlines.isEmpty())
            return;
        outlines.clear();
        repaint();
    }

    @Override
    public void paint(Graphics graphics) {
        // Set a breakpoint here to verify that Swing paints this child overlay.
        super.paint(graphics);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            if (DRAW_CHECKERBOARD && mapView != null) {
                Rectangle worldBounds = mapView.viewToWorld(0, 0, getWidth(), getHeight());
                int startX = Math.floorDiv(worldBounds.x, CELL_SIZE) * CELL_SIZE;
                int startY = Math.floorDiv(worldBounds.y, CELL_SIZE) * CELL_SIZE;
                int endX = worldBounds.x + worldBounds.width + CELL_SIZE;
                int endY = worldBounds.y + worldBounds.height + CELL_SIZE;

                for (int worldY = startY; worldY < endY; worldY += CELL_SIZE) {
                    for (int worldX = startX; worldX < endX; worldX += CELL_SIZE) {
                        Point topLeft = mapView.worldToView(worldX, worldY);
                        Point bottomRight = mapView.worldToView(worldX + CELL_SIZE, worldY + CELL_SIZE);
                        g.setColor((Math.floorDiv(worldX, CELL_SIZE) + Math.floorDiv(worldY, CELL_SIZE)) % 2 == 0
                                ? LIGHT_CELL
                                : DARK_CELL);
                        g.fillRect(topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
                    }
                }
            }

            if (mapView != null) {
                g.setColor(Color.RED);
                for (Rectangle worldOutline : outlines.values()) {
                    Rectangle outline = mapView.worldToView(worldOutline);
                    g.drawRect(outline.x, outline.y, outline.width - 1, outline.height - 1);
                }
            }

            if (mapView != null && isBoxSelection()) {
                Rectangle rectangle = mapView.worldToView(getDragBounds());
                g.setColor(BORDER);
                g.drawRect(rectangle.x, rectangle.y, rectangle.width - 1, rectangle.height - 1);
            }
        } finally {
            g.dispose();
        }
    }
}
