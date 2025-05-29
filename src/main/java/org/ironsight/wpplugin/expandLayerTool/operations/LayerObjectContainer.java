package org.ironsight.wpplugin.expandLayerTool.operations;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.Annotations;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.LayerManager;
import org.pepsoft.worldpainter.selection.SelectionBlock;
import org.pepsoft.worldpainter.selection.SelectionChunk;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class LayerObjectContainer {
    private static LayerObjectContainer instance = new LayerObjectContainer();
    private Dimension dimension;
    private LayerManager wpLayerManager;
    List<Layer> layers = new ArrayList<>();
    public LayerObjectContainer() {
        instance = this;
    }

    public static LayerObjectContainer getInstance() {
        return instance;
    }

    public Layer queryLayer(String layerId) {
        assert layers != null;
        for (Layer layer : layers) {
            if (layer.getId().equals(layerId)) {
                return layer;
            }
        }
        return null;
    }

    private void updateLayerList() {
        layers.clear();
        if (wpLayerManager != null) layers.addAll(wpLayerManager.getLayers());
        if (dimension != null) layers.addAll(dimension.getCustomLayers());
        layers.add(Annotations.INSTANCE);
        layers.add(SelectionChunk.INSTANCE);
        layers.add(SelectionBlock.INSTANCE);
    }

    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
        updateLayerList();
    }

    public void setWpLayerManager(LayerManager wpLayerManager) {
        this.wpLayerManager = wpLayerManager;
    }

    public void writeToFolder(String folder, Consumer<IOException> onError, String... layerId) {
        for (String id : layerId) {
            Layer layer = queryLayer(id);
            if (layer == null) {
                onError.accept(new IOException("Layer " + id + " not found, can not export to file."));
                continue;
            }
            if (!(layer instanceof CustomLayer))
                continue;   //no need to write out default layers, like annotations or frost.
            File layerSaveFile = new File(folder + layer.getName() + "_" + layer.getId() + ".layer");
            try (ObjectOutputStream out =
                         new ObjectOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(
                    layerSaveFile))))) {
                out.writeObject(layer);
                System.out.println("saved layer " + layer.getId() + " to " + layerSaveFile.getAbsolutePath());
            } catch (IOException e) {
                onError.accept(e);
            }
        }
    }
}
