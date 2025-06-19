package org.ironsight.wpplugin.macromachine;

import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

import java.awt.image.BufferedImage;

public class MacroSelectionLayer extends Layer {

    @Override
    public LayerRenderer getRenderer() {
        return MacroSelectionLayerRenderer.instance;
    }
    //fixed UID from class on first release. do not change or all world files will be broken/unloadable
    private static final long serialVersionUID = -6448177550160813133L;
    private transient final BufferedImage icon;
    @Override
    public BufferedImage getIcon() {
        return icon;
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
        this.icon = IconUtils.loadScaledImage(this.getClass().getClassLoader(),
                "org/pepsoft/worldpainter/icons/macroselectionlayer.png");

    }
}
