package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.util.List;
import java.util.function.Consumer;
import org.pepsoft.worldpainter.layers.Layer;

public interface LayerProvider {
  Layer getLayerById(String layerId, Consumer<String> layerNotFoundError);

  List<Layer> getLayers();

  void addLayer(Layer layer);

  boolean existsLayerWithId(String layerId);
}
