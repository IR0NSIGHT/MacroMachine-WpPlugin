package org.demo.wpplugin.operations.River;

import org.demo.wpplugin.pathing.FloatInterpolateList;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class PathHistogram extends JPanel implements KeyListener {
    private final FloatInterpolateList curve;
    private int[] selectable;
    private FloatInterpolateList handles;
    private int selectedIdx;

    PathHistogram(FloatInterpolateList curve, int selectedIdx) {
        super(new BorderLayout());
        this.selectedIdx = selectedIdx;
        this.curve = curve;

        calcSelectables();
        assert selectable.length != 0;
        this.selectedIdx = 0;
        setFocusable(true); // Make sure the component can receive focus for key events
        requestFocusInWindow(); // Request focus to ensure key bindings work
        setupKeyBindings();
    }

    private int getSelectedCurveIdx() {
        return selectable[selectedIdx];
    }

    private void changeValue(float amount) {
        float value = curve.getInterpolatedList()[getSelectedCurveIdx()] + amount;
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
        g2d.translate(50, 0);
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, getWidth(), getHeight());
        g2d.setColor(Color.BLACK);

        float scale = getWidth() * 1f / (curve.getSize() + 100);
        g2d.scale(scale, scale);
        g2d.setColor(Color.BLACK);

        float[] list = curve.getInterpolatedList();
        //draw curve
        for (int i = 1; i < curve.getSize(); i++) {
            float aZ = list[i - 1];
            float bZ = list[i];

            g2d.drawLine(i - 1, (int) (getHeight() / scale - aZ), i
                    , (int) (getHeight() / scale - bZ));
        }
        g2d.drawRect(0, 0, curve.getSize(), getHeight());

        //mark height lines
        for (int y = 0; y < 255; y += 20) {
            int yPos = (int) (getHeight() / scale - y);
            g2d.drawLine(0, yPos, 30, yPos);
            g2d.drawString(String.valueOf(y), 35, yPos);
        }

        float[] dashPattern = {10, 5};
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, dashPattern, 0));

        //mark handles
        int handleIdx = 0;
        for (int x = 0; x < curve.getSize(); x++) {
            if (curve.isInterpolate(x))
                continue;

            int y = 35 + g.getFontMetrics().getHeight() * (handleIdx % 5);
            g2d.setColor(x == getSelectedCurveIdx() ? Color.ORANGE : Color.BLACK);
            g2d.drawLine(x, 0, x, 30);

            String text = "";
            text += String.format("%.2f", curve.getInterpolatedList()[x]);
            g2d.drawString(text, x - g.getFontMetrics().stringWidth(text) / 2, y);

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
        return Math.max(0, Math.min(this.curve.getSize() - 1, idx));
    }

    private void calcSelectables() {
        ArrayList selectables = new ArrayList<Integer>(curve.getSize());
        for (int i = 0; i < curve.getSize(); i++) {
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
                int newCurveIdx = getSelectedCurveIdx() + 100;
                if (selectedIdx < selectable.length - 1) {
                    newCurveIdx = (selectable[selectedIdx] + selectable[selectedIdx + 1]) / 2;
                }
                newCurveIdx = safeIdx(newCurveIdx);
                if (curve.isInterpolate(newCurveIdx)) {
                    curve.setValue(newCurveIdx, curve.getInterpolatedList()[selectable[0]]);
                    selectHandleAt(newCurveIdx);
                }
                break;
            case KeyEvent.VK_DELETE: //Insert new handle
                if (selectable.length < 2)
                    break;
                if (!curve.isInterpolate(getSelectedCurveIdx())) {
                    curve.setToInterpolate(getSelectedCurveIdx());
                    selectedIdx--;
                }
                break;
            case KeyEvent.VK_RIGHT: {
                if (e.isShiftDown() || e.isControlDown()) {
                    //move selectable to the right
                    int curveIdx = getSelectedCurveIdx();
                    int off = e.isControlDown() ? 100 : 1;
                    int targetIdx = safeIdx(curveIdx + off);
                    if (curve.isInterpolate(targetIdx)) {
                        curve.setValue(targetIdx, curve.getInterpolatedList()[curveIdx]);
                        curve.setToInterpolate(curveIdx);
                        selectHandleAt(targetIdx);
                    }
                } else {
                    selectedIdx++;
                }
            }
            break;
            case KeyEvent.VK_LEFT:
                if (e.isShiftDown() || e.isControlDown()) {
                    //move selectable to the right
                    int curveIdx = getSelectedCurveIdx();
                    int off = e.isControlDown() ? -100 : -1;
                    int targetIdx = safeIdx(curveIdx + off);
                    if (curve.isInterpolate(targetIdx)) {
                        curve.setValue(targetIdx, curve.getInterpolatedList()[curveIdx]);
                        curve.setToInterpolate(curveIdx);
                        selectHandleAt(targetIdx);
                    }
                } else {
                    selectedIdx--;
                }
                break;
            default:
        }
        calcSelectables();
        selectedIdx = Math.max(0, Math.min(selectedIdx, selectable.length - 1));
        repaint();
    }

    private void selectHandleAt(int curveIdx) {
        calcSelectables();
        for (int i = 0; i < selectable.length; i++) {
            if (selectable[i] == curveIdx) {
                selectedIdx = i;
                assert getSelectedCurveIdx() == curveIdx;
                break;
            }
        }
        System.out.println("thats not supposed to happen?");
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }

    enum KeyDir {LEFT, RIGHT, UP, DOWN}
}
