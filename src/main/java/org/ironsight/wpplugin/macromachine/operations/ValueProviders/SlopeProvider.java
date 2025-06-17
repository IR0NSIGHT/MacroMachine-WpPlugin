package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
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
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return false;
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
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new SlopeProvider();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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
    public ProviderType getProviderType() {
        return ProviderType.SLOPE;
    }
    @Override
    public String toString() {
        return getName();
    }
    @Override
    public String getName() {
        return "Slope";
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public String getDescription() {
        return "get the slope of a position in degrees from 0 to 90°";
    }
}
