package org.ironsight.wpplugin.macromachine.Layers;

import org.ironsight.wpplugin.macromachine.operations.ValueProviders.LayerProvider;
import org.pepsoft.worldpainter.CustomLayerController;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

/**
 * API to worldpainters CustomLayerController, that manages all custom layers
 * use to get proper list of customlayers avaialble in WP
 * use to register new custom layers to WP
 */
public class CustomLayerControllerWrapper implements LayerProvider {
    private CustomLayerController controller;

    public CustomLayerControllerWrapper() {

    }

    private CustomLayerController getController() {
        if (controller == null) this.controller = getCustomLayerController();
        return controller;
    }

    public List<CustomLayer> getCustomLayers() {
        try {
            Method m = CustomLayerController.class.getDeclaredMethod("getCustomLayers");
            m.setAccessible(true);
            List<CustomLayer> layers = (List<CustomLayer>) m.invoke(getController());
            return layers;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
            System.err.println(e);
            return List.of();
        }
    }

    private CustomLayerController getCustomLayerController() {
        // private final CustomLayerController customLayerController
        // in org.pepsoft.worldpainter.App.java
        try {
            Class<?> appClass = Class.forName("org.pepsoft.worldpainter.App");
            java.lang.reflect.Method method = appClass.getMethod("getInstanceIfExists");
            Object appInstance = method.invoke(null);
            Field customLayerControllerField = appInstance.getClass().getDeclaredField("customLayerController");
            customLayerControllerField.setAccessible(true);
            return (CustomLayerController) customLayerControllerField.get(appInstance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public Layer getLayerById(String layerId, Consumer<String> layerNotFoundError) {
        HashMap<String, CustomLayer> layers = new HashMap<>();
        getCustomLayers().forEach(l -> layers.put(l.getId(), l));
        if (layers.containsKey(layerId)) return layers.get(layerId);
        layerNotFoundError.accept(layerId);
        return null;
    }

    @Override
    public List<Layer> getLayers() {
        return new ArrayList<>(getCustomLayers());
    }

    @Override
    public void addLayer(Layer layer) {
        if (!(layer instanceof CustomLayer customLayer)) return;

        try {
            Method registerMethod = CustomLayerController.class.getDeclaredMethod("registerCustomLayer", CustomLayer.class, boolean.class);
            registerMethod.setAccessible(true); // bypass access checks
            registerMethod.invoke(getController(), customLayer, true);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | NullPointerException e) {
            System.err.println("Failed to register custom layer: " + e);
        }
    }

    @Override
    public boolean existsLayerWithId(String layerId) {
        HashMap<String, CustomLayer> layers = new HashMap<>();
        getCustomLayers().forEach(l -> layers.put(l.getId(), l));
        return (layers.containsKey(layerId));
    }
}
