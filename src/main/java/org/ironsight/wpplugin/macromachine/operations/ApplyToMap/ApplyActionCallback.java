package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import java.util.List;
import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

/**
 * callback to inform GUI of the progress for executing a macro / applying an
 * action to a dimension
 */
public interface ApplyActionCallback
{
    void afterPreparation();
    void onError(int stepIdx, MappingAction action, String error);
    void setProgressOfAction(int percent, MappingAction action);

    boolean isActionAbort();

    void beforeEachAction(MappingAction action, Dimension dimension);

    void afterEachTile(int tileX, int tileY);

    void afterEachAction(ExecutionStatistic statistic, MappingAction action);

    /**
     * allow UI events to be genereated after the action is complete? FALSE: wait
     * until everything is done
     *
     * @return
     */
    boolean isUpdateMapAfterEachAction();

    void setAllActionsBeforeRun(List<MappingAction> steps);

    void afterEverything();
}
