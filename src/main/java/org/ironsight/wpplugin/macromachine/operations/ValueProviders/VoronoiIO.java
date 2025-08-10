package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.flowpowered.noise.module.source.Voronoi;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

public class VoronoiIO implements IPositionValueGetter, EditableIO {
    private final int min, max, seed, frequency;
    private final float scale;
    private final Voronoi generator;
    private final int valueRange;

    public VoronoiIO(int min, int max, int seed, int frequency, int scale) {
        this.max = max;
        this.min = min;
        this.seed = seed;
        this.frequency = frequency;
        this.scale = scale;
        generator = new Voronoi();
        generator.setSeed(seed);
        generator.setFrequency(frequency);
        valueRange = max - min + 1;
    }

    @Override
    public String getName() {
        return "Voronoi";
    }

    @Override
    public String getDescription() {
        return "Voronoi Noise Generator";
    }

    @Override
    public String getToolTipText() {
        return "Produces noise with cell like structures";
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public int getMaxValue() {
        return max;
    }

    @Override
    public int getMinValue() {
        return min;
    }

    @Override
    public void prepareForDimension(Dimension dim) throws IllegalAccessError {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        return new VoronoiIO((Integer) data[0], (Integer) data[1], (Integer) data[2], (Integer) data[3],
                (Integer) data[4]);
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{min, max, seed, frequency, (int) scale};
    }

    @Override
    public String valueToString(int value) {
        return Integer.toString(value);
    }

    @Override
    public boolean isDiscrete() {
        return false;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        int point = 255 * Math.round(((float) (value - getMinValue())) / getMaxValue());
        g.setColor(new Color(point, point, point));
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.VORONOI_NOISE;
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        if (dim != null && !dim.getExtent().contains(x >> TILE_SIZE_BITS, y >> TILE_SIZE_BITS))
            return getMinValue();
        float xf = x / scale, yf = y / scale;
        return (int) ((generator.getValue(xf, yf, 7) + 1d) / 2d * valueRange + getMinValue());
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{min, max, seed, frequency, Math.round(scale)};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"min", "max", "seed", "frequency", "scale"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"min", "max", "seed", "frequency", "scale"};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new VoronoiIO(values[0], values[1], values[2], values[3], values[4]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VoronoiIO voronoiIO = (VoronoiIO) o;
        return min == voronoiIO.min && max == voronoiIO.max && seed == voronoiIO.seed &&
                frequency == voronoiIO.frequency &&
                Float.compare(scale, voronoiIO.scale) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max, seed, frequency, scale);
    }
}
