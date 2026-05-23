package org.ironsight.wpplugin.macromachine.operations.ValueProviders;

import java.io.Serializable;
import org.pepsoft.worldpainter.Dimension;

public interface IPositionValueGetter extends IDisplayUnit, Serializable, IMappingValue {
  int getValueAt(Dimension dim, int x, int y);

  int[] getAllInputValues();

  static boolean isLegalInput(IPositionValueGetter getter, int value) {
    return (getter.getMinValue() <= value && value <= getter.getMaxValue());
  }
}
