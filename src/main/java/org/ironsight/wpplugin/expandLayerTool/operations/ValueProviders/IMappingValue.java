package org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders;


import org.ironsight.wpplugin.expandLayerTool.operations.ProviderType;

import java.awt.*;

public interface IMappingValue {
    static int sanitizeValue(int value, IMappingValue mappingValue) {
        return Math.max(Math.min(value, mappingValue.getMaxValue()), mappingValue.getMinValue());
    }

    int getMaxValue();

    int getMinValue();

    void prepareForDimension(org.pepsoft.worldpainter.Dimension dim);

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
