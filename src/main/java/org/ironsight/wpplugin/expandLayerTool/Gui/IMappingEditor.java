package org.ironsight.wpplugin.expandLayerTool.Gui;

import org.ironsight.wpplugin.expandLayerTool.operations.LayerMapping;

import java.util.function.Consumer;

public interface IMappingEditor {
    void setMapping(LayerMapping mapping);

    void setOnUpdate(Consumer<LayerMapping> onUpdate);

    void setOnSelect(Consumer<Integer> onSelect);

    void setSelected(Integer selectedPointIdx);
}
