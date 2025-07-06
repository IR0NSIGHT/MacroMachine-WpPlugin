package org.pepsoft.worldpainter;

import org.pepsoft.worldpainter.layers.CustomLayer;

import java.lang.reflect.Field;
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
        return controller.getCustomLayers();
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
