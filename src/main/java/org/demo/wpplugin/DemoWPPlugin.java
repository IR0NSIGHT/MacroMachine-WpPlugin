package org.demo.wpplugin;

import org.demo.wpplugin.layers.PathPreviewLayer;
import org.demo.wpplugin.operations.EditPath.EditPathOperation;
import org.demo.wpplugin.operations.ApplyPath.ApplyPathOperation;
import org.demo.wpplugin.operations.FlattenPathOperation;
import org.demo.wpplugin.operations.LinearByAngleOperation;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.OperationProvider;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.demo.wpplugin.Version.VERSION;

/**
 * The main plugin class. This demo combines the various providers in one plugin class. You could of course also
 * separate them out into separate plugins classes for clarity. And of course you can leave out any providers for
 * services your plugin does not provide.
 *
 * <p><strong>Note:</strong> this class is referred to from the {@code org.pepsoft.worldpainter.plugins} file, so when
 * you rename or copy it, be sure to keep that file up-to-date.
 */
@SuppressWarnings("unused") // Instantiated by WorldPainter
public class DemoWPPlugin extends AbstractPlugin implements
        // This demo has the plugin class implementing all of these, but they may also be implemented by separate
        // classes, as long as each class implements Plugin and is mentioned in the org.pepsoft.worldpainter.plugins
        // registry file
        LayerProvider,          // Implement this to provide one or more singular, unconfigurable layers
        OperationProvider      // Implement this to provide one or more custom operations for the Tools panel
{
    /**
     * Short, human-readble name of the plugin.
     */
    static final String NAME = "Demo WP Plugin";

    // LayerProvider
    private static final List<Layer> LAYERS = singletonList(PathPreviewLayer.INSTANCE);

    // OperationProvider
    private static final List<Operation> OPERATIONS = Arrays.asList(new EditPathOperation(), new ApplyPathOperation());

    /**
     * The plugin class must have a default (public, no arguments) constructor.
     */
    public DemoWPPlugin() {
        super(NAME, VERSION);
    }

    /**
     * Get the list of custom {@link Layer}s provided by this plugin.
     */
    @Override
    public List<Layer> getLayers() {
        return LAYERS;
    }

    @Override
    public List<Operation> getOperations() {
        return OPERATIONS;
    }
}
