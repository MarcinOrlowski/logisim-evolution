/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Location;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class WireRepairData {
  private final Wire wire;
  private final Location point;

  public WireRepairData(Wire wire, Location point) {
    this.wire = wire;
    this.point = point;
  }

  public Location getPoint() {
    return point;
  }

  public Wire getWire() {
    return wire;
  }
}
