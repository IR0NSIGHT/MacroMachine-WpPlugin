package org.ironsight.wpplugin.expandLayerTool.operations;

import org.ironsight.wpplugin.expandLayerTool.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.Dimension;
import org.pepsoft.worldpainter.Terrain;
import org.pepsoft.worldpainter.WorldPainterView;
import org.pepsoft.worldpainter.layers.DeciduousForest;
import org.pepsoft.worldpainter.layers.PineForest;
import org.pepsoft.worldpainter.operations.AbstractOperation;

import javax.swing.*;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import static org.ironsight.wpplugin.expandLayerTool.Gui.ActionEditor.createDialog;

public class MacroDialogOperation extends AbstractOperation {
    private static final String NAME = "Macro Operation";
    private static final String DESCRIPTION = "Create complex reusable global operations to automate your workflow.";
    private static final String ID = "macro_dialog_operation";
    private WorldPainterView mWorldPainterView;

    public MacroDialogOperation() {
        super(NAME, DESCRIPTION, "macrooperation");
        MappingMacroContainer.getInstance().readFromFile();
        LayerMappingContainer.INSTANCE.readFromFile();
        LayerMappingContainer.INSTANCE.subscribe(() -> LayerMappingContainer.INSTANCE.writeToFile());
        MappingMacroContainer.getInstance().subscribe(() -> MappingMacroContainer.getInstance().writeToFile());
    }

    public static void ErrorPopUp(String message) {
        JOptionPane.showMessageDialog(null, message, "Error",
                // Title of the dialog
                JOptionPane.ERROR_MESSAGE
                // Type of message (error icon)
        );
    }

    private Dimension getDimension() {
        return mWorldPainterView.getDimension();
    }

    public void openDialog() {
        try {
            InputOutputProvider.INSTANCE.updateFrom(getDimension());
            JDialog dialog = createDialog(null, this::applyLayerAction);
            dialog.toFront();              // Bring it to the front
            dialog.requestFocusInWindow();
            dialog.setVisible(true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void applyLayerAction(MappingMacro macro) {
        try {
            this.getDimension().setEventsInhibited(true);
            LinkedList<List<UUID>> actionIds = new LinkedList<>();
            List<List<UUID>> steps = macro.collectActions(actionIds);
            for (List<UUID> step : steps) {
                for (UUID actionId : step) {
                    LayerMapping action = LayerMappingContainer.INSTANCE.queryById(actionId);
                    if (action == null) {
                        ErrorPopUp("Action " + actionId + " not found.");
                        return;
                    }
                    try {
                        action.output.prepareForDimension(getDimension());
                        action.input.prepareForDimension(getDimension());
                    } catch (IllegalAccessError e) {    //FIXME move this check further up.
                        JOptionPane.showMessageDialog(null,
                                "Action " + action.getName() + " can not be applied to the map." + e.getMessage(),
                                "Error",
                                // Title of the dialog
                                JOptionPane.ERROR_MESSAGE
                                // Type of message (error icon)
                        );
                        return;
                    }
                }
            }

            // ----------------------- macro is ready and can be applied to map

            System.out.println("Execution order");
            actionIds.forEach(step -> {
                StringBuilder b = new StringBuilder("Step:\n");
                String[] names = step.stream()
                        .map(LayerMappingContainer.INSTANCE::queryById)
                        .map(LayerMapping::getName)
                        .map(f -> "\t" + f)
                        .toArray(String[]::new);
                b.append(String.join("\n", names));
                System.out.println(b);
            });

            macro.setTileFilter(new TileFilter().withSelection(TileFilter.FilterType.EXCEPT_ON)
                    .withLayer(TileFilter.FilterType.ONLY_ON,
                            PineForest.INSTANCE.getId(),
                            DeciduousForest.INSTANCE.getId())
                    .withTerrain(TileFilter.FilterType.ONLY_ON, Terrain.GRASS, Terrain.STONE)
                    .withHeight(TileFilter.FilterType.EXCEPT_ON, 70, 73));

            macro.apply(getDimension(), LayerMappingContainer.INSTANCE, MappingMacroContainer.getInstance());
        } catch (Exception ex) {
            System.out.println(ex);
        } finally {
            this.getDimension().setEventsInhibited(false);
        }
    }

    @Override
    protected void activate() {
        openDialog();
    }

    @Override
    protected void deactivate() {

    }


    @Override
    public void setView(WorldPainterView view) {
        this.mWorldPainterView = view;
    }

    @Override
    public void interrupt() {

    }
}
