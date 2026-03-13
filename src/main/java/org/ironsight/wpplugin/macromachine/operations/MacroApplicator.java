package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyActionCallback;

import java.util.Collection;

public interface MacroApplicator
{
    Collection<ExecutionStatistic> applyLayerAction(Macro macro, ApplyActionCallback callback);
}
