/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class AttrTableModelEvent {
  private final AttrTableModel model;
  private final int index;

  public AttrTableModelEvent(AttrTableModel model) {
    this(model, -1);
  }

  public AttrTableModelEvent(AttrTableModel model, int index) {
    this.model = model;
    this.index = index;
  }

  public AttrTableModel getModel() {
    return model;
  }

  public int getRowIndex() {
    return index;
  }

  public Object getSource() {
    return model;
  }
}
