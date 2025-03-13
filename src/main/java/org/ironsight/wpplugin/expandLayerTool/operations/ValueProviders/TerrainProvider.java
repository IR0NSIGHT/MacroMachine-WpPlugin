package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

import java.awt.*;

public class TerrainProvider implements IPositionValueGetter, IPositionValueSetter {
    private static final ColourScheme colorScheme = new HardcodedColourScheme();

    public TerrainProvider() {
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
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
    public void prepareForDimension(Dimension dim) {

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
    public IMappingValue instantiateFrom(Object[] data) {
        return new TerrainProvider();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
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
        g.drawImage(Terrain.values()[value].getScaledIcon(Math.min(dim.height, dim.width),colorScheme),0,0,null);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.TERRAIN;
    }
}
