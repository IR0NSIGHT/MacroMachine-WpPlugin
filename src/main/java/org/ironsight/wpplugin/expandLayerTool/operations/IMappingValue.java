package org.ironsight.wpplugin.expandLayerTool.operations;

import java.awt.*;

public interface IMappingValue {
    int getMinValue();

    int getMaxValue();

    String valueToString(int value);

    /**
     * if the output layer can be smoothly interpolated or only knows discrete values
     *
     * @return
     */
    boolean isDiscrete();

    void paint(Graphics g, int value, java.awt.Dimension dim);
}
