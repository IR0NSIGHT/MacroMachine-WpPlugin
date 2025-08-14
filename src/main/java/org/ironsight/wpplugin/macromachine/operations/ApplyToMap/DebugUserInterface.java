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

    public void setProgessTo(int actionIndex, int actionpercent, int totalActions);
    BreakpointReaction CheckBreakpointStatus(int index, MappingAction action);

    boolean isAbort();

}

