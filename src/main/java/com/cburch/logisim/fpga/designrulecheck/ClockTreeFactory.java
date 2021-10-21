/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import com.cburch.logisim.comp.Component;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;

public class ClockTreeFactory {

  @Setter private ClockSourceContainer sourceContainer;
  private final ArrayList<ClockTreeContainer> sourceTrees;

  public ClockTreeFactory() {
    sourceTrees = new ArrayList<>();
  }

  public void addClockNet(List<String> hierarchyNames, int clocksourceid, ConnectionPoint connection, boolean isPinClock) {
    ClockTreeContainer destination = null;
    for (final var search : sourceTrees) {
      if (search.equals(hierarchyNames, clocksourceid)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(hierarchyNames, clocksourceid, isPinClock);
      sourceTrees.add(destination);
    } else if (!destination.isPinClockSource() && isPinClock) destination.setPinClock();
    destination.addNet(connection);
  }

  public void addClockSource(List<String> hierarchyNames, int clockSourceId, ConnectionPoint connection) {
    ClockTreeContainer destination = null;
    for (final var search : sourceTrees) {
      if (search.equals(hierarchyNames, clockSourceId)) {
        destination = search;
      }
    }
    if (destination == null) {
      destination = new ClockTreeContainer(hierarchyNames, clockSourceId, false);
      sourceTrees.add(destination);
    }
    destination.addSource(connection);
  }

  public void clean() {
    for (final var tree : sourceTrees) tree.clear();
    sourceTrees.clear();
    if (sourceContainer != null) sourceContainer.clear();
  }

  public int getClockSourceId(List<String> hierarchy, Net selectedNet, byte selectedNetBitIndex) {
    for (var i = 0; i < sourceContainer.getNrOfSources(); i++) {
      for (final var ThisClockNet : sourceTrees) {
        if (ThisClockNet.equals(hierarchy, i)) {
          /*
           * we found a clock net corresponding the Hierarchy and
           * clock source id
           */
          for (final var clockEntry : ThisClockNet.getClockEntries(selectedNet)) {
            if (clockEntry == selectedNetBitIndex) return i;
          }
        }
      }
    }
    return -1;
  }

  public int getClockSourceId(Component comp) {
    if (sourceContainer == null) return -1;
    return sourceContainer.getClockId(comp);
  }

  public ClockSourceContainer getSourceContainer() {
    if (sourceContainer == null) sourceContainer = new ClockSourceContainer();
    return sourceContainer;
  }

}
