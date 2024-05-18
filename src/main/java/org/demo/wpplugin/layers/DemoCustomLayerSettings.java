package org.demo.wpplugin.layers;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.exporters.ExporterSettings;

public class DemoCustomLayerSettings implements ExporterSettings {
    public DemoCustomLayerSettings(BezierPathLayer layer) {
        this.layer = layer;
    }

    @Override
    public boolean isApplyEverywhere() {
        return false;
    }

    @Override
    public Layer getLayer() {
        return layer;
    }

    @Override
    public ExporterSettings clone() {
        // This demo implementation contains no state; if it were to be expanded to contain actual state then this
        // method should be re-implemented to create an actual deep copy:
        return this;
    }

    private final BezierPathLayer layer;
}