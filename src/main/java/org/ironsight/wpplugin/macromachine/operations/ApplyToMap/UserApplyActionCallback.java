package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class UserApplyActionCallback implements ApplyActionCallback {
    int actionIdx = 0;
    private final boolean isDebug;
    private DebugUserInterface debugUI;
    private List<MappingAction> steps;

    public UserApplyActionCallback(DebugUserInterface debuggerUI, boolean isDebug) {
        this.isDebug = isDebug;
        this.debugUI = debuggerUI;
    }

    @Override
    public void setProgressOfAction(int percent) {
       String update = String.format("%d/%d - %d%%",this.actionIdx,steps.size(), percent);
       System.out.println(update);
    }

    @Override
    public boolean isActionAbort() {
        System.out.println("Test is abort:" + debugUI.isAbort());
        return debugUI.isAbort();
    }

    @Override
    public void beforeEachAction(MappingAction a, Dimension dimension) {
        System.out.println("BEFORE EACH ACTION: " + a.getName());

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
        System.out.println("AFTER EACH TILE : " + tileX + "," + tileY);
    }

    @Override
    public void afterEachAction(ExecutionStatistic statistic) {
        System.out.println("AFTER EACH ACTION : " + statistic.toString());
        GlobalActionPanel.logMessage(statistic.toString());
        this.actionIdx++;
    }

    @Override
    public boolean isUpdateMapAfterEachAction() {
        return isDebug;
    }

    @Override
    public void setAllActionsBeforeRun(List<MappingAction> steps) {
        System.out.println("SET STEPS BEFORE RUN ACTION : " + steps.toString());

        this.steps = steps;
        this.actionIdx = 0;
        debugUI.SetBreakpoints(new ArrayList<>(steps));
    }

    @Override
    public void afterEverything() {
        System.out.println("AFTER EVERYTHING : ");
        debugUI.afterEverything();
    }
}
