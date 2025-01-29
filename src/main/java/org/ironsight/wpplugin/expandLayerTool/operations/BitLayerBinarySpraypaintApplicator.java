package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.util.Objects;
import java.util.Random;

public class BitLayerBinarySpraypaintApplicator implements IPositionValueSetter, IPositionValueGetter {
    transient Random random = new Random();
    Layer layer;

    public BitLayerBinarySpraypaintApplicator(Layer layer) {
        this.layer = layer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BitLayerBinarySpraypaintApplicator that = (BitLayerBinarySpraypaintApplicator) o;
        return Objects.equals(layer, that.layer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(layer);
    }

    /**
     * @param dim
     * @param x
     * @param y
     * @param value 0 to 100 chance
     */
    @Override
    public void setValueAt(Dimension dim, int x, int y, int value) {
        long positionHash = ((long) x * 73856093L) ^ ((long) y * 19349663L);
        random.setSeed(positionHash);
        int randInt = random.nextInt(100);
        boolean set = value >= randInt;
        dim.setBitLayerValueAt(layer, x, y, set);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public int getMaxValue() {
        return 100;
    }

    @Override
    public String valueToString(int value) {
        return value + "%";
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return dim.getBitLayerValueAt(layer, x, y) ? 100 : 0;
    }

    @Override
    public String getName() {
        return "Set layer " + layer.getName() + " (spraypaint)";
    }

    @Override
    public String getDescription() {
        return "spraypaint binary layer " + layer.getName() + " (ON or OFF) based on input chance 0 to 100%.";
    }
}
