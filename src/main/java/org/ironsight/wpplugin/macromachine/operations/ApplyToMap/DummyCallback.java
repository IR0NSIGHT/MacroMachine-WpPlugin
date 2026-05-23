package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import java.util.List;
import org.ironsight.wpplugin.macromachine.operations.ExecutionStatistic;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;
import org.pepsoft.worldpainter.Dimension;

public class DummyCallback implements ApplyActionCallback {
  @Override
  public void setProgressOfAction(int percent) {}

  @Override
  public boolean isActionAbort() {
    return false;
  }

  @Override
  public void beforeEachAction(MappingAction action, Dimension dimension) {}

  @Override
  public void afterEachTile(int tileX, int tileY) {}

  @Override
  public void afterEachAction(ExecutionStatistic statistic) {}

  @Override
  public boolean isUpdateMapAfterEachAction() {
    return false;
  }

  @Override
  public void setAllActionsBeforeRun(List<MappingAction> steps) {}

  @Override
  public void afterEverything() {}
}
