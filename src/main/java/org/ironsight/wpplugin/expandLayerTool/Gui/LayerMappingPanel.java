package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import javax.swing.*;
import java.util.function.Consumer;

public abstract class LayerMappingPanel extends JPanel {
    protected LayerMapping mapping;
    private boolean allowEvents = true;
    private Consumer<LayerMapping> onUpdate = f -> {
    };

    public LayerMappingPanel() {
        allowEvents = false;
        initComponents();
        allowEvents = true;
    }

    public boolean isAllowEvents() {
        return allowEvents;
    }

    protected abstract void updateComponents();

    protected abstract void initComponents();

    /**
     * internal way to signalize "i changed the mapping, do the events" will set mapping, call onUpdate and trigger
     * internal update()
     *
     * @param mapping
     */
    protected final void updateMapping(LayerMapping mapping) {
        if (mapping == null || this.mapping == null || this.mapping.equals(mapping) || !allowEvents) {
            return;
        }
        System.out.println("EVENT: " + this.getClass().getSimpleName() + " UPDATE MAPPING TO " + mapping);

        setMapping(mapping);
        if (onUpdate != null) onUpdate.accept(mapping);
    }

    public final void setMapping(LayerMapping mapping) {
        assert mapping != null;
        assert mapping.getMappingPoints() != null;
        assert mapping.input != null;
        assert mapping.output != null;
        if (this.mapping != null && this.mapping.equals(mapping)) {
            return;
        }
        System.out.println("EVENT: " + this.getClass().getSimpleName() + " SET MAPPING TO " + mapping);
        allowEvents = false;
        this.mapping = mapping;
        updateComponents();
        this.revalidate();
        this.repaint();
        allowEvents = true;
    }

    public final void setOnUpdate(Consumer<LayerMapping> onUpdate) {
        this.onUpdate = onUpdate;
    }
}
