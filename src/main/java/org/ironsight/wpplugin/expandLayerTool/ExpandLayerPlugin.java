package org.ironsight.wpplugin.expandLayerTool;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMappingContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.MappingMacroContainer;
import org.ironsight.wpplugin.expandLayerTool.operations.SelectEdgeOperation;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.OperationProvider;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.ironsight.wpplugin.expandLayerTool.Version.VERSION;

/**
 * The main plugin class. This demo combines the various providers in one plugin class. You could of course also
 * separate them out into separate plugins classes for clarity. And of course you can leave out any providers for
 * services your plugin does not provide.
 *
 * <p><strong>Note:</strong> this class is referred to from the {@code org.pepsoft.worldpainter.plugins} file, so when
 * you rename or copy it, be sure to keep that file up-to-date.
 */
@SuppressWarnings("unused") // Instantiated by WorldPainter
public class ExpandLayerPlugin extends AbstractPlugin implements
        // This demo has the plugin class implementing all of these, but they may also be implemented by separate
        // classes, as long as each class implements Plugin and is mentioned in the org.pepsoft.worldpainter.plugins
        // registry file
        OperationProvider      // Implement this to provide one or more custom operations for the Tools panel
{
    /**
     * Short, human-readble name of the plugin.
     */
    static final String NAME = "Expand Layer Tool Plugin";

    // LayerProvider
    private static final List<Layer> LAYERS = new ArrayList<>();

    // OperationProvider
    private static final List<Operation> OPERATIONS = singletonList(new SelectEdgeOperation());

    /**
     * The plugin class must have a default (public, no arguments) constructor.
     */
    public ExpandLayerPlugin() {
        super(NAME, VERSION);
        LayerMappingContainer.INSTANCE.subscribe(() -> LayerMappingContainer.INSTANCE.writeToFile());
        MappingMacroContainer.getInstance().subscribe(() -> MappingMacroContainer.getInstance().writeToFile());
    }

    @Override
    public List<Operation> getOperations() {
        return OPERATIONS;
    }
}
