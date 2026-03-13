package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Tile;

import java.awt.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE;

public class RandomNoise implements IPositionValueGetter, IPositionTileValueGetter, EditableIO
{
    private final int seed;
    // between 0 and 1
    private final float chance;
    private final Random random;
    public static final int BLOCK = 0;
    public static final int PASS = 1;

    public RandomNoise(int seed, float chance) {
        this.seed = seed;
        this.chance = chance;
        this.random = new Random(seed);
    }

    private boolean getRandomPassForPos(int x, int y, float chance, Random random) {
        long positionHash = ((long) x * 73856093L) ^ ((long) y * 19349663L);
        random.setSeed(positionHash);
        return chance > random.nextFloat();
    }

    @Override
    public int[] getEditableValues() {
        return new int[]{seed, (int) Math.ceil(1 / chance)};
    }

    @Override
    public EditableIO instantiateWithValues(int[] values) {
        return new RandomNoise(values[0], 1f / values[1]);
    }

    @Override
    public String[] getValueNames() {
        return new String[]{"seed", "1 in x blocks"};
    }

    @Override
    public String[] getValueTooltips() {
        return new String[]{"seed", "on average, this 1 block out of every X blocks receives value '1'"};
    }

    @Override
    public String getName() {
        return "Random Chance";
    }

    @Override
    public String getDescription() {
        return "Generates random values for each block with a specified chance.";
    }

    @Override
    public String getToolTipText() {
        return getDescription();
    }

    @Override
    public int[] getAllPossibleValues() {
        return getAllInputValues();
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public int getMaxValue() {
        return PASS;
    }

    @Override
    public int getMinValue() {
        return BLOCK;
    }

    private final int[] values = new int[]{BLOCK, PASS};
    @Override
    public int[] getAllInputValues() {
        return Arrays.copyOf(values, values.length);
    }

    @Override
    public void prepareForDimension(Dimension dim) throws IllegalAccessError {

    }

    @Override
    public IMappingValue instantiateFrom(Object[] data) {
        int seed = (Integer) data[0];
        int chance = (Integer) data[1];
        return (IMappingValue) instantiateWithValues(new int[]{seed, chance});
    }

    @Override
    public Object[] getSaveData() {
        return new Object[]{(Integer) seed, ((Float) (1 / chance)).intValue()};
    }

    @Override
    public String valueToString(int value) {
        return value == PASS
                ? String.format("1 in %d blocks", Math.round(1f / chance))
                : String.format("%d in %d blocks", Math.round(1f / chance) - 1, Math.round(1f / chance));
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, dim.width, dim.height);
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.RANDOM_NOISE;
    }

    @Override
    public int getValueAt(Tile tile, int tileX, int tileY) {
        return getRandomPassForPos(tileX + tile.getX() * TILE_SIZE, tileY + tile.getX() * TILE_SIZE, chance, random)
                ? PASS
                : BLOCK;
    }

    @Override
    public int getValueAt(Dimension dim, int x, int y) {
        return getRandomPassForPos(x, y, chance, random) ? PASS : BLOCK;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        RandomNoise that = (RandomNoise) o;
        return seed == that.seed && Float.compare(chance, that.chance) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(seed, chance);
    }

    @Override
    public String toString() {
        return "RandomNoise{" + "seed=" + seed + ", chance=" + chance + '}';
    }
}
