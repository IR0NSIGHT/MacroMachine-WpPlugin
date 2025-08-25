package org.ironsight.wpplugin.macromachine.operations;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.PaintRenderer;

import java.awt.image.BufferedImage;
import java.io.Serial;

import static java.awt.Color.YELLOW;

class AnnotationLayer extends Layer {

    private transient LayerRenderer renderer;

    protected AnnotationLayer(String id, String name, String description, DataSize dataSize, boolean discrete,
                              int priority,
                              char mnemonic) {
        super(id, name, description, dataSize, discrete, priority, mnemonic);
    }

    @Override
    public LayerRenderer getRenderer() {
        if (renderer == null)
            this.renderer =  new PaintRenderer(YELLOW, .5f);
        return renderer;
    }

    private static final BufferedImage icon = new BufferedImage(16,16,BufferedImage.TYPE_INT_RGB);
    @Override
    public BufferedImage getIcon() {
        return icon;
    }

    @Serial
    private static final long serialVersionUID = 1L;

}
