package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.ColourScheme;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.colourschemes.HardcodedColourScheme;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.stream.IntStream;

public class TerrainProvider implements IPositionValueGetter, IPositionValueSetter {
    private static final ColourScheme colorScheme = new HardcodedColourScheme();
    private final int IGNORE = Integer.MAX_VALUE;
    private final int[] outputValues;
    private final int[] inputValues;
    public TerrainProvider() {
        outputValues = IntStream.range(-1, Terrain.values().length).toArray();
        outputValues[0] = IGNORE;
        inputValues = IntStream.range(0,Terrain.values().length).toArray();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        Terrain t = dim.getTerrainAt(x, y);
        if (t == null)
            return getMinValue();
        return t.ordinal();
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public int[] getAllPossibleValues() {
        return new int[0];
    }

    @Override
    public boolean isVirtual() {
        return false;
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
    public boolean isIgnoreValue(int value) {
        return value == IGNORE;
    }

    @Override
    public int[] getAllInputValues() {
        return Arrays.copyOf(inputValues,inputValues.length);
    }

    @Override
    public int[] getAllOutputValues() {
        return Arrays.copyOf(outputValues, outputValues.length);
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
        if (value == IGNORE)
            return "skip";
        return Terrain.values()[value].getName();
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
        Terrain terrain = Terrain.values()[value];
        BufferedImage terrainImg = terrain.getScaledIcon(Math.min(dim.height, dim.width), colorScheme);
        g.drawImage(terrainImg, 0, 0, null);
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.TERRAIN;
    }
}
