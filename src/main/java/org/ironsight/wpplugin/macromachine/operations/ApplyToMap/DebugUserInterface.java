package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import java.util.ArrayList;

public interface DebugUserInterface {
    enum BreakpointReaction {
        WAIT,
        CONTINUE,
        ABORT,
        RESTART,
        SKIP,
        STEP_OUT
    }

    BreakpointReaction CheckBreakpointStatus(int index, MappingAction action);
    void OnReachedBreakpoint(int idx);
    void PostReachedBreakpoint(int idx);
    void SetBreakpoints(ArrayList<String> breakpoints);
    boolean isAbort();
    void afterEverything();
}
