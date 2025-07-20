package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import java.util.ArrayList;

public interface BreakpointListener {
    void OnReachedBreakpoint(int idx);

    void PostReachedBreakpoint(int idx);

    void SetBreakpoints(ArrayList<MappingAction> breakpoints);

    void afterEverything();
}
