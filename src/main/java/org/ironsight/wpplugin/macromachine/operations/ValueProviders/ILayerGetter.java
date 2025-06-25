package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.layers.Layer;

public interface ILayerGetter {
    public String getLayerName();
    public String getLayerId();
    public boolean isCustomLayer();
    public Layer getLayer();
}
