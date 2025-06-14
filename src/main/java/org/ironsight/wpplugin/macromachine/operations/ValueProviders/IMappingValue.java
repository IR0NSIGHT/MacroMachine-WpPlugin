package org.ironsight.wpplugin.macromachine.operations.ValueProviders;


import org.ironsight.wpplugin.macromachine.operations.MappingPoint;
import org.ironsight.wpplugin.macromachine.operations.ProviderType;

import java.awt.*;

public interface IMappingValue extends IDisplayUnit {
    static int sanitizeValue(int value, IMappingValue mappingValue) {
        return Math.max(Math.min(value, mappingValue.getMaxValue()), mappingValue.getMinValue());
    }

    static MappingPoint[] getAllPointsForDiscreteIO(IPositionValueSetter mappingValue, int outputValue) {
        MappingPoint[] arr = new MappingPoint[range(mappingValue)];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = new MappingPoint(i,outputValue);
        }
        return arr;
    }

    static int range(IMappingValue mappingValue) {
        return mappingValue.getMaxValue() - mappingValue.getMinValue() + 1;
    }

    /**
     * virtual values dont exist in the "real world", the dimension. f.e. actionfilter or intermediate values are
     * virtual
     * height slope and waterheight are real values from the world.
     * @return
     */
    boolean isVirtual();

    int getMaxValue();

    int getMinValue();

    void prepareForDimension(org.pepsoft.worldpainter.Dimension dim) throws IllegalAccessError;

    IMappingValue instantiateFrom(Object[] data);

    Object[] getSaveData();

    String valueToString(int value);

    /**
     * if the output layer can be smoothly interpolated or only knows discrete values
     *
     * @return
     */
    boolean isDiscrete();

    void paint(Graphics g, int value, java.awt.Dimension dim);

    ProviderType getProviderType();

    boolean equals(Object o);
}
