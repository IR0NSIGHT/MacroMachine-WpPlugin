package org.demo.wpplugin.Gui;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.pathing.Path;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;

public class PathHistogram extends JPanel implements KeyListener {
    private final float[] terrainCurve;
    private final HeightDimension dimension;
    private Path path;
    private int selectedHandleIdx;

    public PathHistogram(Path path, int selectedIdx, float[] terrainCurve, HeightDimension dimension) {
        super(new BorderLayout());
        this.selectedHandleIdx = selectedIdx;
        overwritePath(path);
        this.terrainCurve = terrainCurve;
        this.dimension = dimension;
        setFocusable(true); // Make sure the component can receive focus for key events
        requestFocusInWindow(); // Request focus to ensure key bindings work
        setupKeyBindings();
    }

    private void overwritePath(Path path) {
        this.path = path;
    }

    private void setupKeyBindings() {
        this.addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        ContinuousCurve curve = ContinuousCurve.fromPath(path, dimension);

        float[] curveHeights = Path.interpolateWaterZ(curve, dimension);
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(100, getHeight() - 50);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), -getHeight());
        g2d.setColor(Color.BLACK);

        int extraWidth = 500;
        float scale = getWidth() * 1f / (curveHeights.length + extraWidth);
        g2d.scale(scale, scale);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, -300, curveHeights.length, 300);

        //draw curve
        for (int i = 1; i < curveHeights.length; i++) {
            //handles line
            float aZ = curveHeights[i - 1];
            float bZ = curveHeights[i];

            //draw terrain height
            g2d.setColor(Color.GREEN);
            float terrainA = terrainCurve[i - 1];
            float terrainB = terrainCurve[i];
            g2d.drawLine(i - 1, -Math.round(terrainA), i, -Math.round(terrainB));

            //draw curve as interpoalted by Path
            g2d.setColor(Color.BLACK);
            g2d.drawLine(i - 1, -Math.round(aZ), i, -Math.round(bZ));
        }
        g2d.setColor(Color.BLACK);

        //mark height lines
        for (int y = 0; y <= 255; y += 25) {
            g2d.drawString(String.valueOf(y), -50, -y);

            g2d.drawLine(-50 + g2d.getFontMetrics().stringWidth(String.valueOf(y)), -y, -10, -y);
        }

        float[] dashPattern = {10, 5};
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

        int[] handleToCurve = path.handleToCurveIdx(true);
        //mark handles
        for (int handleIdx = 1; handleIdx < path.amountHandles() - 1; handleIdx++) {
            float curveHeightF = getValue(path.handleByIndex(handleIdx), RiverInformation.WATER_Z);
            boolean notSet = curveHeightF == INHERIT_VALUE;
            int curveHeight = Math.round(curveHeightF);
            int y = curveHeight / 2;
            Color c = Color.BLACK;
            if (notSet) {
                c = Color.LIGHT_GRAY;
                y = 30;
            }
            int x = handleToCurve[handleIdx];
            //vertical lines
            g2d.setColor(handleIdx == getSelectedHandleIdx() ? Color.ORANGE : c);
            g2d.drawLine(x, 0, x, -y);
            g2d.drawLine(x, -(y + g.getFontMetrics().getHeight()), x, -2 * y);

            String text = notSet ? "(INHERIT)" : String.format("%.2f", getValue(path.handleByIndex(handleIdx),
                    RiverInformation.WATER_Z));
            g2d.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, -y);
        }
    }

    private int getSelectedHandleIdx() {
        return selectedHandleIdx;
    }

    @Override
    public Dimension getPreferredSize() {
        // Get screen dimensions
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

        // Use a fraction of the screen size, e.g., 70% width and 40% height
        int width = (int) (screenSize.width * 0.7);
        int height = (int) (screenSize.height * 0.4);

        // Return the dynamically calculated size
        return new Dimension(width, height);
    }

    public Path getPath() {
        return path;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        switch (key) {
            case KeyEvent.VK_UP: {
                int change = e.isControlDown() ? 25 : 1;
                changeValue(change);
            }
            break;
            case KeyEvent.VK_DOWN: {
                int change = e.isControlDown() ? 25 : 1;
                changeValue(-change);
            }
            break;
            case KeyEvent.VK_DELETE: {
                float[] handle = path.handleByIndex(getSelectedHandleIdx());
                float[] newHandle = setValue(handle, RiverInformation.WATER_Z, INHERIT_VALUE);
                Path newP = path.setHandleByIdx(newHandle, getSelectedHandleIdx());
                overwritePath(newP);
                break;
            }
            case KeyEvent.VK_RIGHT:
                selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), 1);
                break;
            case KeyEvent.VK_LEFT:
                selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), -1);
                break;
            default:
        }
        repaint();
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    private void changeValue(float amount) {
        int handleIdx = getSelectedHandleIdx();
        if (handleIdx < 0 || handleIdx >= path.amountHandles()) {
            throw new IllegalArgumentException("can not set value because idx is not a handle");
        } else {
            float[] handle = path.handleByIndex(handleIdx);

            float targetValue = getValue(handle, RiverInformation.WATER_Z) + amount;
            targetValue = targetValue == INHERIT_VALUE ? INHERIT_VALUE : Math.min(RiverInformation.WATER_Z.max, Math.max(RiverInformation.WATER_Z.min, targetValue));

            float[] newHandle = setValue(handle, RiverInformation.WATER_Z, targetValue);
                    ;
            overwritePath(path.setHandleByIdx(newHandle, handleIdx));
        }
    }

    private int selectableIdxNear(int startIdx, int dir) {
        //dont allow selecting the first and last index, as those are control points and not on the curve
        return Math.max(1, Math.min(path.amountHandles() - 2, startIdx + dir));
    }
}
