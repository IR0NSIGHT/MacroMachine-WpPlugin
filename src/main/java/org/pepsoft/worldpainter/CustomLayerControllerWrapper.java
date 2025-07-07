package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class CustomLayerControllerWrapper {
    private CustomLayerController controller;

    /**
     * ONLY instantiate this if you KNOW for a fact an App instance exists.
     */
    public CustomLayerControllerWrapper() {
        this.controller = getCustomLayerController();
        assert controller != null;
    }

    public void registerCustomLayer(final CustomLayer layer, boolean activate) {
        try {
            Method method = controller.getClass().getDeclaredMethod("registerCustomLayer", CustomLayer.class, boolean.class);
            method.setAccessible(true);
            method.invoke(controller, layer, activate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to call registerCustomLayer via reflection", e);
        }
    }

    public boolean containsLayer(Layer layer) {
        return new HashSet<>(getCustomLayers().stream().map(Layer::getId).collect(Collectors.toList())).contains(layer.getId());
    }

    public List<CustomLayer> getCustomLayers() {
        try {
            Method m = CustomLayerController.class.getDeclaredMethod("getCustomLayers");
            m.setAccessible(true);
            List<CustomLayer> layers = (List<CustomLayer>) m.invoke(controller);
            return layers;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private CustomLayerController getCustomLayerController() {
        // private final CustomLayerController customLayerController
        // in org.pepsoft.worldpainter.App.java
        try {
            // Create an instance of the App class

           // Class<?> appClass = Class.forName("org.pepsoft.worldpainter.App");
            Object appInstance = App.getInstanceIfExists();

            // Get the declared field from the App class
            Field customLayerControllerField = appInstance.getClass().getDeclaredField("customLayerController");
            // Make the field accessible
            customLayerControllerField.setAccessible(true);

            // Get the value of the field from the instance
            return (CustomLayerController) customLayerControllerField.get(appInstance);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
