package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_20Biomes;
import org.pepsoft.worldpainter.layers.Biome;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class VanillaBiomeProvider implements IPositionValueGetter {
    Map.Entry<Integer, String>[] biomes;

    public VanillaBiomeProvider() {
        HashMap<Integer, String> validBiomes = new HashMap<>();
        for (int biomeIdx = 0; biomeIdx < Minecraft1_20Biomes.BIOME_NAMES.length; biomeIdx++) {
            String name = Minecraft1_20Biomes.BIOME_NAMES[biomeIdx];
            if (name != null) validBiomes.put(biomeIdx, name);
        }
        biomes = new Map.Entry[validBiomes.size()];
        biomes = validBiomes.entrySet().toArray(biomes);
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }

    @Override
    public String getName() {
        return "Get Biome";
    }

    @Override
    public String getDescription() {
        return "Get biome type";
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getLayerValueAt(Biome.INSTANCE, x, y);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return biomes.length;
    }

    @Override
    public String valueToString(int value) {
        if (value < 0 || value >= biomes.length) return "NULL";
        return biomes[value].getValue();
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, dim.width, dim.height);
    }
}
