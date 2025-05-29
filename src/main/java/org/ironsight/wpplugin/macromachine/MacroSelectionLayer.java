package org.ironsight.wpplugin.macromachine;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

public class MacroSelectionLayer extends Layer {
    @Override
    public LayerRenderer getRenderer() {
        return MacroSelectionLayerRenderer.instance;
    }

    /**
     * the purpose of this layer, is to provide a BIT layer the user can paint for selection of areas
     * the distanceToEdge function provided by worldpainter can only search inside of bit layers, and selection is
     * split into SelectionBlock and SelecctionChunk which messes up the result.
     * so this is a single-layer-paintable-selection substitute.
     */

    private static final String id = "org.ironsight.wpplugin.macropainter.macroselectionlayer";
    private static final String name = "Macro Selection";
    private static final String description = "a simple bit layer to make selection easier";

    public static MacroSelectionLayer INSTANCE = new MacroSelectionLayer();

    protected MacroSelectionLayer() {
        super(id, name, description, DataSize.BIT, false, 0);
    }
}
