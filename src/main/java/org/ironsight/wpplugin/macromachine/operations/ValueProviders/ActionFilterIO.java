package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;

public class ActionFilterIO implements IPositionValueSetter, IPositionValueGetter {
    public static ActionFilterIO instance = new ActionFilterIO();
    private static int value;
    private static int lastX;
    private static int lastY;
    public static final int PASS_VALUE = 1;
    public static final int BLOCK_VALUE = 0;

    private ActionFilterIO() {
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    public boolean isSelected() {
        return value == PASS_VALUE;
    }
    @Override
    public boolean isVirtual() {
        return true;
    }
    public void setSelected(boolean selected) {
        if (selected) {
            value = PASS_VALUE;
        } else value = BLOCK_VALUE;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (lastX != x || lastY != y) return 0;
        return value;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        ActionFilterIO.value = value;
        lastX = x;
        lastY = y;
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public String getName() {
        return "Action Filter";
    }

    @Override
    public String getDescription() {
        return "only blocks that pass this filter will be used in following actions.";
    }

    @Override
    public int getMaxValue() {
        return 1;
    }

    @Override
    public int getMinValue() {
        return 0;
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
        return value == PASS_VALUE ? "PASS (1)" : "BLOCK (0)";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(value == PASS_VALUE ? Color.GREEN : Color.RED);
        g.fillRect(0, 0, dim.width, dim.height);
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE_SELECTION;
    }
}
