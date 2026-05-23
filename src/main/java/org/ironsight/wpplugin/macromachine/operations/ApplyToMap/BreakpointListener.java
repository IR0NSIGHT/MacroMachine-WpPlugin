package org.ironsight.wpplugin.macromachine.operations.ApplyToMap;

import java.util.ArrayList;
import org.ironsight.wpplugin.macromachine.operations.MappingAction;

public interface BreakpointListener {
  void OnReachedBreakpoint(int idx);

  void PostReachedBreakpoint(int idx);

  void SetBreakpoints(ArrayList<MappingAction> breakpoints);

  void afterEverything();
}
