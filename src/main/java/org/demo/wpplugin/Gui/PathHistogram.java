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
    private final Point userFocus = new Point(0, 0);
    private float userZoom = 1f;
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

        Color grassGreen = new Color(69,110,51);
        Color skyBlue = new Color(192,255,255);
        Color waterBlue = new Color(49,72,244);

        ContinuousCurve curve = ContinuousCurve.fromPath(path, dimension);
        float[] curveHeights = Path.interpolateWaterZ(curve, dimension);

        {   //draw background
            g2d.setColor(skyBlue);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }

        //shift down so image -y maps to terrain y
        g2d.translate(0, getHeight());

        int graphicsHeight = 300;

        //scale to window
        float scale = Math.min(getHeight(), getWidth()) * 1f / graphicsHeight;
        float totalScale = userZoom * scale;

        float[] dashPattern = {10f / totalScale, 5f / totalScale};
        Stroke dottedHandle = new BasicStroke(3f / totalScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0);
        Stroke dottedGrid = new BasicStroke(1f / totalScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{3f / totalScale, 2f / totalScale}, 0);


        g2d.translate(totalScale*(-userFocus.x + 50), totalScale*(userFocus.y - 50 ));
        g2d.scale(totalScale, totalScale);
        Font font = new Font("Arial", Font.PLAIN, (int) (20 / (totalScale)));
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(1f / totalScale));

        g2d.setColor(Color.BLACK);

        {        //draw interpoalted curve
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < curveHeights.length; i++) {
                if (i < userFocus.x)
                    continue;
                g2d.setColor(grassGreen);  //green
                int terrainB = Math.round(terrainCurve[i]);
                g2d.fillRect(i, -(int) terrainB, 1, terrainB - userFocus.y);

                g2d.setColor(waterBlue); //blue
                int aZ = Math.round(curveHeights[i]);
                g2d.fillRect(i - 1, -aZ, 1, aZ  - userFocus.y);
            }

            g2d.drawString("terrain profile", terrainCurve.length, terrainCurve[terrainCurve.length - 1]);
            g2d.drawString("river profile", curveHeights.length, -curveHeights[curveHeights.length - 1]);
        }

        {
            g2d.setStroke(dottedGrid);
            //horizontal lines
            g2d.setColor(Color.BLACK);
            int y;
            for (y = 0; y <= userFocus.y + 300; y += 50) {
                if (y < userFocus.y)
                    continue;
                g2d.drawLine(userFocus.x, -y, curveHeights.length, -y);
                g2d.drawString(String.valueOf(y), userFocus.x - g2d.getFontMetrics().stringWidth(String.valueOf(y)), -y);
            }
            //vertical lines
            for (int x = 0; x <= curveHeights.length; x += 50) {
                if (x < userFocus.x)
                    continue;
                g2d.drawLine(x, -userFocus.y, x, -y);
                g2d.drawString(String.valueOf(x), x, -userFocus.y + g2d.getFontMetrics().getHeight());
            }
            g2d.setColor(Color.RED);
            g2d.drawString("focus" + userFocus.toString() + " zoom " + String.valueOf(totalScale), userFocus.x, 2 * g2d.getFontMetrics().getHeight());
            g2d.drawLine(userFocus.x, 0, userFocus.x, -255);
        }

        g2d.setStroke(dottedHandle);
        //mark water line
        g2d.setColor(Color.BLUE);
        g2d.drawLine(userFocus.x, -62, curveHeights.length, -62);
        g2d.drawString("ocean level",userFocus.x - g2d.getFontMetrics().stringWidth("ocean level"),-62);

        g2d.setStroke(dottedHandle);

        int[] handleToCurve = path.handleToCurveIdx(true);
        //mark handles
        for (int handleIdx = 1; handleIdx < path.amountHandles() - 1; handleIdx++) {
            int curveX = handleToCurve[handleIdx];
            if (curveX < userFocus.x)
                continue;

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

            String text = String.format("%.0f", curveHeights[handleToCurve[handleIdx]]);
            if (notSet)
                text = "("+text+")";
            g2d.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, -y);
        }
        if (1==1)return;;
        // draw overlay
        g2d.setStroke(new BasicStroke(1f / totalScale));
        int widthOfString = g2d.getFontMetrics().stringWidth(String.valueOf(1000));
        {    //mark height lines
            g2d.setColor(Color.BLACK);
            int windowX = (int) (userFocus.x / totalScale);
            for (int y = 0; y <= 255; y += 25) {
                g2d.drawString(String.valueOf(((y))), windowX, -(y));
                g2d.drawLine(windowX + widthOfString, -(y), windowX + 2 * widthOfString, -(y));
            }
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

            case KeyEvent.VK_PLUS:
                userZoom = Math.min(userZoom * 1.5f, 10f);
                break;

            case KeyEvent.VK_MINUS:
                userZoom = Math.max(userZoom / 1.5f, 0.1f);
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
                else selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), 1);
                break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown()) userFocus.x -= 40;
                else selectedHandleIdx = selectableIdxNear(getSelectedHandleIdx(), -1);
                break;
            default:
        }

        int[] handleToCurve = path.handleToCurveIdx(true);
        int curveLength = handleToCurve[handleToCurve.length-2];
        userFocus.x = Math.max(0, Math.min(userFocus.x, curveLength - 50));
        userFocus.y = Math.max(0,Math.min(255,userFocus.y));
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
