package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

public interface ILayerGetter {
    public String getLayerName();
    public String getLayerId();
    public boolean isCustomLayer();
}
