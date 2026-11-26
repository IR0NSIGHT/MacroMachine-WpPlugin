package org.ironsight.wpplugin.macromachine.Layers.CityBuilder;

import java.awt.event.ActionEvent;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.zip.GZIPInputStream;
import javax.swing.Action;

import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.layers.CustomLayer;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.painting.PaintFactory;

/**
 * The City Tool's "Layer" menu talks to WorldPainter through here: which City
 * Layers the dimension has, selecting one as the paint, WorldPainter's own
 * layer import dialog and the bundled tutorial layer. WorldPainter keeps the
 * import action and the custom layer controller private, so those are reached
 * by reflection; every call degrades to doing nothing (or to the fallback) when
 * WorldPainter's GUI is not running, as in tests.
 */
final class CityLayerLibrary
{
    /**
     * The tutorial layer: a WorldPainter layer file (gzipped serialized layer, as
     * WorldPainter exports it), bundled with the plugin.
     */
    static final String TUTORIAL_LAYER_RESOURCE = "/CityBuilder/tutorial_houses.layer";
    /** The name the tutorial layer gets in the world. */
    static final String TUTORIAL_LAYER_NAME = "tutorial_houses";

    private CityLayerLibrary() {
    }

    /**
     * WorldPainter's main window, or {@code null} without its GUI. Reached by
     * reflection: App's signature drags in JIDE, which is not on our compile
     * classpath.
     */
    private static Object app() {
        try {
            Class<?> type = Class.forName("org.pepsoft.worldpainter.App");
            return type.getMethod("getInstanceIfExists").invoke(null);
        } catch (ReflectiveOperationException | LinkageError ex) {
            return null;
        }
    }

    private static Object privateField(Object owner, String name) throws ReflectiveOperationException {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    /** City Layers of {@code dimension}, sorted by name (ignoring case). */
    static List<CityLayer> cityLayers(Dimension dimension) {
        if (dimension == null)
            return List.of();
        var layers = new ArrayList<CityLayer>();
        for (Layer layer : customLayers(dimension))
            if (layer instanceof CityLayer city)
                layers.add(city);
        layers.sort(Comparator.comparing((CityLayer layer) -> layer.getName().toLowerCase(Locale.ROOT))
                .thenComparing(Layer::getName));
        return layers;
    }

    /**
     * Makes {@code layer} WorldPainter's selected paint, which hands it to the
     * tool; without WorldPainter's GUI, {@code fallback} gets the layer's paint.
     */
    static void select(CityLayer layer, Consumer<org.pepsoft.worldpainter.painting.Paint> fallback) {
        Object app = app();
        if (app != null) {
            try {
                app.getClass()
                        .getMethod("selectPaint", String.class)
                        .invoke(app, PaintFactory.createLayerPaintId(layer));
                return;
            } catch (ReflectiveOperationException ex) {
                // fall back to handing the paint to the tool directly
            }
        }
        fallback.accept(PaintFactory.createLayerPaint(layer));
    }

    /**
     * Opens WorldPainter's "Import custom layer(s)" dialog. Returns the City Layer
     * it added (the first, if several), or {@code null}.
     */
    static CityLayer importFromFile(Dimension dimension) throws ReflectiveOperationException {
        Object app = app();
        if (app == null)
            return null;
        var before = cityLayers(dimension);
        ((Action) privateField(app, "ACTION_IMPORT_LAYER"))
                .actionPerformed(new ActionEvent(app, ActionEvent.ACTION_PERFORMED, "importLayer"));
        CityLayer imported = null;
        for (CityLayer layer : cityLayers(dimension))
            if (!before.contains(layer)) {
                // its house positions belong to the world the file was saved in
                layer.forgetPlacements();
                if (imported == null)
                    imported = layer;
            }
        return imported;
    }

    /**
     * The custom layers loaded now: WorldPainter's own list while its GUI runs
     * (layers just imported or created are only copied into the dimension when the
     * world is saved), else the dimension's.
     */
    @SuppressWarnings("unchecked")
    private static List<? extends Layer> customLayers(Dimension dimension) {
        Object app = app();
        if (app != null) {
            try {
                return (List<? extends Layer>) app.getClass().getMethod("getCustomLayers").invoke(app);
            } catch (ReflectiveOperationException | ClassCastException ex) {
                // fall back to the dimension's list
            }
        }
        return dimension.getCustomLayers();
    }

    /**
     * Whether {@code layer} is one of the open world's custom layers (the very same
     * object). Without WorldPainter's GUI (tests) every layer counts.
     */
    static boolean inWorld(CityLayer layer, Dimension dimension) {
        if (app() == null || dimension == null)
            return true;
        for (Layer custom : customLayers(dimension))
            if (custom == layer)
                return true;
        return false;
    }

    /** Whether the plugin bundles the tutorial layer. */
    static boolean hasTutorialLayer() {
        return CityLayerLibrary.class.getResource(TUTORIAL_LAYER_RESOURCE) != null;
    }

    /** Reads the bundled tutorial layer (a new copy on every call). */
    static CityLayer readTutorialLayer() throws IOException, ClassNotFoundException {
        URL url = CityLayerLibrary.class.getResource(TUTORIAL_LAYER_RESOURCE);
        if (url == null)
            throw new IOException("The tutorial layer is not bundled: " + TUTORIAL_LAYER_RESOURCE);
        try (InputStream stream = url.openStream();
                var in = new ObjectInputStream(new GZIPInputStream(new BufferedInputStream(stream)))) {
            if (in.readObject() instanceof CityLayer layer) {
                layer.setName(TUTORIAL_LAYER_NAME);
                // the file was saved from a world with houses: only its schematics come along
                layer.forgetPlacements();
                return layer;
            }
            throw new IOException("The tutorial layer file holds no City Layer");
        }
    }

    /**
     * The tutorial layer in {@code dimension}: the one already there (same name),
     * or the bundled one, added the way WorldPainter imports a layer file.
     */
    static CityLayer addTutorialLayer(Dimension dimension) throws IOException, ReflectiveOperationException {
        CityLayer tutorial = readTutorialLayer();
        // pressed before: never a second copy. The same layer id (it survives renaming)
        // or the same name is the tutorial layer already in the world.
        CityLayer present = presentTutorialLayer(dimension, tutorial);
        if (present != null)
            return present;
        Object app = app();
        if (app == null)
            return null;
        Object controller = privateField(app, "customLayerController");
        Method importLayer = controller.getClass().getDeclaredMethod("importCustomLayer", CustomLayer.class);
        importLayer.setAccessible(true);
        importLayer.invoke(controller, tutorial);
        if (presentTutorialLayer(dimension, tutorial) == null)
            throw new IOException("WorldPainter did not add the tutorial layer to this dimension");
        return tutorial;
    }

    /**
     * The tutorial layer if the dimension has it already (same layer id or name),
     * else {@code null}.
     */
    static CityLayer presentTutorialLayer(Dimension dimension, CityLayer tutorial) {
        for (CityLayer layer : cityLayers(dimension))
            if (layer.getId().equals(tutorial.getId()))
                return layer;
        for (CityLayer layer : cityLayers(dimension))
            if (layer.getName().equals(tutorial.getName()))
                return layer;
        return null;
    }
}
