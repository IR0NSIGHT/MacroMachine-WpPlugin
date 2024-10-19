package org.demo.wpplugin.Gui;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static org.demo.wpplugin.operations.River.RiverHandleInformation.*;

public class PathHistogram extends JPanel implements KeyListener {
    private final float[] terrainCurve;
    private final HeightDimension dimension;
    private int userZoom = 3;
    private final Point userFocus = new Point(0, 0);
    private Path path;
    private int selectedHandleIdx;
    private Graphics2D g2d;

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
        super.paintComponent(g);
        g2d = (Graphics2D) g;

        ContinuousCurve curve = ContinuousCurve.fromPath(path, dimension);
        float[] curveHeights = Path.interpolateWaterZ(curve, dimension);


        //shift to right
        g2d.translate(100, getHeight() - 50);


        int extraWidth = 200;
        int graphicsWidth = Math.max(255, (curveHeights.length)) + extraWidth;
        int graphicsHeight = 300;
        //scale to window
        float scale = Math.min(getHeight(), getWidth()) * 1f / graphicsHeight;
        g2d.scale(scale, scale);

        {   //draw background
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), -getHeight());
        }


        int xShiftBase = g2d.getFontMetrics().stringWidth(String.valueOf(1000));

        {    //mark height lines
            g2d.setColor(Color.BLACK);
            for (int y = 0; y <= 255; y += 25) {
                g2d.drawString(String.valueOf(y), 0, getGraphicsY(y));
                g2d.drawLine(xShiftBase, getGraphicsY(y), xShiftBase * 2, getGraphicsY(y));
            }
        }
        g2d.translate(xShiftBase * 2, 0);   //shift rest of image right so the height lines are left of it

        //mark water line
        g2d.setColor(Color.BLUE);
        g2d.drawLine(getGraphicsX(0), getGraphicsY(62), getGraphicsX(curveHeights.length), getGraphicsY(62));


        g2d.setColor(Color.BLACK);
        g2d.drawRect(getGraphicsX(0), getGraphicsY(0), getGraphicsX(curveHeights.length), getGraphicsY(255));

        {        //draw terrain curve
            g2d.setColor(Color.GREEN);
            for (int i = 0; i < terrainCurve.length; i++) {
                //draw terrain height
                float terrainB = terrainCurve[i];
                markHeightPoint(i, terrainB);
            }
            g2d.drawString("terrain profile", getGraphicsX(terrainCurve.length), getGraphicsY(terrainCurve[terrainCurve.length - 1]));
        }
        {        //draw interpoalted curve
            g2d.setColor(Color.BLACK);
            for (int i = 1; i < curveHeights.length; i++) {
                //handles line
                float aZ = curveHeights[i - 1];
                float bZ = curveHeights[i];
                g2d.drawLine(i - 1, -Math.round(aZ), i, -Math.round(bZ));
            }
            g2d.drawString("river profile", curveHeights.length, -curveHeights[curveHeights.length - 1]);
        }

        g2d.setColor(Color.BLACK);


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

            String text = String.format("%.2f", curveHeights[handleToCurve[handleIdx]]) + (notSet ? "\n(INHERIT)" : "");
            g2d.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, -y);
        }
    }

    private int getGraphicsX(int rawX) {
        return (userFocus.x + rawX) * userZoom;
    }

    private int getGraphicsY(float heightOnMap) {
        return -(int) ((userFocus.y + heightOnMap) * userZoom);
    }

    /**
     * takes a point on curve with x y coord and translate it + draws on the current graphics, includes user zoom
     *
     * @param orgX   curve index untranslated (raw)
     * @param height raw height on worldpainter map
     */
    private void markHeightPoint(int orgX, float height) {
        int x = getGraphicsX(orgX);
        int y = getGraphicsY(height);
        g2d.drawRect(x, y, userZoom, userZoom);
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

            case KeyEvent.VK_PLUS:
                userZoom = Math.min(userZoom + 1, 5);
                break;

            case KeyEvent.VK_MINUS:
                userZoom = Math.max(userZoom - 1, 1);
                break;

            case KeyEvent.VK_UP: {
                if (e.isShiftDown()) userFocus.y += 1;
                else {
                    int change = e.isControlDown() ? 25 : 1;
                    changeValue(change);
                }
            }
            break;
            case KeyEvent.VK_DOWN: {
                if (e.isShiftDown()) userFocus.y -= 1;
                else {
                    int change = e.isControlDown() ? 25 : 1;
                    changeValue(-change);
                }
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
                if (e.isShiftDown()) userFocus.x += 1;
                else
                    selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), 1);
                break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown()) userFocus.x -= 1;
                else
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

            float targetValue = RiverHandleInformation.sanitizeInput(getValue(handle, RiverInformation.WATER_Z) + amount, RiverInformation.WATER_Z);

            float[] newHandle = setValue(handle, RiverInformation.WATER_Z, targetValue);
            overwritePath(path.setHandleByIdx(newHandle, handleIdx));
        }
    }

    private int selectableIdxNear(int startIdx, int dir) {
        //dont allow selecting the first and last index, as those are control points and not on the curve
        return Math.max(1, Math.min(path.amountHandles() - 2, startIdx + dir));
    }
}
