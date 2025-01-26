package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.Layer;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class LayerMapping {
    public IPositionValueGetter input;
    public IPositionValueSetter output;
    MappingPoint[] mappingPoints;

    public LayerMapping(IPositionValueGetter input, IPositionValueSetter output, MappingPoint[] mappingPoints) {
        this.input = input;
        this.output = output;
        this.mappingPoints = mappingPoints.clone();
        Arrays.sort(this.mappingPoints, Comparator.comparing(mp -> mp.input));
    }

    public MappingPoint[] getMappingPoints() {
        return mappingPoints;
    }

    public LayerMapping withNewPoints(MappingPoint[] mappingPoints) {
        return new LayerMapping(this.input, this.output, mappingPoints);
    }

    public void applyToPoint(Dimension dim, int x, int y) {
        if (x == 20 && y == 70) {
            System.out.println();
        }
        int value = input.getValueAt(dim, x, y);
        int mapped = map(value);
        output.setValueAt(dim, x, y, mapped);
    }

    int map(int input) {    //TODO do linear interpolation
        if (input < mappingPoints[0].input) return mappingPoints[0].output;
        for (int i = 0; i < mappingPoints.length - 1; i++) {
            if (mappingPoints[i].input <= input && mappingPoints[i + 1].input > input) {  //value inbetween i and i+1
                int a = mappingPoints[i].input;
                int b = mappingPoints[i + 1].input;
                int dist = b - a;
                float t = ((float) input - a) / dist;
                float interpol = (1 - t) * mappingPoints[i].output + t * mappingPoints[i + 1].output;
                return Math.round(interpol);
            }
        }
        //no match, return highest value
        return mappingPoints[mappingPoints.length - 1].output;
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

    public static class NibbleLayerSetter implements IPositionValueSetter {
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
            if (value == 0)
                return "Absent (0)";
            try {
                String name = Annotations.getColourName(value);
                return name+"("+Integer.toString(value)+")";
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.out.println(ex);
            }
            return "ERROR";
        }
    }

    public static class BitLayerBinarySpraypaintSetter implements IPositionValueSetter {
        Random random = new Random();
        Layer layer;

        public BitLayerBinarySpraypaintSetter(Layer layer) {
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
        public String getName() {
            return "Set layer " + layer.getName() + " (spraypaint)";
        }

        @Override
        public String getDescription() {
            return "spraypaint binary layer " + layer.getName() + " (ON or OFF) based on input chance 0 to 100%.";
        }
    }
}
