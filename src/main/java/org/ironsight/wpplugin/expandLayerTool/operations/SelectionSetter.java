package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.selection.SelectionBlock;

public class SelectionSetter implements IPositionValueSetter, IPositionValueGetter {
    private static final String[] names = new String[]{"Not Selected", "Selected", "Dont change"};

    @Override
    public String getName() {
        return "Set Selection";
    }

    @Override
    public String getDescription() {
        return "Add or remove a position from the selection layer";
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        if (value == 2) return;
        dim.setBitLayerValueAt(SelectionBlock.INSTANCE, x, y, value == 1);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return names.length;
    }

    @Override
    public String valueToString(int value) {
        return names[value];
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ? 1 : 0;
    }
}
