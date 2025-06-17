package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.kenperlin.ImprovedNoise;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

public class PerlinNoiseIO implements IPositionValueGetter, EditableIO {
    private final float scale;
    private final float amplitude;
    private final long seed;
    private final int octaves;
    private transient ImprovedNoise generator;
    private float shift = 0, multi = 1;
    public PerlinNoiseIO(float scale, float amplitude, long seed, int octaves) {
        this.scale = scale;
        this.amplitude = amplitude;
        this.seed = seed;
        this.octaves = Math.max(Math.min(octaves, octaveNormalizer.length-1),1);
        generator = new ImprovedNoise(42069);

        //brute force collect data to force generator output range into [0,1]
        // dev note: improvedNoise does not reliable produce values across the whole histogram and there seems no way
        // of predicting the histogram based on scale ampl seed or octaves. so we have to measure average histogram
        // and adjust accordingly.
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        for (int x = -1000; x < 1000; x+=27) {
            for (int y = -1000; y < 1000; y+=27) {
                float point = getRawValueAt(x,y);
                min = Math.min(point, min);
                max = Math.max(point, max);
            }
        }
        shift = min;
        multi = (max-min);
    }

    @Override
    public String getName() {
        return "Perlin Noise";
    }

    @Override
    public String getDescription() {
        return "Perlin Noise Generator";
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public int getMaxValue() {
        return Math.round(this.amplitude);
    }

    @Override
    public int getMinValue() {
        return 0;
    }

    @Override
    public void prepareForDimension(Dimension dim) throws IllegalAccessError {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        int[] intArray = new int[data.length];

        for (int i = 0; i < data.length; i++) {
            intArray[i] = (int)data[i]; // Autoboxing converts int to Integer
        }
        return instantiateWithValues(intArray);
    }

    @Override
    public Object[] getSaveData() {
        int[] intArray = getEditableValues();
        Object[] objectArray = new Object[intArray.length];

        for (int i = 0; i < intArray.length; i++) {
            objectArray[i] = intArray[i]; // Autoboxing converts int to Integer
        }
        return objectArray;
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
        float point = (float) value / getMaxValue();
        g.setColor(new Color(point, point, point));
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.PERLIN_NOISE;
    }
    private static final float[] octaveNormalizer = new float[]{
            1,  //should be zero but would cause division error
            1,
            1 + 1/2f,
            1 + 1/2f + 1/4f,
            1 + 1/2f + 1/4f + 1/8f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f + 1/32f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f + 1/32f + 1/64f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f + 1/32f + 1/64f + 1/128f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f + 1/32f + 1/64f + 1/128f + 1/256f,
            1 + 1/2f + 1/4f + 1/8f + 1/16f + 1/32f + 1/64f + 1/128f + 1/256f + 1/512f,
    };

    /**
     * returns perlin noise value between [0,1]
     * @param x
     * @param y
     * @return
     */
    private float getRawValueAt(int x, int y) {
        float value = 0;
        for (int i = 1; i < Math.pow(2, octaves); i *= 2) { // harmonic series (?)
            //improved noise wraps at 256 by default implementation and returns [-1,1]
            double rawValue = generator.noise((x) / (scale / i), (y) / (scale / i), seed); //-1..1+
            rawValue += 1 - 0.495; //shift lower bound of histogram upwards
            rawValue *= 1.5375;
            value += (float) rawValue / i;
        }
        assert octaves <= octaveNormalizer.length -1;
        value = value / octaveNormalizer[octaves];
        return value;
    }
    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        float value = getRawValueAt(x,y);
        value -= shift;
        value /= multi;
        int finalValue = (int) Math.max(0, Math.min(amplitude, value * amplitude));

        assert finalValue >= 0;
        assert finalValue <= amplitude;
        return finalValue;
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{Math.round(scale), Math.round(amplitude), octaves, (int) seed};
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"scale", "amplitude","octaves","seed"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"the size of the noise in x/y direction.",
                "the size of the perlin noise in z direction, will produce exactly <amplitude> amount of values " +
                        "starting at zero",
                "less octaves = smoother, more octaves = bumpier",
                "seed that determines the shape of the random noise"};
    }

    private static final int SCALE_IDX = 0;
    private static final int AMPLITUDE_IDX = 1;
    private static final int OCTAVES_IDX = 2;
    private static final int SEED_IDX = 3;



    @Override
    public PerlinNoiseIO instantiateWithValues(int[] values) {
        assert values.length == 4;
        float scale = EditableIO.clamp(values[SCALE_IDX], 1, 30000);
        float amplitude = EditableIO.clamp(values[AMPLITUDE_IDX], 1, 1000);
        int octaves = (int) EditableIO.clamp(values[OCTAVES_IDX], 1, octaveNormalizer.length-1);
        long seed = (long) EditableIO.clamp(values[SEED_IDX], 0, Integer.MAX_VALUE);
        return new PerlinNoiseIO(scale, amplitude, seed, octaves);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerlinNoiseIO that = (PerlinNoiseIO) o;
        return Float.compare(scale, that.scale) == 0 && Float.compare(amplitude, that.amplitude) == 0 &&
                seed == that.seed && this.octaves == that.octaves;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scale, amplitude, seed);
    }

    @Override
    public String toString() {
        return getName();
    }
    @Override
    public String getToolTipText() {
        return getDescription();
    }
}
