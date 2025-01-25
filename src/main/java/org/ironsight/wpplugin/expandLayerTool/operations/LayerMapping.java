package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class LayerMapping {
    PositionValueGetter input;
    PositionValueSetter output;

    public MappingPoint[] getMappingPoints() {
        return mappingPoints;
    }

    MappingPoint[] mappingPoints;

    public LayerMapping(PositionValueGetter input, PositionValueSetter output, MappingPoint[] mappingPoints) {
        this.input = input;
        this.output = output;
        this.mappingPoints = mappingPoints.clone();
        Arrays.sort(this.mappingPoints, Comparator.comparing(mp -> mp.input));
    }

    public LayerMapping withNewPoints(MappingPoint[] mappingPoints) {
        return new LayerMapping(this.input, this.output, mappingPoints);
    }

    int map(int input) {    //TODO do linear interpolation
        if (input < mappingPoints[0].input)
            return mappingPoints[0].output;
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

    public void applyToPoint(Dimension dim, int x, int y) {
        if (x == 20 && y == 70) {
            System.out.println();
        }
        int value = input.getValueAt(dim, x, y);
        int mapped = map(value);
        output.setValueAt(dim, x, y, mapped);
    }

    int reverseMap(int input) {
        throw new NotImplementedException(); //sorry i was lazy
    }

    public static class MappingPoint {
        public MappingPoint(int input, int output) {
            this.input = input;
            this.output = output;
        }

        public final int input;
        public final int output;

        @Override
        public String toString() {
            return "MappingPoint{" +
                    "input=" + input +
                    ", output=" + output +
                    '}';
        }
    }

    interface PositionValueGetter {
        int getValueAt(Dimension dim, int x, int y);
    }

    interface PositionValueSetter {
        void setValueAt(Dimension dim, int x, int y, int value);
    }

    public static class SlopeProvider implements PositionValueGetter {
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
    }

    public static class NibbleLayerSetter implements PositionValueSetter {
        private final Layer layer;

        public NibbleLayerSetter(Layer layer) {
            this.layer = layer;
        }

        @Override
        public void setValueAt(Dimension dim, int x, int y, int value) {
            dim.setLayerValueAt(layer, x, y, value);
        }
    }

    public static class BitLayerBinarySpraypaintSetter implements PositionValueSetter {
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
            dim.setBitLayerValueAt(layer, x, y, set );
        }
    }
}
