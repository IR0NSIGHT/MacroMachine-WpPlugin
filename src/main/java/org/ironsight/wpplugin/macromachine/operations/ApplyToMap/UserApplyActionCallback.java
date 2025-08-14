package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserApplyActionCallback implements ApplyActionCallback {
    private final boolean isDebug;
    int actionIdx = 0;
    int totalActions = 0;
    private DebugUserInterface debugUI;
    private List<MappingAction> steps;

    public UserApplyActionCallback(DebugUserInterface debuggerUI, boolean isDebug) {
        this.isDebug = isDebug;
        this.debugUI = debuggerUI;
    }


    @Override
    public void setProgressOfAction(int percent) {
        debugUI.setProgessTo(actionIdx, percent, totalActions);
    }

    @Override
    public boolean isActionAbort() {
        return debugUI.isAbort();
    }

    @Override
    public void beforeEachAction(MappingAction a, Dimension dimension) {
        debugUI.setProgessTo(actionIdx, 0, totalActions);

        debugUI.OnReachedBreakpoint(actionIdx);
        DebugUserInterface.BreakpointReaction breakpointReaction = DebugUserInterface.BreakpointReaction.WAIT;
        if (isDebug) {
            if (dimension != null)
                dimension.rememberChanges();
            try {
                while (!isActionAbort() && breakpointReaction == DebugUserInterface.BreakpointReaction.WAIT) {
                    breakpointReaction = debugUI.CheckBreakpointStatus(actionIdx, a);
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        debugUI.PostReachedBreakpoint(actionIdx);
    }

    @Override
    public void afterEachTile(int tileX, int tileY) {
    }

    @Override
    public void afterEachAction(ExecutionStatistic statistic) {
        debugUI.setProgessTo(actionIdx, 100, totalActions);

        if (statistic != null) {
            GlobalActionPanel.logMessage(statistic.toString());
        }
        this.actionIdx++;
    }

    @Override
    public boolean isUpdateMapAfterEachAction() {
        return isDebug;
    }

    @Override
    public void setAllActionsBeforeRun(List<MappingAction> steps) {
        this.steps = steps;
        this.actionIdx = 0;
        debugUI.SetBreakpoints(new ArrayList<>(steps));
        totalActions = steps.size();
        debugUI.setProgessTo(0, 0, steps.size());
    }

    @Override
    public void afterEverything() {
        debugUI.afterEverything();
    }
}
