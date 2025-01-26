package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.biomeschemes.Minecraft1_20Biomes;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Biome;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.selection.SelectionBlock;

import java.util.*;

public class LayerMapping implements IDisplayUnit {
    public final IPositionValueGetter input;
    public final IPositionValueSetter output;
    public ActionType actionType = ActionType.SET;
    MappingPoint[] mappingPoints;
    private String name = "Mapping";
    private String description = "Mapping one property to another";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LayerMapping mapping = (LayerMapping) o;
        return Objects.equals(input, mapping.input) && Objects.equals(output, mapping.output) && actionType == mapping.actionType && Arrays.equals(mappingPoints, mapping.mappingPoints) && Objects.equals(name, mapping.name) && Objects.equals(description, mapping.description);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(input, output, actionType, name, description);
        result = 31 * result + Arrays.hashCode(mappingPoints);
        return result;
    }

    public LayerMapping(IPositionValueGetter input, IPositionValueSetter output, MappingPoint[] mappingPoints,
                        ActionType type, String name, String description) {
        this.name = name;
        this.description = description;
        this.input = input;
        this.output = output;
        this.mappingPoints = mappingPoints.clone();
        this.actionType = type;
        Arrays.sort(this.mappingPoints, Comparator.comparing(mp -> mp.input));
    }

    public MappingPoint[] getMappingPoints() {
        return mappingPoints;
    }

    public LayerMapping withNewPoints(MappingPoint[] mappingPoints) {
        return new LayerMapping(this.input, this.output, mappingPoints, this.getActionType(), this.getName(), this.getDescription());
    }

    public ActionType getActionType() {
        return actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public void applyToPoint(Dimension dim, int x, int y) {
        int value = input.getValueAt(dim, x, y);
        int modifier = map(value);

        int existingValue = output instanceof IPositionValueGetter ? ((IPositionValueGetter) output).getValueAt(dim,
                x, y) : 0;
        int outputValue;
        switch (actionType) {
            case SET:
                outputValue = modifier;
                break;
            case DIVIDE:
                outputValue = Math.round(1f * existingValue / modifier);
                break;
            case MULTIPLY:
                outputValue = existingValue * modifier;
                break;
            case DECREMENT:
                outputValue = existingValue - modifier;
                break;
            case INCREMENT:
                outputValue = existingValue + modifier;
                break;
            case MIN:
                outputValue = Math.min(existingValue, modifier);
                break;
            case MAX:
                outputValue = Math.max(existingValue, modifier);
                break;
            default:
                throw new EnumConstantNotPresentException(ActionType.class, actionType.displayName);
        }

        output.setValueAt(dim, x, y, outputValue);
    }

    int map(int input) {    //TODO do linear interpolation
        if (input < mappingPoints[0].input) return mappingPoints[0].output;
        for (int i = 0; i < mappingPoints.length - 1; i++) {
            if (mappingPoints[i].input <= input && mappingPoints[i + 1].input > input) {  //value inbetween i and i+1
                if (output.isDiscrete()) {
                    return mappingPoints[i + 1].output;
                } else {
                    int a = mappingPoints[i].input;
                    int b = mappingPoints[i + 1].input;
                    int dist = b - a;
                    float t = ((float) input - a) / dist;
                    float interpol = (1 - t) * mappingPoints[i].output + t * mappingPoints[i + 1].output;
                    return Math.round(interpol);
                }

            }
        }
        //no match, return highest value
        return mappingPoints[mappingPoints.length - 1].output;
    }

    public int sanitizeInput(int value) {
        return Math.min(input.getMaxValue(), Math.max(input.getMinValue(), value));
    }

    public int sanitizeOutput(int value) {
        return Math.min(output.getMaxValue(), Math.max(output.getMinValue(), value));
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public enum ActionType {
        INCREMENT("increment"), DECREMENT("decrement"), MULTIPLY("multiply with"), DIVIDE("divide by"), SET("set to")
        , MIN("limit to"), MAX("at least");

        private final String displayName;

        ActionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    public static class MappingPoint {
        public final int input;
        public final int output;

        public MappingPoint(int input, int output) {
            this.input = input;
            this.output = output;
        }

        @Override
        public String toString() {
            return "MappingPoint{" + "input=" + input + ", output=" + output + '}';
        }
    }

    public static class StonePaletteApplicator implements IPositionValueSetter {
        private final Terrain[] materials;
        HashSet<Terrain> mats;

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

    }

    public static class VanillaBiomeProvider implements IPositionValueGetter {
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
    }

    public static class SlopeProvider implements IPositionValueGetter {
        /**
         * slope in degrees 0-90
         *
         * @param dim
         * @param x
         * @param y
         * @return
         */
        @Override
        public int getValueAt(Dimension dim, int x, int y) {
            return (int) Math.round(Math.toDegrees(Math.atan(dim.getSlope(x, y))));
        }

        @Override
        public int getMinValue() {
            return 0;
        }

        @Override
        public int getMaxValue() {
            return 90;
        }

        @Override
        public String valueToString(int value) {
            return value + "°";
        }

        @Override
        public String getName() {
            return "Get Slope";
        }

        @Override
        public String getDescription() {
            return "get the slope of a position in degrees from 0 to 90°";
        }
    }

    public static class HeightProvider implements IPositionValueGetter {

        @Override
        public int getValueAt(Dimension dim, int x, int y) {
            return Math.round(dim.getHeightAt(x, y));
        }

        @Override
        public int getMinValue() {
            return -64;
        }

        @Override
        public int getMaxValue() {
            return 364; //TODO is the correct?
        }

        @Override
        public String valueToString(int value) {
            return value + "H";
        }

        @Override
        public String getName() {
            return "Get Height";
        }

        @Override
        public String getDescription() {
            return "get the height of a position in percent for 0 to 255.";
        }
    }

    public static class SelectionSetter implements IPositionValueSetter, IPositionValueGetter {
        private final String[] names = new String[]{"Not Selected", "Selected", "Dont change"};

        @Override
        public String getName() {
            return "Set Selection";
        }

        @Override
        public String getDescription() {
            return "Add or remove a position from the selection layer";
        }

        @Override
        public void setValueAt(Dimension dim, int x, int y, int value) {
            if (value == 2) return;
            dim.setBitLayerValueAt(SelectionBlock.INSTANCE, x, y, value == 1);
        }

        @Override
        public int getMinValue() {
            return 0;
        }

        @Override
        public int getMaxValue() {
            return names.length;
        }

        @Override
        public String valueToString(int value) {
            return names[value];
        }

        @Override
        public boolean isDiscrete() {
            return true;
        }

        @Override
        public int getValueAt(Dimension dim, int x, int y) {
            return dim.getBitLayerValueAt(SelectionBlock.INSTANCE, x, y) ? 1 : 0;
        }
    }

    public static class NibbleLayerSetter implements IPositionValueSetter, IPositionValueGetter {
        private final Layer layer;

        public NibbleLayerSetter(Layer layer) {
            this.layer = layer;
        }

        @Override
        public void setValueAt(Dimension dim, int x, int y, int value) {
            dim.setLayerValueAt(layer, x, y, value);
        }

        @Override
        public int getMinValue() {
            return 0;
        }

        @Override
        public int getMaxValue() {
            return 15;
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
        public int getValueAt(Dimension dim, int x, int y) {
            return dim.getLayerValueAt(layer, x, y);
        }

        @Override
        public String getName() {
            return "Set layer " + layer.getName();
        }

        @Override
        public String getDescription() {
            return "Set layer " + layer.getName() + " with values 0 to 15, where 0 is absent, 15 is full";
        }
    }

    public static class AnnotationSetter extends NibbleLayerSetter {

        public AnnotationSetter() {
            super(Annotations.INSTANCE);
        }

        @Override
        public String valueToString(int value) {
            if (value == 0) return "Absent (0)";
            try {
                String name = Annotations.getColourName(value);
                return name + "(" + value + ")";
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println(ex);
            }
            return "ERROR";
        }

        @Override
        public boolean isDiscrete() {
            return true;
        }
    }

    public static class BitLayerBinarySpraypaintApplicator implements IPositionValueSetter, IPositionValueGetter {
        Random random = new Random();
        Layer layer;

        public BitLayerBinarySpraypaintApplicator(Layer layer) {
            this.layer = layer;
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
}
