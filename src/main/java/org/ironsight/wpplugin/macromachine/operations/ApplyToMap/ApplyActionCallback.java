package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

import java.util.List;

public interface ApplyActionCallback {
    void setProgressOfAction(int percent);

    boolean isActionAbort();

    void beforeEachAction(MappingAction action, Dimension dimension);

    void afterEachTile(int tileX, int tileY);

    void afterEachAction(ExecutionStatistic statistic);
    boolean isUpdateMapAfterEachAction();

    void setAllActionsBeforeRun(List<MappingAction> steps);

    void afterEverything();
}
