package org.ironsight.wpplugin.macromachine;

import org.ironsight.wpplugin.macromachine.Layers.HeatMapLayer;
import org.ironsight.wpplugin.macromachine.operations.MacroDialogOperation;
import org.ironsight.wpplugin.macromachine.operations.PreviewOperation;
import org.pepsoft.worldpainter.WPContext;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.operations.Operation;
import org.pepsoft.worldpainter.plugins.AbstractPlugin;
import org.pepsoft.worldpainter.plugins.LayerProvider;
import org.pepsoft.worldpainter.plugins.OperationProvider;
import org.pepsoft.worldpainter.plugins.WPPluginManager;

import java.util.ArrayList;
import java.util.List;

import static org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel.ErrorPopUpString;
import static org.ironsight.wpplugin.macromachine.Version.VERSION;

/**
 * The main plugin class. This demo combines the various providers in one plugin class. You could of course also
 * separate them out into separate plugins classes for clarity. And of course you can leave out any providers for
 * services your plugin does not provide.
 *
 * <p><strong>Note:</strong> this class is referred to from the {@code org.pepsoft.worldpainter.plugins} file, so when
 * you rename or copy it, be sure to keep that file up-to-date.
 */
@SuppressWarnings("unused") // Instantiated by WorldPainter
public class MacroMachinePlugin extends AbstractPlugin implements
        // This demo has the plugin class implementing all of these, but they may also be implemented by separate
        // classes, as long as each class implements Plugin and is mentioned in the org.pepsoft.worldpainter.plugins
        // registry file
        LayerProvider,          // Implement this to provide one or more singular, unconfigurable layers
        OperationProvider      // Implement this to provide one or more custom operations for the Tools panel
{
    public WorldPainterView view;
    @Override
    public void init(WPContext context) {
        super.init(context);
    }

    public static MacroMachinePlugin getInstance() {
        if (instance == null)
            instance = new MacroMachinePlugin();
        return instance;
    }
    private static MacroMachinePlugin instance;
    /**
     * Short, human-readble name of the plugin.
     */
    public static final String NAME = "Macro Machine";

    // LayerProvider
    private List<Layer> LAYERS;

    // OperationProvider
    private List<Operation> OPERATIONS;

    public static void error(String mssg) {
        if (logger != null)
            logger.error("MacroMachine:" + mssg);
        else
            System.err.println(mssg);
    }

    private static org.slf4j.Logger logger;

    /**
     * The plugin class must have a default (public, no arguments) constructor.
     */
    public MacroMachinePlugin() {
        super(NAME, VERSION);
        instance = this;
        logger = org.slf4j.LoggerFactory.getLogger(WPPluginManager.class);
    }

    @Override
    public List<Layer> getLayers() {
        if (LAYERS == null) {
            LAYERS = new ArrayList<>();
            LAYERS.add(MacroSelectionLayer.INSTANCE);
            LAYERS.add(HeatMapLayer.INSTANCE);
            LAYERS.add(PreviewOperation.annotationLayer);
        }
        return LAYERS;
    }

    @Override
    public List<Operation> getOperations() {
        if (OPERATIONS == null) {
            OPERATIONS = new ArrayList<>();
            OPERATIONS.add(new MacroDialogOperation());
            OPERATIONS.add(new PreviewOperation());
        }
        return OPERATIONS;
    }
}
