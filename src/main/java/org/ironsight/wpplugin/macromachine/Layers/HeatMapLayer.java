package org.ironsight.wpplugin.macromachine.Layers;

import org.ironsight.wpplugin.macromachine.MacroSelectionLayerRenderer;
import org.pepsoft.util.IconUtils;
import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;

import java.awt.image.BufferedImage;

public class HeatMapLayer extends Layer {

    @Override
    public LayerRenderer getRenderer() {
        return HeatMapLayerRenderer.instance;
    }
    //fixed UID from class on first release. do not change or all world files will be broken/unloadable
    private static final long serialVersionUID = 1L;
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

    private static final String id = "org.ironsight.wpplugin.macropainter.heatmaplayer";
    private static final String name = "Heatmap";
    private static final String description = "";

    public static HeatMapLayer INSTANCE = new HeatMapLayer();

    protected HeatMapLayer() {
        super(id, name, description, DataSize.NIBBLE, false, 0);
        this.icon = IconUtils.loadScaledImage(this.getClass().getClassLoader(),
                "org/pepsoft/worldpainter/icons/macroselectionlayer.png");

    }
}
