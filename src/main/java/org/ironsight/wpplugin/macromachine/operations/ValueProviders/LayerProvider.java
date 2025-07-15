package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import org.pepsoft.worldpainter.layers.Layer;

import java.util.List;
import java.util.function.Consumer;

public interface LayerProvider {
    Layer getLayerById(String layerId, Consumer<String> layerNotFoundError);

    List<Layer> getLayers();

    void addLayer(Layer layer);

    boolean existsLayerWithId(String layerId);
}
