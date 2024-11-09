package org.ironsight.wpplugin.rivertool.Gui;

import org.ironsight.wpplugin.rivertool.geometry.HeightDimension;
import org.ironsight.wpplugin.rivertool.operations.ContinuousCurve;
import org.ironsight.wpplugin.rivertool.operations.River.RiverHandleInformation;
import org.ironsight.wpplugin.rivertool.pathing.Path;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class PathHistogram extends JPanel implements KeyListener {
    private final float[] terrainCurve;
    private final HeightDimension dimension;
    private final Point userFocus = new Point(0, 0);
    /**
     * array that tells which handles are currently selected by the user
     */
    private final boolean[] handleSelection;
    int[] handleToCurve;
    private float userZoom = 1f;
    private Path path;
    /**
     * the handle where the users selection cursor is currently
     */
    private int cursorHandleIdx;
    private ContinuousCurve curve;
    private boolean recalcCurve = false;

    public PathHistogram(Path path, int selectedIdx, HeightDimension dimension) {
        super(new BorderLayout());
        this.cursorHandleIdx = selectedIdx;
        overwritePath(path);
        this.dimension = dimension;
        handleSelection = new boolean[path.amountHandles()];
        this.curve = ContinuousCurve.fromPath(path, dimension);
        this.terrainCurve = curve.terrainCurve(dimension);


        handleToCurve = ContinuousCurve.handleToCurve(path);
        setFocusable(true); // Make sure the component can receive focus for key events
        requestFocusInWindow(); // Request focus to ensure key bindings work
        setupKeyBindings();
        assert curve.curveLength() == 1 + handleToCurve[handleSelection.length - 1] : "last handle index is not at " +
                "end of curve";
    }

    private void overwritePath(Path path) {
        this.path = path;
        recalcCurve = true;
    }

    private void setupKeyBindings() {
        this.addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (recalcCurve) {
            recalcCurve = false;
            curve = ContinuousCurve.fromPath(path, dimension);
            handleToCurve = ContinuousCurve.handleToCurve(path);    //FIXME does this ever change?
        }

        Graphics2D g2d = (Graphics2D) g;

        Color grassGreen = new Color(69, 110, 51);
        Color skyBlue = new Color(192, 255, 255);
        Color waterBlue = new Color(49, 72, 244);

        float[] waterCurve = curve.getWaterCurve(dimension);
        assert waterCurve.length == curve.curveLength();
        assert terrainCurve.length == curve.curveLength();
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
        Stroke dottedHandle = new BasicStroke(3f / totalScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                dashPattern, 0);
        Stroke dottedGrid = new BasicStroke(1f / totalScale, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0,
                new float[]{3f / totalScale, 2f / totalScale}, 0);

        int borderWidth = 100;
        g2d.translate(totalScale * (-userFocus.x) + borderWidth, totalScale * (userFocus.y) - borderWidth);
        g2d.scale(totalScale, totalScale);
        Font font = new Font("Arial", Font.PLAIN, (int) (20f / (totalScale)));
        g2d.setFont(font);
        g2d.setStroke(new BasicStroke(1f / totalScale));

        g2d.setColor(Color.BLACK);

        {        //draw interpoalted curve
            g2d.setColor(Color.BLACK);
            for (int i = 0; i < waterCurve.length; i++) {
                if (i < userFocus.x) continue;
                g2d.setColor(grassGreen);  //green
                int terrainB = Math.round(terrainCurve[i]);
                g2d.fillRect(i, -terrainB, 1, terrainB - userFocus.y);
                //FIXME why are terrain curve and waterCurve 2 different lengths?
                g2d.setColor(waterBlue); //blue
                int aZ = Math.round(waterCurve[i]);
                g2d.fillRect(i - 1, -aZ, 1, aZ - userFocus.y);
            }
        }

        {
            g2d.setStroke(dottedGrid);
            //horizontal lines
            g2d.setColor(Color.BLACK);
            int y;
            for (y = 0; y <= userFocus.y + 300; y += 50) {
                if (y < userFocus.y) continue;
                g2d.drawLine(userFocus.x, -y, waterCurve.length, -y);
                g2d.drawString(String.valueOf(y), userFocus.x - g2d.getFontMetrics().stringWidth(String.valueOf(y)),
                        -y);
            }
            //vertical lines
            for (int x = 0; x <= waterCurve.length; x += 50) {
                if (x < userFocus.x) continue;
                g2d.drawLine(x, -userFocus.y, x, -y);
                g2d.drawString(String.valueOf(x), x, -userFocus.y + g2d.getFontMetrics().getHeight());
            }
            g2d.setColor(Color.RED);
            int fontHeight = g2d.getFontMetrics().getHeight();
            g2d.drawString(String.valueOf(y), userFocus.x - g2d.getFontMetrics().stringWidth(String.valueOf(y)), -y);

            g2d.drawString(String.format("ancor position %d,%d, zoom: %.2f", userFocus.x, userFocus.y, userZoom),
                    userFocus.x, -userFocus.y + 2 * fontHeight);
            g2d.drawString(String.format("length: %d, handles: %d", curve.curveLength(), path.amountHandles()),
                    userFocus.x, -userFocus.y + 3 * fontHeight);
            g2d.drawString(String.format("highest water: %.0f, lowest water: %.0f",
                    curve.getMax(RiverHandleInformation.RiverInformation.WATER_Z), curve.getMin(RiverHandleInformation.RiverInformation.WATER_Z)), userFocus.x,
                    -userFocus.y + 4 * fontHeight);
        }

        g2d.setStroke(dottedHandle);

        int oceanLevel = 62;
        if (userFocus.y < oceanLevel) {
            //mark water line
            g2d.setColor(Color.BLUE);
            g2d.drawLine(userFocus.x, -62, waterCurve.length, -62);
            g2d.drawString("ocean level", userFocus.x - g2d.getFontMetrics().stringWidth("ocean level"), -oceanLevel);
        }

        g2d.setStroke(dottedHandle);

        //DRAW HANDLES
        for (int handleIdx = 0; handleIdx < path.amountHandles(); handleIdx++) {
            int pointCurveIdx = handleToCurve[handleIdx];
            if (pointCurveIdx < userFocus.x) continue;

            float curveHeightHandle = RiverHandleInformation.getValue(path.handleByIndex(handleIdx), RiverHandleInformation.RiverInformation.WATER_Z);
            boolean notSet = curveHeightHandle == RiverHandleInformation.INHERIT_VALUE;
            int curveHeightReal = Math.round(waterCurve[pointCurveIdx]);
            Color c = Color.BLACK;
            if (notSet) {
                c = Color.LIGHT_GRAY;
            }
            if (handleIdx == getCursorHandleIdx()) c = Color.RED;
            if (handleSelection[handleIdx]) c = Color.ORANGE;
            //vertical lines
            g2d.setColor(c);
            g2d.setStroke(dottedHandle);

            if (handleIdx == getCursorHandleIdx()) {
                int width = 8;
                g2d.fillOval(pointCurveIdx - width / 2, -userFocus.y + 10 + width / 2, width, width);
            }

            String text = String.format("%.0f -> %d", curveHeightHandle, curveHeightReal);
            if (notSet) text = "(" + text + ")";
            //draw above terrain or watercurve
            int terrainHeight = Math.round(terrainCurve[pointCurveIdx]);
            float tHeight = Math.max(terrainHeight, curveHeightReal);
            if (userFocus.y < tHeight)
                g2d.drawString(text, pointCurveIdx - g.getFontMetrics().stringWidth(text) / 2, -tHeight - 10);

            if (curveHeightHandle > userFocus.y) {
                g2d.drawLine(pointCurveIdx, -userFocus.y, pointCurveIdx, -Math.round(curveHeightHandle)); //-(int)
                // tHeight
            }

            if (terrainHeight >= userFocus.y) {
                g2d.setStroke(dottedGrid);
                g2d.drawLine(pointCurveIdx, -userFocus.y, pointCurveIdx, -Math.round(terrainHeight));
            }

        }
    }

    private int getCursorHandleIdx() {
        return cursorHandleIdx;
    }

    private void setCursorHandleIdx(int idx) {
        cursorHandleIdx = idx;
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
            // ZOOM STUFF
            case KeyEvent.VK_PLUS:
                userZoom = Math.min(userZoom * 1.5f, 10f);
                break;

            case KeyEvent.VK_MINUS:
                userZoom = Math.max(userZoom / 1.5f, 0.1f);
                break;

            case KeyEvent.VK_ENTER:
                toggleHandleSelection(getCursorHandleIdx());
                break;

            // CHANGE VALUE OF SELECTION
            case KeyEvent.VK_UP: {
                if (e.isShiftDown()) userFocus.y += 10;
                else {
                    int change = e.isControlDown() ? 25 : 1;
                    changeHandleValue(change);
                }
            }
            break;
            case KeyEvent.VK_DOWN: {
                if (e.isShiftDown()) userFocus.y -= 10;
                else {
                    int change = e.isControlDown() ? 25 : 1;
                    changeHandleValue(-change);
                }
            }
            break;
            case KeyEvent.VK_T: {
                //set all selected handles to terrain height
                for (int i = 0; i < handleSelection.length; i++) {
                    if (!handleSelection[i]) continue;
                    float[] handle = path.handleByIndex(i);
                    int pointCurveIdx = handleToCurve[i];
                    float terrainHeight = terrainCurve[pointCurveIdx];
                    float[] newHandle = RiverHandleInformation.setValue(handle, RiverHandleInformation.RiverInformation.WATER_Z, terrainHeight);
                    Path newP = path.setHandleByIdx(newHandle, i);
                    overwritePath(newP);
                }
                break;
            }
            case KeyEvent.VK_DELETE: {
                // set all selected handles to INHERIT
                for (int i = 0; i < handleSelection.length; i++) {
                    if (!handleSelection[i]) continue;
                    float[] handle = path.handleByIndex(i);
                    float[] newHandle = RiverHandleInformation.setValue(handle, RiverHandleInformation.RiverInformation.WATER_Z, RiverHandleInformation.INHERIT_VALUE);
                    Path newP = path.setHandleByIdx(newHandle, i);
                    overwritePath(newP);
                }
                break;
            }

            // SELECTION CURSOR INPUT
            case KeyEvent.VK_RIGHT:
                if (e.isShiftDown()) userFocus.x += 40;
                else {
                    //expand selection
                    if (e.isControlDown()) {
                        expandSelectState(+1);
                    } else {
                        moveCursor(+1);
                    }
                }
                break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown()) userFocus.x -= 40;
                else {
                    //expand selection
                    if (e.isControlDown()) {
                        expandSelectState(-1);
                    } else {
                        moveCursor(-1);
                    }
                }
                break;

            case KeyEvent.VK_A:
                if (e.isControlDown()) {
                    boolean allSelected = true;
                    for (int i = 0; i < handleSelection.length; i++) {
                        allSelected = allSelected && isSelected(i);
                    }
                    if (allSelected) deselectAll();
                    else selectAll();
                }
                break;
            case KeyEvent.VK_I:
                if (e.isControlDown()) invertTotalSelection();
                break;
            default:
        }

        int maxTerrain = Math.round(curve.getMax(RiverHandleInformation.RiverInformation.WATER_Z));

        int curveLength = handleToCurve[handleToCurve.length - 2];
        userFocus.x = Math.max(-50, Math.min(userFocus.x, curveLength - 50));
        userFocus.y = Math.max(0, Math.min(maxTerrain, userFocus.y));
        repaint();
    }

    private void toggleHandleSelection(int handleIdx) {
        if (isSelected(handleIdx)) doUnSelect(handleIdx);
        else doSelect(handleIdx);
    }

    private void changeHandleValue(float amount) {
        for (int handleIdx = 1; handleIdx < handleSelection.length - 1; handleIdx++) {
            if (!handleSelection[handleIdx]) continue;
            float[] handle = path.handleByIndex(handleIdx);

            float targetValue =
                    RiverHandleInformation.sanitizeInput(RiverHandleInformation.getValue(handle, RiverHandleInformation.RiverInformation.WATER_Z) + amount,
                            RiverHandleInformation.RiverInformation.WATER_Z);

            float[] newHandle = RiverHandleInformation.setValue(handle, RiverHandleInformation.RiverInformation.WATER_Z, targetValue);
            overwritePath(path.setHandleByIdx(newHandle, handleIdx));
        }
    }

    private void expandSelectState(int dir) {
        //copy current select state to next cursor
        int nextCursor = selectableIdxNear(getCursorHandleIdx(), dir);
        if (isSelected(getCursorHandleIdx())) doSelect(nextCursor);
        else doUnSelect(nextCursor);
        setCursorHandleIdx(nextCursor);
    }

    private void moveCursor(int dir) {
        setCursorHandleIdx(selectableIdxNear(getCursorHandleIdx(), dir));
    }

    private boolean isSelected(int idx) {
        return handleSelection[idx];
    }

    private void deselectAll() {
        for (int i = 0; i < handleSelection.length; i++) {
            doUnSelect(i);
        }
    }

    private void selectAll() {
        for (int i = 0; i < handleSelection.length; i++) {
            doSelect(i);
        }
    }

    private void invertTotalSelection() {
        for (int i = 0; i < handleSelection.length; i++) {
            toggleHandleSelection(i);
        }
    }

    private void doUnSelect(int idx) {
        handleSelection[idx] = false;
    }

    private void doSelect(int idx) {
        handleSelection[idx] = true;
    }

    private int selectableIdxNear(int startIdx, int dir) {
        //dont allow selecting the first and last index, as those are control points and not on the curve
        return Math.max(1, Math.min(path.amountHandles() - 2, startIdx + dir));
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
