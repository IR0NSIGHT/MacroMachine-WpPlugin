package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Layer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class LayerMapping {
    PositionValue input;
    Layer output;
    MappingPoint[] mappingPoints;

    public LayerMapping(PositionValue input, Layer output, MappingPoint[] mappingPoints) {
        this.input = input;
        this.output = output;
        this.mappingPoints = mappingPoints;
    }

    int map(int input) {    //TODO do linear interpolation
        for (int i = 0; i < mappingPoints.length; i++) {
            if (mappingPoints[i].input >= input) return mappingPoints[i].output;
        }
        //no match, return highest value
        return mappingPoints[mappingPoints.length - 1].output;
    }

    public void applyToPoint(Dimension dim, int x, int y) {
        if (x == 20 && y == 70) {
            System.out.println("");
        }
        int value = input.getValueAt(dim, x, y);
        int mapped = map(value);
        dim.setLayerValueAt(output, x, y, mapped);
    }

    int reverseMap(int input) {
        throw new NotImplementedException(); //sorry i was lazy
    }

    public static class MappingPoint {
        public MappingPoint(int input, int output) {
            this.input = input;
            this.output = output;
        }

        int input;
        int output;
    }

    interface PositionValue {
        int getValueAt(Dimension dim, int x, int y);
    }

    public static class SlopeProvider implements PositionValue {
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
}
