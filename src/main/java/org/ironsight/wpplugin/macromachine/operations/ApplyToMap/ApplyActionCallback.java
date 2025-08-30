package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

import java.util.List;

/**
 * callback to inform GUI of the progress for executing a macro / applying an action to a dimension
 */
public interface ApplyActionCallback {
    void setProgressOfAction(int percent);

    boolean isActionAbort();

    void beforeEachAction(MappingAction action, Dimension dimension);

    void afterEachTile(int tileX, int tileY);

    void afterEachAction(ExecutionStatistic statistic);

    /**
     * allow UI events to be genereated after the action is complete? FALSE: wait until everything is done
     * @return
     */
    boolean isUpdateMapAfterEachAction();

    void setAllActionsBeforeRun(List<MappingAction> steps);

    void afterEverything();
}
