package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class HeightProvider implements IPositionValueGetter, IPositionValueSetter {
    private HeightProvider instance;

    public HeightProvider() {
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return Math.round(dim.getHeightAt(x, y));
    }

    @Override
    public String getName() {
        return "Height";
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public String getDescription() {
        return "get the height of a position in percent for 0 to 255.";
    }

    private HeightProvider getInstance() {
        if (instance == null) instance = new HeightProvider();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setHeightAt(x, y, value);
    }


    @Override
    public int getMinValue() {
        return -64;
    }


    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return getInstance();
    }


    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }


    @Override
    public int getMaxValue() {
        return 364; //TODO is the correct?
    }

    @Override
    public String valueToString(int value) {
        return value + "H";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        float percent = (value - getMinValue() * 1f) / (getMaxValue() - getMinValue());

        g.setColor(new Color(131, 154, 255));
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(new Color(0, 142, 7));
        g.fillRect(0, (int) (dim.height * (1 - percent)), dim.width, (int) (dim.height * (percent)));
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.HEIGHT;
    }


    @Override
    public void prepareForDimension(Dimension dim) {

    }
}
