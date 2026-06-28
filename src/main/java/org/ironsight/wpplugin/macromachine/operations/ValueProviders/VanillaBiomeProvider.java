package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import static org.pepsoft.worldpainter.Constants.TILE_SIZE_BITS;

import java.awt.*;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_21Biomes;
import org.pepsoft.worldpainter.layers.Biome;

public class VanillaBiomeProvider implements IPositionValueGetter, IPositionValueSetter
{
    String[] biomes;
    private static final int[] BIOME_COLORS = new int[256];

    static {
        BIOME_COLORS[0]   = new Color(0, 80, 255).getRGB();        // Ocean
        BIOME_COLORS[1]   = new Color(90, 180, 70).getRGB();       // Plains
        BIOME_COLORS[2]   = new Color(210, 180, 80).getRGB();      // Desert
        BIOME_COLORS[3]   = new Color(120, 140, 100).getRGB();     // Windswept Hills
        BIOME_COLORS[4]   = new Color(50, 140, 50).getRGB();       // Forest
        BIOME_COLORS[5]   = new Color(40, 110, 70).getRGB();       // Taiga
        BIOME_COLORS[6]   = new Color(80, 100, 60).getRGB();       // Swamp
        BIOME_COLORS[7]   = new Color(0, 100, 180).getRGB();       // River
        BIOME_COLORS[8]   = new Color(180, 40, 40).getRGB();       // Nether Wastes
        BIOME_COLORS[9]   = new Color(140, 80, 200).getRGB();      // The End
        BIOME_COLORS[10]  = new Color(140, 190, 220).getRGB();     // Frozen Ocean
        BIOME_COLORS[11]  = new Color(160, 190, 210).getRGB();     // Frozen River
        BIOME_COLORS[12]  = new Color(220, 230, 240).getRGB();     // Snowy Plains
        BIOME_COLORS[13]  = new Color(200, 210, 220).getRGB();     // Snowy Mountains
        BIOME_COLORS[14]  = new Color(160, 130, 100).getRGB();     // Mushroom Fields
        BIOME_COLORS[15]  = new Color(150, 120, 90).getRGB();      // Mushroom Field Shore
        BIOME_COLORS[16]  = new Color(210, 190, 140).getRGB();     // Beach
        BIOME_COLORS[17]  = new Color(200, 170, 70).getRGB();      // Desert Hills
        BIOME_COLORS[18]  = new Color(60, 130, 60).getRGB();       // Wooded Hills
        BIOME_COLORS[19]  = new Color(50, 110, 70).getRGB();       // Taiga Hills
        BIOME_COLORS[20]  = new Color(130, 140, 110).getRGB();     // Mountain Edge
        BIOME_COLORS[21]  = new Color(40, 180, 50).getRGB();       // Jungle
        BIOME_COLORS[22]  = new Color(50, 160, 60).getRGB();       // Jungle Hills
        BIOME_COLORS[23]  = new Color(80, 170, 70).getRGB();       // Sparse Jungle
        BIOME_COLORS[24]  = new Color(0, 40, 140).getRGB();        // Deep Ocean
        BIOME_COLORS[25]  = new Color(150, 150, 140).getRGB();     // Stone Shore
        BIOME_COLORS[26]  = new Color(200, 210, 200).getRGB();     // Snowy Beach
        BIOME_COLORS[27]  = new Color(70, 170, 60).getRGB();       // Birch Forest
        BIOME_COLORS[28]  = new Color(80, 160, 65).getRGB();       // Birch Forest Hills
        BIOME_COLORS[29]  = new Color(30, 100, 30).getRGB();       // Dark Forest
        BIOME_COLORS[30]  = new Color(50, 120, 80).getRGB();       // Snowy Taiga
        BIOME_COLORS[31]  = new Color(60, 115, 85).getRGB();       // Snowy Taiga Hills
        BIOME_COLORS[32]  = new Color(45, 130, 75).getRGB();       // Old Growth Pine Taiga
        BIOME_COLORS[33]  = new Color(55, 120, 80).getRGB();       // Giant Tree Taiga Hills
        BIOME_COLORS[34]  = new Color(100, 140, 90).getRGB();      // Windswept Forest
        BIOME_COLORS[35]  = new Color(160, 180, 60).getRGB();      // Savanna
        BIOME_COLORS[36]  = new Color(150, 170, 80).getRGB();      // Savanna Plateau
        BIOME_COLORS[37]  = new Color(190, 140, 60).getRGB();      // Badlands
        BIOME_COLORS[38]  = new Color(180, 130, 55).getRGB();      // Wooded Badlands
        BIOME_COLORS[39]  = new Color(170, 120, 50).getRGB();      // Badlands Plateau
        BIOME_COLORS[40]  = new Color(100, 60, 140).getRGB();      // Small End Islands
        BIOME_COLORS[41]  = new Color(120, 70, 160).getRGB();      // End Midlands
        BIOME_COLORS[42]  = new Color(140, 80, 180).getRGB();      // End Highlands
        BIOME_COLORS[43]  = new Color(90, 50, 120).getRGB();       // End Barrens
        BIOME_COLORS[44]  = new Color(0, 120, 220).getRGB();       // Warm Ocean
        BIOME_COLORS[45]  = new Color(0, 100, 200).getRGB();       // Lukewarm Ocean
        BIOME_COLORS[46]  = new Color(60, 120, 190).getRGB();      // Cold Ocean
        BIOME_COLORS[47]  = new Color(0, 80, 200).getRGB();        // Deep Warm Ocean
        BIOME_COLORS[48]  = new Color(0, 70, 180).getRGB();        // Deep Lukewarm Ocean
        BIOME_COLORS[49]  = new Color(40, 90, 170).getRGB();       // Deep Cold Ocean
        BIOME_COLORS[50]  = new Color(80, 130, 180).getRGB();      // Deep Frozen Ocean
        BIOME_COLORS[127] = new Color(0, 0, 0).getRGB();           // The Void
        BIOME_COLORS[129] = new Color(100, 190, 80).getRGB();      // Sunflower Plains
        BIOME_COLORS[130] = new Color(200, 180, 90).getRGB();      // Desert Lakes
        BIOME_COLORS[131] = new Color(130, 150, 110).getRGB();     // Windswept Gravelly Hills
        BIOME_COLORS[132] = new Color(70, 170, 60).getRGB();       // Flower Forest
        BIOME_COLORS[133] = new Color(45, 115, 75).getRGB();       // Taiga Mountains
        BIOME_COLORS[134] = new Color(70, 90, 55).getRGB();        // Swamp Hills
        BIOME_COLORS[140] = new Color(170, 200, 220).getRGB();     // Ice Spikes
        BIOME_COLORS[149] = new Color(50, 170, 60).getRGB();       // Modified Jungle
        BIOME_COLORS[151] = new Color(80, 160, 70).getRGB();       // Modified Jungle Edge
        BIOME_COLORS[155] = new Color(75, 170, 65).getRGB();       // Old Growth Birch Forest
        BIOME_COLORS[156] = new Color(85, 165, 70).getRGB();       // Tall Birch Hills
        BIOME_COLORS[157] = new Color(40, 100, 40).getRGB();       // Dark Forest Hills
        BIOME_COLORS[158] = new Color(55, 125, 85).getRGB();       // Snowy Taiga Mountains
        BIOME_COLORS[160] = new Color(50, 130, 80).getRGB();       // Old Growth Spruce Taiga
        BIOME_COLORS[161] = new Color(60, 125, 85).getRGB();       // Giant Spruce Taiga Hills
        BIOME_COLORS[162] = new Color(120, 145, 105).getRGB();     // Modified Gravelly Mountains
        BIOME_COLORS[163] = new Color(150, 170, 55).getRGB();      // Windswept Savanna
        BIOME_COLORS[164] = new Color(140, 160, 70).getRGB();      // Shattered Savanna Plateau
        BIOME_COLORS[165] = new Color(200, 120, 40).getRGB();      // Eroded Badlands
        BIOME_COLORS[166] = new Color(175, 125, 50).getRGB();      // Modified Wooded Badlands Plateau
        BIOME_COLORS[167] = new Color(165, 115, 45).getRGB();      // Modified Badlands Plateau
        BIOME_COLORS[168] = new Color(50, 180, 60).getRGB();       // Bamboo Jungle
        BIOME_COLORS[169] = new Color(60, 170, 70).getRGB();       // Bamboo Jungle Hills
        BIOME_COLORS[170] = new Color(130, 100, 50).getRGB();      // Soul Sand Valley
        BIOME_COLORS[171] = new Color(160, 40, 40).getRGB();       // Crimson Forest
        BIOME_COLORS[172] = new Color(40, 160, 140).getRGB();      // Warped Forest
        BIOME_COLORS[173] = new Color(90, 80, 80).getRGB();        // Basalt Deltas
        BIOME_COLORS[174] = new Color(140, 110, 70).getRGB();      // Dripstone Caves
        BIOME_COLORS[175] = new Color(60, 140, 80).getRGB();       // Lush Caves
        BIOME_COLORS[245] = new Color(180, 200, 160).getRGB();     // Pale Garden
        BIOME_COLORS[246] = new Color(220, 160, 180).getRGB();     // Cherry Grove
        BIOME_COLORS[247] = new Color(70, 130, 50).getRGB();       // Mangrove Swamp
        BIOME_COLORS[248] = new Color(20, 10, 40).getRGB();        // Deep Dark
        BIOME_COLORS[249] = new Color(180, 200, 220).getRGB();     // Frozen Peaks
        BIOME_COLORS[250] = new Color(100, 150, 130).getRGB();     // Grove
        BIOME_COLORS[251] = new Color(170, 180, 190).getRGB();     // Jagged Peaks
        BIOME_COLORS[252] = new Color(130, 190, 80).getRGB();      // Meadow
        BIOME_COLORS[253] = new Color(190, 200, 210).getRGB();     // Snowy Slopes
        BIOME_COLORS[254] = new Color(150, 160, 170).getRGB();     // Stony Peaks
        BIOME_COLORS[255] = new Color(180, 180, 180).getRGB();     // Auto Biome
    }

    public VanillaBiomeProvider() {
        biomes = Minecraft1_21Biomes.BIOME_NAMES.clone();
        outputValues = IntStream.range(-1, Minecraft1_21Biomes.BIOME_NAMES.length).toArray();
        outputValues[0] = IGNORE_VALUE;
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
    public int[] getAllPossibleValues() {
        return getAllOutputValues();
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
    public IMappingValue instantiateFrom(IoParameter[] data) {
        return new VanillaBiomeProvider();
    }

    @Override
    public IoParameter[] getSaveData() {
        return new IoParameter[0];
    }

    @Override
    public int getMaxValue() {
        return biomes.length - 1;
    }

    @Override
    public String valueToString(int value) {
        if (value == IGNORE_VALUE)
            return "Skip";
        if (value < 0 || value >= biomes.length)
            return "INVALID (" + value + ")";
        if (value == 255)
            return "Auto Biome";
        if (biomes[value] == null)
            return "zzz-NULL-(" + value + ")";
        return biomes[value];
    }

    @Override
    public boolean isDiscrete() {
        return true;
    }

    @Override
    public void paint(Graphics g, int value, java.awt.Dimension dim) {
        if (isIgnoreValue(value))
            return;
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

    private final int[] inputValues = IntStream.range(0, Minecraft1_21Biomes.BIOME_NAMES.length).toArray();
    private final int[] outputValues;

    @Override
    public int[] getAllInputValues() {
        return Arrays.copyOf(inputValues, inputValues.length);
    }

    @Override
    public boolean isIgnoreValue(int value) {
        return value == IGNORE_VALUE;
    }

    @Override
    public int[] getAllOutputValues() {
        return Arrays.copyOf(outputValues, outputValues.length);
    }

    @Override
    public void prepareForDimension(Dimension dim) {
    }

    @Override
    public int getColorForValue(int value) {
        if (value < 0 || value >= BIOME_COLORS.length) return 0;
        return BIOME_COLORS[value];
    }

    @Override
    public String getIconName() {
        return "biome_provider.svg";
    }

}
