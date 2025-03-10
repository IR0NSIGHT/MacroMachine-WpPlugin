package org.ironsight.wpplugin.expandLayerTool.Gui;

import java.util.function.Consumer;

public interface IMappingPointSelector {
    void setOnSelect(Consumer<Integer[]> onSelect);

    void setSelectedInputs(Integer[] selectedPointIdx);
}
