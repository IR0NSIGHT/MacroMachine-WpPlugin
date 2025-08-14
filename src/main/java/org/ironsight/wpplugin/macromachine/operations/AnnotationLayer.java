package org.ironsight.wpplugin.macromachine.operations;

import org.pepsoft.worldpainter.layers.Layer;
import org.pepsoft.worldpainter.layers.renderers.LayerRenderer;
import org.pepsoft.worldpainter.layers.renderers.PaintRenderer;

import static java.awt.Color.YELLOW;

class AnnotationLayer extends Layer {

    private final transient LayerRenderer renderer = new PaintRenderer(YELLOW, .5f);

    protected AnnotationLayer(String id, String name, String description, DataSize dataSize, boolean discrete,
                              int priority,
                              char mnemonic) {
        super(id, name, description, dataSize, discrete, priority, mnemonic);
    }

    @Override
    public LayerRenderer getRenderer() {
        return renderer;
    }
}
