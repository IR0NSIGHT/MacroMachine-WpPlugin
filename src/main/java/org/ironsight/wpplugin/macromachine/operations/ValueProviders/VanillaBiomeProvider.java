package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes;
import org.pepsoft.worldpainter.layers.Biome;

import java.awt.*;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class VanillaBiomeProvider implements IPositionValueGetter, IPositionValueSetter {
    String[] biomes;

    public VanillaBiomeProvider() {
        biomes = Minecraft1_21Biomes.BIOME_NAMES.clone();
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
    @Override
    public boolean equals(Object obj) {
        return obj != null && this.getClass().equals(obj.getClass());
    }
    @Override
    public String toString() {
        return getName();
    }
    @Override
    public String getName() {
        return "Biome";
    }

    @Override
    public int hashCode() {
        return getProviderType().hashCode();
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Get biome type of a position";
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (!dim.getExtent().contains(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return getMinValue();
        return dim.getLayerValueAt(Biome.INSTANCE, x, y);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new VanillaBiomeProvider();
    }

    @Override
    public Object[] getSaveData() {
        return new Object[0];
    }

    @Override
    public int getMaxValue() {
        return biomes.length-1;
    }

    @Override
    public String valueToString(int value) {
        if (value < 0 || value >= biomes.length) return "INVALID (" + value + ")";
        if (value == 255)
            return "Auto Biome";
        if (biomes[value] == null) return "zzz-NULL-(" + value + ")";
        return biomes[value];
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

    @Override
    public ProviderType getProviderType() {
        return ProviderType.VANILLA_BIOME;
    }

    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        dim.setLayerValueAt(Biome.INSTANCE, x, y, value);
    }

    @Override
    public void prepareForDimension(Dimension dim) {

    }
}
