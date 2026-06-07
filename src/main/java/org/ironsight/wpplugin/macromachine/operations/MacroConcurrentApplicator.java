package org.ironsight.wpplugin.macromachine.operations;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.*;
import org.ironsight.wpplugin.macromachine.Gui.GlobalActionPanel;
import org.ironsight.wpplugin.macromachine.Layers.CustomLayerControllerWrapper;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStateDTO;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStatus;
import org.ironsight.wpplugin.macromachine.REST.DTOs.ExecutionStepDTO;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyAction;
import org.ironsight.wpplugin.macromachine.operations.ApplyToMap.ApplyActionCallback;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.ActionFilterIO;
import org.ironsight.wpplugin.macromachine.operations.ValueProviders.InputOutputProvider;
import org.pepsoft.worldpainter.Dimension;

public class MacroConcurrentApplicator implements MacroApplicator {
  private final MacroContainer macros;
  private final MappingActionContainer actions;
  private final Supplier<Dimension> getDimension;
  private ExecutionStateDTO currentExecutionState =
      new ExecutionStateDTO(null, List.of(), 0, ExecutionStatus.IDLE);
  private Queue<UUID> queue = new ConcurrentLinkedQueue<>();
  private final int extraSuspendMillis = 10;
  private Consumer<UUID> onMacroFinished;

  public MacroConcurrentApplicator(
      MacroContainer macros,
      MappingActionContainer actions,
      Supplier<Dimension> getDimension,
      Consumer<UUID> onMacroFinished) {
    this.macros = macros;
    this.actions = actions;
    this.getDimension = getDimension;
    this.onMacroFinished = onMacroFinished;
  }

  public void shutdown() {
    executor.shutdown();
    executor = null;
  }

  private static ScheduledExecutorService executor = null;

  public void start() {
    if (executor != null) throw new IllegalArgumentException("can not run a second applicator!");
    executor =
        Executors.newSingleThreadScheduledExecutor(
            r -> {
              Thread t = new Thread(r);
              t.setName("MacroConcurrentApplicator-thread");
              return t;
            });
    executor.scheduleAtFixedRate(this::runExecutionQueue, 0, 1000, TimeUnit.MILLISECONDS);
  }

  @Override
  public Collection<ExecutionStatistic> applyLayerAction(
      Macro macro, ApplyActionCallback callback) {
    ApplyAction.ApplicationContext context =
        new ApplyAction.ApplicationContext(
            getDimension.get(),
            macros,
            actions,
            InputOutputProvider.INSTANCE,
            new CustomLayerControllerWrapper(),
            ActionFilterIO.instance); // FIXME lots of singletons with static access here
    updateState(new ExecutionStateDTO(macro.getUid(), List.of(), 0, ExecutionStatus.RUNNING));
    return Macro.applyMacroToDimension(context, macro, callback);
  }

  // FIXME this interface should have a onError callback
  @Override
  public ExecutionStateDTO getCurrentState() {
    synchronized (currentExecutionState) {
      return currentExecutionState;
    }
  }

  @Override
  public void updateState(ExecutionStateDTO newState) {
    synchronized (currentExecutionState) {
      if (currentExecutionState.status() != newState.status())
        System.out.println(
            "UPDATE EXECUTION STATE TO:" + newState.status() + " for " + newState.executionId());
      this.currentExecutionState = newState;
    }
  }

  private void runExecutionQueue() {
    // System.out.println("run rexecution for queue: " + queue);
    // System.out.println("queue:" + queue);
    if (queue.isEmpty()) return;
    final var nextUID = queue.poll();
    var next = macros.queryById(nextUID);
    if (next != null) {
      updateState(new ExecutionStateDTO(nextUID, List.of(), 0, ExecutionStatus.RUNNING));
      SwingUtilities.invokeLater(
          () -> {
            // System.out.println("Execute macro: " + next.getName() + "(" + next.getUid() + ")");
            // System.out.println("Queue:" + queue.size());
            try {
              applyMacroSync(next);
            } catch (Exception ex) {
              System.err.println("applying macro caused error:" + ex);
            } finally {
              updateState(new ExecutionStateDTO(null, List.of(), 0, ExecutionStatus.IDLE));
              onMacroFinished.accept(nextUID);
            }
          });
    } else {
      GlobalActionPanel.logMessage("error: can not execute macro, doesnt exist: " + nextUID);
    }
  }

  @Override
  public void queueMacro(UUID macro) {
    queue.add(macro);
  }

  @Override
  public List<UUID> getQueue() {
    return new ArrayList<>(queue);
  }

  @Override
  public void applyMacroSync(Macro macro) {
    ApplyActionCallback callback =
        new ApplyActionCallback() {
          @Override
          public void setProgressOfAction(int percent) {
            var state = getCurrentState();

            var steps = new ArrayList<>(state.steps());

            int idx = state.currentStepIndex();
            var currentStep = steps.get(idx);

            var updatedStep =
                new ExecutionStepDTO(currentStep.actionId(), currentStep.breakpoint(), percent);

            steps.set(idx, updatedStep);

            updateState(new ExecutionStateDTO(state.executionId(), steps, idx, state.status()));
          }

          @Override
          public boolean isActionAbort() {
            return getCurrentState().status().equals(ExecutionStatus.ABORTING);
          }

          @Override
          public void beforeEachAction(MappingAction action, Dimension dimension) {
            var currentState = getCurrentState();
            updateState(
                new ExecutionStateDTO(
                    currentState.executionId(),
                    currentState.steps(),
                    Math.min(currentState.steps().size() - 1, currentState.currentStepIndex() + 1),
                    currentState.status()));
          }

          @Override
          public void afterEachTile(int tileX, int tileY) {
            try {
              Thread.sleep(extraSuspendMillis);

            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public void afterEachAction(ExecutionStatistic statistic) {}

          @Override
          public boolean isUpdateMapAfterEachAction() {
            return false; // FIXME will be necessary for breakpoint usage
          }

          @Override
          public void setAllActionsBeforeRun(List<MappingAction> steps) {
            var stepDTOS =
                steps.stream()
                    .map(action -> new ExecutionStepDTO(action.getUid(), false, 0))
                    .toList();

            updateState(
                new ExecutionStateDTO(
                    getCurrentState().executionId(),
                    stepDTOS,
                    getCurrentState().currentStepIndex(),
                    getCurrentState().status()));
          }

          @Override
          public void afterEverything() {
            updateState(
                new ExecutionStateDTO(
                    getCurrentState().executionId(), List.of(), 0, ExecutionStatus.COMPLETED));
          }
        };

    applyLayerAction(macro, callback);
  }
}
