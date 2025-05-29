package org.ironsight.wpplugin.macromachine.Gui;

import java.util.function.Consumer;

public interface IMappingPointSelector {
    void setOnSelect(Consumer<boolean[]> onSelect);

    void setSelectedInputs(boolean[] selectedPointIdx);
}
