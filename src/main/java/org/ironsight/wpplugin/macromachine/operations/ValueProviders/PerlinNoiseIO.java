package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import com.kenperlin.ImprovedNoise;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.util.PerlinNoise;
import org.pepsoft.worldpainter.Dimension;

import java.awt.*;
import java.util.Objects;

public class PerlinNoiseIO  implements IPositionValueGetter{
    private float scale;
    private final  float amplitude;
    private final  long seed;
    private transient ImprovedNoise generator;

    public PerlinNoiseIO(float scale, float amplitude, long seed) {
        this.scale = scale;
        this.amplitude = amplitude;
        this.seed = seed;
        generator = new ImprovedNoise(42069);
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
        return  new PerlinNoiseIO (((Double)data[0]).floatValue(),((Double)data[1]).floatValue(),
                ((Double)data[2]).longValue());
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{(double)scale, (double)amplitude, (double)seed};
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
        float point = (float)value/getMaxValue();
        g.setColor(new Color(point,point,point));
        g.fillRect(0,0,dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.PERLIN_NOISE;
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        scale = 400;
        int value = 0;
        for (int i = 1; i < 32; i*=2) { // harmonic series (?)
            //improved noise wraps at 256 by default implementation and returns [-1,1]
            double rawValue = generator.noise((x) / (scale / i),(y) / (scale/i),0); //-1..1
            rawValue /= 2;  //bring to 0..2
            rawValue += .27;  //magic number to force the noise to be as much as possible in [0,1] range
            rawValue *= 1.975f;
            rawValue *= amplitude/i;
            value += Math.round(rawValue);
        }
        value /= 2;
        value = (int)Math.max(0,Math.min(amplitude,value));

        assert value >= 0;
        assert value <= amplitude;
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PerlinNoiseIO that = (PerlinNoiseIO) o;
        return Float.compare(scale, that.scale) == 0 && Float.compare(amplitude, that.amplitude) == 0 &&
                seed == that.seed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(scale, amplitude, seed);
    }
}
