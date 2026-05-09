package org.ironsight.wpplugin.macromachine.operations;

import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStateDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStatus;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyActionCallback;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public interface MacroApplicator
{
    Collection<ExecutionStatistic> applyLayerAction(Macro macro, ApplyActionCallback callback);

    ExecutionStateDTO getCurrentState();

    void updateState(ExecutionStateDTO newState);

    void applyMacroSync(Macro macro);
    void queueMacro(UUID macroUid);

    List<UUID> getQueue();
    static MacroApplicator mock() {
        return new MacroApplicator() {
            private ExecutionStateDTO stateDTO = new ExecutionStateDTO(null, List.of(),0,ExecutionStatus.IDLE);
            private LinkedList<UUID> queue = new LinkedList<>();
            @Override
            public Collection<ExecutionStatistic> applyLayerAction(Macro macro, ApplyActionCallback callback) {
                System.out.println("apply macro " + macro);
                return java.util.List.of();
            }

            @Override
            public ExecutionStateDTO getCurrentState() {
                return stateDTO;
            }

            @Override
            public void updateState(ExecutionStateDTO newState) {
                this.stateDTO = newState;
            }

            @Override
            public void applyMacroSync(Macro macro) {

            }

            @Override
            public void queueMacro(UUID macroUid) {
                queue.add(macroUid);
            }

            @Override
            public List<UUID> getQueue() {
                return queue.stream().toList();
            }
        };
    }
}
