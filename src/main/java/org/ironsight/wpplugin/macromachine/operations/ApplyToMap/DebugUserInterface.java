package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import org.ironsight.wpplugin.macromachine.operations.MappingAction;

import java.util.ArrayList;

public interface DebugUserInterface extends BreakpointListener {
    enum BreakpointReaction {
        WAIT,
        CONTINUE,
        ABORT,
        RESTART,
        SKIP,
        STEP_OUT
    }

    BreakpointReaction CheckBreakpointStatus(int index, MappingAction action);

    boolean isAbort();

}

