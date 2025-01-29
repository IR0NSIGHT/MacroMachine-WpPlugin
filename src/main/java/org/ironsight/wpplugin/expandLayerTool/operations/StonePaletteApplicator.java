package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;

import java.util.Arrays;
import java.util.HashSet;

public class StonePaletteApplicator implements IPositionValueSetter {
    private final Terrain[] materials;
    private transient HashSet<Terrain> mats;

    public StonePaletteApplicator() {
        materials = new Terrain[]{Terrain.GRASS, Terrain.GRAVEL, Terrain.STONE, Terrain.COBBLESTONE,
                Terrain.MOSSY_COBBLESTONE, Terrain.GRANITE, Terrain.DIORITE, Terrain.ANDESITE, Terrain.DEEPSLATE,
                Terrain.STONE_MIX, Terrain.ROCK};
        mats = new HashSet<>(Arrays.asList(materials));
    }

    @Override
    public String getName() {
        return "Stone Palette";
    }

    @Override
    public String getDescription() {
        return "a palette of most common stones";
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setTerrainAt(x, y, materials[value]);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return materials.length - 1;
    }

    @Override
    public String valueToString(int value) {
        if (value < 0 || value > materials.length) return "INVALID";
        return materials[value].getName() + "(" + value + ")";
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass()) && Arrays.equals(materials,
                ((StonePaletteApplicator) obj).materials);
    }
}
