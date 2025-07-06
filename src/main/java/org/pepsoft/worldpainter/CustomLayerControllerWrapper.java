package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.CustomLayer;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class CustomLayerControllerWrapper {
    private CustomLayerController controller;

    /**
     * ONLY instantiate this if you KNOW for a fact an App instance exists.
     */
    public CustomLayerControllerWrapper() {
        this.controller = getCustomLayerController();
        assert controller != null;
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
