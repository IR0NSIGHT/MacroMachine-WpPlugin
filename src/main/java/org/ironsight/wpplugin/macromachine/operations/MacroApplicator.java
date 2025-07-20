package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyAction;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyActionCallback;

import java.util.Collection;
import java.util.function.Consumer;

public interface MacroApplicator {
    Collection<ExecutionStatistic> applyLayerAction(Macro macro, ApplyActionCallback callback);
}
