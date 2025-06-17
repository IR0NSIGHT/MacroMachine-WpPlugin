package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

public class IntermediateValueIO implements IPositionValueSetter, IPositionValueGetter {
    private static int value;
    private static int lastX;
    private static int lastY;
    @Override
    public String toString() {
        return getName();
    }
    private final int min, max;
    private final String customName;    //user names this intermediat value to represetn "snow chance" f.e.

    public IntermediateValueIO(int min, int max, String customName) {
        this.min = min;
        this.max = max;
        this.customName = customName;
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntermediateValueIO that = (IntermediateValueIO) o;
        return min == that.min && max == that.max && Objects.equals(customName, that.customName);
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (lastX != x || lastY != y) return 0;
        return value;
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        IntermediateValueIO.value = value;
        lastX = x;
        lastY = y;
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }

    @Override
    public String getName() {
        return "Intermediate Value" + (customName.isEmpty() ? "" : "("+customName+")");
    }

    @Override
    public String getDescription() {
        return "a value to temporarily store an intermediate result. will not exist before or after macro execution, " +
                "only while the macro runs.";
    }

    @Override
    public int getMaxValue() {
        return max;
    }

    @Override
    public int getMinValue() {
        return min;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        try {
            int min = (int)EditableIO.clamp ((Integer)data[0], -100,100);
            int max =(int)EditableIO.clamp ((Integer)data[1], -100,100);
            return new IntermediateValueIO(min, max, (String)data[2]);
        } catch (Exception ex) {
            return new IntermediateValueIO(0,100,"");
        }
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{
                min,
                max,
                customName
        };
    }

    @Override
    public String valueToString(int value) {
        return Integer.toString(value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        float percent = ((float) value - getMinValue()) / (getMaxValue() - getMinValue());
        g.setColor(Color.black);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(Color.RED);
        g.fillRect(0, 0, (int) (dim.width * percent), dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.INTERMEDIATE;
    }
}
