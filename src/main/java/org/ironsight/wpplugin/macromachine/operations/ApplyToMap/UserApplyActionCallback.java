package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

import java.util.ArrayList;
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
        return debugUI.isAbort();
    }

    @Override
    public void beforeEachAction(MappingAction a, Dimension dimension) {
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
    public void afterEachAction() {
        this.actionIdx++;
    }

    @Override
    public boolean updateMapAfterEachAction() {
        return isDebug;
    }

    @Override
    public void setAllActionsBeforeRun(List<MappingAction> steps) {
        this.steps = steps;
        this.actionIdx = 0;
        debugUI.SetBreakpoints(steps.stream()
                .map(MappingAction::getName)
                .collect(Collectors.toCollection(ArrayList::new)));
    }

    @Override
    public void afterEverything() {
        debugUI.afterEverything();
    }
}
