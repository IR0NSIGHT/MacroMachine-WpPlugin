package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.awt.*;

public class SelectionSetter implements IPositionValueSetter, IPositionValueGetter {
    private static final String[] names = new String[]{"Not Selected", "Selected"};

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
        return names.length - 1;
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
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        if (value == 0) return;
        g.setColor(Color.red);
        g.fillRect(0, 0, dim.width, dim.height);
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
