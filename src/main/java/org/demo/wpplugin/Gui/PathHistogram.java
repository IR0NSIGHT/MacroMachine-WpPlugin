package org.demo.wpplugin.Gui;

import org.demo.wpplugin.geometry.HeightDimension;
import org.demo.wpplugin.operations.ContinuousCurve;
import org.demo.wpplugin.operations.River.RiverHandleInformation;
import org.demo.wpplugin.pathing.Path;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;

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
        g2d.translate(50, getHeight() - 50);


        int extraWidth = 200;
        int graphicsWidth = Math.max(255, (curveHeights.length)) + extraWidth;
        int graphicsHeight = 300;

        {   //draw background
            g2d.setColor(Color.GREEN);
            g2d.fillRect(0, 0, getWidth(), -getHeight());
        }

        AffineTransform originalTransform = g2d.getTransform();
        //scale to window
        float scale = 1; //Math.min(getHeight(), getWidth()) * 1f / graphicsHeight;
        g2d.translate(userFocus.x,userFocus.y);
        float totalScale = scale*userZoom;
        g2d.scale(totalScale, totalScale);
        Font font = new Font("Arial", Font.PLAIN, (int) (20/(scale*totalScale)));
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(1f/totalScale));

        //mark water line
        g2d.setColor(Color.BLUE);
        g2d.drawLine(0, -62, curveHeights.length, -62);


        g2d.setColor(Color.BLACK);
        g2d.drawRect(0,-0, curveHeights.length, -255);

        {        //draw terrain curve
            g2d.setColor(Color.GREEN);
            for (int i = 0; i < terrainCurve.length; i++) {
                //draw terrain height
                float terrainB = terrainCurve[i];
                g2d.drawRect(i,-(int)terrainB,1,1);
            }
            g2d.drawString("terrain profile", terrainCurve.length, terrainCurve[terrainCurve.length - 1]);
        }
        {        //draw interpoalted curve
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < curveHeights.length; i++) {
                g2d.setColor(Color.DARK_GRAY);
                float terrainB = terrainCurve[i];
                g2d.fillRect(i,-(int)terrainB,1, (int)terrainB);

                g2d.setColor(Color.BLUE);
                float aZ = curveHeights[i];
                g2d.fillRect(i -1,-(int)aZ,1,1);


            }
            g2d.drawString("river profile", curveHeights.length, -curveHeights[curveHeights.length - 1]);
        }

        g2d.setColor(Color.BLACK);


        float[] dashPattern = {10f/totalScale, 5f/totalScale};
        g2d.setStroke(new BasicStroke(3f/totalScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

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

        g2d.setStroke(new BasicStroke(1f/totalScale));
        int widthOfString = g2d.getFontMetrics().stringWidth(String.valueOf(1000));
        {    //mark height lines
            g2d.setColor(Color.BLACK);
            int windowX = (int) (-userFocus.x/totalScale);
            for (int y = 0; y <= 255; y += 25) {
                g2d.drawString(String.valueOf(((y))), windowX, -(y));
                g2d.drawLine(windowX+ widthOfString, -(y), windowX + 2* widthOfString, -(y));
            }
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
                if (e.isShiftDown()) userFocus.y += 10;
                else {
                    int change = e.isControlDown() ? 25 : 1;
                    changeValue(change);
                }
            }
            break;
            case KeyEvent.VK_DOWN: {
                if (e.isShiftDown()) userFocus.y -= 10;
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
                if (e.isShiftDown()) userFocus.x += 40;
                else
                    selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), 1);
                break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown()) userFocus.x -= 40;
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
