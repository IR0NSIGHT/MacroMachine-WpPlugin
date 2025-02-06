package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;

import java.awt.*;
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

        dim.setBitLayerValueAt(layer, x, y, doPaintPos(x, y, value));
    }

    private boolean doPaintPos(int x, int y, int value) {
        long positionHash = ((long) x * 73856093L) ^ ((long) y * 19349663L);
        random.setSeed(positionHash);
        int randInt = random.nextInt(100);
        boolean set = value >= randInt;
        return set;
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
        return layer.getName() + " (spraypaint)";
    }

    @Override
    public String getDescription() {
        return "spraypaint binary layer " + layer.getName() + " (ON or OFF) based on input chance 0 to 100%.";
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        //value is 0 to 100
        g.setColor(Color.black);
        g.fillRect(0, 0, dim.width, dim.height);
        g.setColor(Color.RED);
        int range = dim.width < 100 ? 10 : 100;
        int cellSize = dim.width / range;
        for (int y = 0; y <= range; y++) {
            for (int x = 0; x <= range; x++) {
                if (doPaintPos(x, y, value))
                    g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
            }
        }
    }
}
