package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

import java.awt.*;
import java.io.IOException;
import java.io.ObjectInputStream;

public class TerrainIO implements IPositionValueGetter, IPositionValueSetter {
    private static ColourScheme colorScheme = new HardcodedColourScheme();

    public TerrainIO() {
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getTerrainAt(x, y).ordinal();
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setTerrainAt(x, y, Terrain.values()[value]);
    }

    @Override
    public String getName() {
        return "Terrain";
    }

    @Override
    public String getDescription() {
        return "surface terrain blocks";
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return Terrain.values().length - 1;
    }

    @Override
    public String valueToString(int value) {
        return Terrain.values()[value].name();
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    public int getColour(int value) {
        assert colorScheme != null;
        Terrain t = Terrain.values()[value];
        return t.getColour(1234L, 0, 0, 0, 0, null, colorScheme);
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        int color = getColour(value);
        g.setColor(new Color(color));
        g.fillRect(0, 0, dim.width, dim.height);
    }
}
