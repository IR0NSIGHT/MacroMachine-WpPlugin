package org.ironsight.wpplugin.macromachine.operations;

import java.util.Collection;
import java.util.function.Consumer;

public interface MacroApplicator {
    Collection<ExecutionStatistic> applyLayerAction(MappingMacro macro, Consumer<ApplyAction.Progess> setProgress);
}
