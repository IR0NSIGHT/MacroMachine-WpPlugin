package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class AlwaysIO implements IPositionValueGetter {
    public static AlwaysIO instance = new AlwaysIO();

    @Override
    public boolean isVirtual() {
        return true;
    }
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return 0;
    }

    @Override
    public String getName() {
        return "Always";
    }

    @Override
    public String getDescription() {
        return "Always apply the output with one value";
    }

    @Override
    public int getMaxValue() {
        return 0;
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return instance;
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public String valueToString(int value) {
        return "Always";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {

    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.ALWAYS;
    }
}
