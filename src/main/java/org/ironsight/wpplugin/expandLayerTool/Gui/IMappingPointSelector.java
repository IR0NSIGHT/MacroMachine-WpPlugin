package org.ironsight.wpplugin.expandLayerTool.Gui;

import java.util.function.Consumer;
import java.util.function.IntBinaryOperator;

public interface IMappingPointSelector {
    void setOnSelect(Consumer<boolean[]> onSelect);

    void setSelectedInputs(boolean[] selectedPointIdx);
}
