package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.util.function.Consumer;

public abstract class LayerMappingPanel extends JPanel {
    protected LayerMapping mapping;
    private Consumer<LayerMapping> onUpdate = f -> {
    };

    public LayerMappingPanel() {
        initComponents();
    }

    protected abstract void updateComponents();

    protected abstract void initComponents();

    /**
     * will set mapping, call onUpdate and trigger internal update()
     * @param mapping
     */
    protected final void updateMapping(LayerMapping mapping) {
        if (this.mapping != null && this.mapping.equals(mapping)) {
            return;
        }
        setMapping(mapping);
        if (onUpdate != null) onUpdate.accept(mapping);
    }

    public final void setMapping(LayerMapping mapping) {
        assert mapping != null;
        if (this.mapping != null && this.mapping.equals(mapping)) {
            return;
        }
        this.mapping = mapping;
        updateComponents();
    }

    public final void setOnUpdate(Consumer<LayerMapping> onUpdate) {
        this.onUpdate = onUpdate;
    }
}
