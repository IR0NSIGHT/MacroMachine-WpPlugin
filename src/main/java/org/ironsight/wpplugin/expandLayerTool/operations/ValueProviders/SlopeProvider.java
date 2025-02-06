package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class SlopeProvider implements IPositionValueGetter {
    /**
     * slope in degrees 0-90
     *
     * @param dim
     * @param x
     * @param y
     * @return
     */
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return (int) Math.round(Math.toDegrees(Math.atan(dim.getSlope(x, y))));
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 90;
    }

    @Override
    public String valueToString(int value) {
        return value + "°";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.BLUE);
        g2d.fillRect(0, 0, dim.width, dim.height);
        g2d.translate(dim.width / 2f, dim.height / 2f);
        g2d.rotate(Math.toRadians(value));
        g2d.setColor(new Color(34, 153, 84));

        g2d.fillRect(-dim.width, 0, 2 * dim.width, (int) (dim.height * 1.41f));
    }

    @Override
    public String getName() {
        return "Slope";
    }

    @Override
    public String getDescription() {
        return "the slope of a position in degrees from 0 to 90°";
    }
}
