/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.CircuitState;
import java.util.HashMap;
import java.util.List;
import javax.swing.tree.TreeNode;
import lombok.val;

class SimulationTreeTopNode extends SimulationTreeNode {

  public SimulationTreeTopNode(SimulationTreeModel model, List<CircuitState> allRootStates) {
    super(model, null);
    for (val state : allRootStates)
      children.add(new SimulationTreeCircuitNode(model, null, state, null));
  }

  public void updateSimulationList(List<CircuitState> allRootStates) {
    var changed = false;
    val old = new HashMap<CircuitState, TreeNode>();
    val oldPos = new HashMap<CircuitState, Integer>();
    var i = 0;
    for (val node : children) {
      val state = ((SimulationTreeCircuitNode) node).getCircuitState();
      old.put(state, node);
      oldPos.put(state, i++);
    }
    children.clear();
    i = 0;
    for (val state : allRootStates) {
      var node = old.get(state);
      if (node == null) {
        changed = true;
        node = new SimulationTreeCircuitNode(model, null, state, null);
      } else if (oldPos.get(state) != i++) {
        changed = true;
      }
      children.add(node);
    }
    if (changed) model.fireStructureChanged(this);
  }

  @Override
  public String toString() {
    return "Active Simulations";
  }
}
