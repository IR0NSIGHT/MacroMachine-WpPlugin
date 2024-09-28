package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.pathing.FloatInterpolateLinearList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Arrays;

public class PathHistogram extends JPanel implements KeyListener {
    private final FloatInterpolateLinearList curve;
    private final boolean onlyValueChange;
    private final float[] terrainCurve;
    private int[] selectable;
    private FloatInterpolateLinearList handles;
    private int selectedCurveIdx;

    PathHistogram(FloatInterpolateLinearList curve, int selectedIdx, boolean onlyValueChange, float[] terrainCurve) {
        super(new BorderLayout());
        this.selectedCurveIdx = curve.handleIdcs()[0];
        this.curve = curve;
        this.terrainCurve = terrainCurve;
        this.onlyValueChange = onlyValueChange;
        setFocusable(true); // Make sure the component can receive focus for key events
        requestFocusInWindow(); // Request focus to ensure key bindings work
        setupKeyBindings();
    }

    private int getSelectedCurveIdx() {
        return selectedCurveIdx;
    }

    private void changeValue(float amount) {
        float value = curve.getInterpolatedValue(getSelectedCurveIdx()) + amount;
        curve.setValue(getSelectedCurveIdx(), value);
    }

    private void setupKeyBindings() {
        this.addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        assert !curve.isInterpolate(getSelectedCurveIdx()) : "this index can not be selected because its " +
                "interpolated";

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(50, getHeight() - 50);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), -getHeight());
        g2d.setColor(Color.BLACK);

        float scale = getWidth() * 1f / (curve.getCurveLength() + 100);
        g2d.scale(scale, scale);
        g2d.setColor(Color.BLACK);

        //draw curve
        float minSoFar = Math.min(terrainCurve[0], curve.getInterpolatedValue(0));
        for (int i = 1; i < curve.getCurveLength(); i++) {
            //handles line
            g2d.setColor(Color.GRAY);
            float aZ = curve.getInterpolatedValue(i - 1).floatValue();
            float bZ = curve.getInterpolatedValue(i).floatValue();

            g2d.drawLine(
                    i - 1, -Math.round(aZ),
                    i, -Math.round(bZ));

            g2d.setColor(Color.GREEN);
            float terrainA = terrainCurve[i - 1];
            float terrainB = terrainCurve[i];
            g2d.drawLine(i - 1, -Math.round(terrainA), i, -Math.round(terrainB));

            //min line
            g2d.setColor(Color.BLACK);
            g2d.drawLine(i - 1, -Math.round(Math.min(terrainA, minSoFar)), i,
                    -Math.round(Math.min(terrainB, minSoFar)));

            minSoFar = Math.min(Math.min(minSoFar, bZ), terrainB);
        }
        g2d.setColor(Color.BLACK);
        g2d.drawRect(0, 0, curve.getCurveLength(), -getHeight());

        //mark height lines
        for (int y = 0; y < 255; y += 20) {
            g2d.drawLine(0, -y, 30, -y);
            g2d.drawString(String.valueOf(y), 35, -y);
        }

        float[] dashPattern = {10, 5};
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

        //mark handles
        int handleIdx = 0;
        for (int x = 0; x < curve.getCurveLength(); x++) {
            if (curve.isInterpolate(x))
                continue;

            int curveHeight = curve.getInterpolatedValue(x).intValue();
            int y = curveHeight / 2;
            g2d.setColor(x == getSelectedCurveIdx() ? Color.ORANGE : Color.BLACK);
            g2d.drawLine(x, 0, x, -y);
            g2d.drawLine(x, -(y + g.getFontMetrics().getHeight()), x, -2 * y);


            String text = "";
            text += String.format("%.2f", curve.getInterpolatedValue(x));
            g2d.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, -y);

            handleIdx++;
        }
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

    @Override
    public void keyTyped(KeyEvent e) {

    }

    private int safeIdx(int idx) {
        return Math.max(0, Math.min(this.curve.getCurveLength() - 1, idx));
    }

    private void calcSelectables() {
        ArrayList selectables = new ArrayList<Integer>(curve.getCurveLength());
        for (int i = 0; i < curve.getCurveLength(); i++) {
            if (!curve.isInterpolate(i)) {
                selectables.add(i);
            }
        }
        this.selectable = new int[selectables.size()];
        int i = 0;
        for (Object idx : selectables.subList(0, selectables.size())) {
            this.selectable[i++] = (int) idx;
        }
        assert this.selectable.length != 0;
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
            case KeyEvent.VK_SPACE: //Insert new handle
                if (onlyValueChange)
                    break;
                int newCurveIdx = getSelectedCurveIdx() + 100;
                if (selectedCurveIdx < selectable.length - 1) {
                    newCurveIdx = (selectable[selectedCurveIdx] + selectable[selectedCurveIdx + 1]) / 2;
                }
                newCurveIdx = safeIdx(newCurveIdx);
                if (curve.isInterpolate(newCurveIdx)) {
                    curve.setValue(newCurveIdx, curve.getInterpolatedValue(selectable[0]));
                    selectedCurveIdx = newCurveIdx;
                }
                break;
            case KeyEvent.VK_DELETE:
                if (onlyValueChange)
                    break;
                if (selectable.length < 2)
                    break;
                if (!curve.isInterpolate(getSelectedCurveIdx())) {
                    curve.setToInterpolate(getSelectedCurveIdx());
                    selectedCurveIdx = selectableIdxNear(getSelectedCurveIdx(), -1);
                }
                break;
            case KeyEvent.VK_RIGHT: {

                if (e.isShiftDown() || e.isControlDown()) {
                    if (onlyValueChange)
                        break;
                    //move selectable to the right
                    int curveIdx = getSelectedCurveIdx();
                    int off = e.isControlDown() ? 100 : 1;
                    int targetIdx = safeIdx(curveIdx + off);
                    if (curve.isInterpolate(targetIdx)) {
                        curve.setValue(targetIdx, curve.getInterpolatedValue(curveIdx));
                        curve.setToInterpolate(curveIdx);
                        selectedCurveIdx = targetIdx;
                    }
                } else {
                    selectedCurveIdx = selectableIdxNear(selectedCurveIdx, 1);
                }
            }
            break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown() || e.isControlDown()) {
                    if (onlyValueChange)
                        break;
                    //move selectable to the right
                    int curveIdx = getSelectedCurveIdx();
                    int off = e.isControlDown() ? -100 : -1;
                    int targetIdx = safeIdx(curveIdx + off);
                    if (curve.isInterpolate(targetIdx)) {
                        curve.setValue(targetIdx, curve.getInterpolatedValue(curveIdx));
                        curve.setToInterpolate(curveIdx);
                        selectedCurveIdx = targetIdx;
                    }
                } else {
                    selectedCurveIdx = selectableIdxNear(selectedCurveIdx, -1);
                }
                break;
            default:
        }
        calcSelectables();
        assert !curve.isInterpolate(getSelectedCurveIdx());
        repaint();
    }

    private int selectableIdxNear(int startIdx, int dir) {
        int[] selectables = curve.handleIdcs();
        int find = Arrays.binarySearch(selectables, startIdx);
        if (find < 0)
            return selectables[0];
        else {
            find = Math.min(Math.max(find + dir, 0), selectables.length - 1);
            return selectables[find];
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    enum KeyDir {LEFT, RIGHT, UP, DOWN}
}
