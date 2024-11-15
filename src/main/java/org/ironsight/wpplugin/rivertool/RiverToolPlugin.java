package org.ironsight.wpplugin.rivertool;

import org.ironsight.wpplugin.rivertool.layers.PathPreviewLayer;
import org.ironsight.wpplugin.rivertool.operations.EditPath.EditPathOperation;
import org.ironsight.wpplugin.rivertool.operations.ApplyPath.ApplyRiverOperation;
import org.ironsight.wpplugin.rivertool.operations.SelectEdgeOperation;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.OperationProvider;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.ironsight.wpplugin.rivertool.Version.VERSION;

/**
 * The main plugin class. This demo combines the various providers in one plugin class. You could of course also
 * separate them out into separate plugins classes for clarity. And of course you can leave out any providers for
 * services your plugin does not provide.
 *
 * <p><strong>Note:</strong> this class is referred to from the {@code org.pepsoft.worldpainter.plugins} file, so when
 * you rename or copy it, be sure to keep that file up-to-date.
 */
@SuppressWarnings("unused") // Instantiated by WorldPainter
public class RiverToolPlugin extends AbstractPlugin implements
        // This demo has the plugin class implementing all of these, but they may also be implemented by separate
        // classes, as long as each class implements Plugin and is mentioned in the org.pepsoft.worldpainter.plugins
        // registry file
        LayerProvider,          // Implement this to provide one or more singular, unconfigurable layers
        OperationProvider      // Implement this to provide one or more custom operations for the Tools panel
{
    /**
     * Short, human-readble name of the plugin.
     */
    static final String NAME = "River Tool Plugin";

    // LayerProvider
    private static final List<Layer> LAYERS = singletonList(PathPreviewLayer.INSTANCE);

    // OperationProvider
    private static final List<Operation> OPERATIONS = Arrays.asList(new EditPathOperation(), new ApplyRiverOperation(), new SelectEdgeOperation());

    /**
     * The plugin class must have a default (public, no arguments) constructor.
     */
    public RiverToolPlugin() {
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
