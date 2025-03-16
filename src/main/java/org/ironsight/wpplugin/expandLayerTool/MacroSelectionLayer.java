package org.ironsight.wpplugin.expandLayerTool;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

public class MacroSelectionLayer extends Layer {
    @Override
    public LayerRenderer getRenderer() {
        return MacroSelectionLayerRenderer.instance;
    }

    private static final String id = "irn-SelectionBitLayer";
    private static final String name = "Macro Selection";
    private static final String description = "a bit layer to select blocks for usage in macros.";

    public static MacroSelectionLayer INSTANCE = new MacroSelectionLayer();

    protected MacroSelectionLayer() {
        super(id, name, description, DataSize.BIT, false, 0);
    }
}
