/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.soc.data.SocSupport;
import java.util.ArrayList;
import lombok.val;

public interface CircuitStateHolder {
  class HierarchyInfo {
    private final Circuit mainCircuit;
    private final ArrayList<Component> components = new ArrayList<>();

    public HierarchyInfo(Circuit circ) {
      mainCircuit = circ;
    }

    public void addComponent(Component comp) {
      components.add(comp);
    }

    public void registerCircuitListener(CircuitListener l) {
      if (mainCircuit != null) mainCircuit.addCircuitListener(l);
      for (val c : components) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          val f = (SubcircuitFactory) c.getFactory();
          f.getSubcircuit().addCircuitListener(l);
        }
      }
    }

    public void deregisterCircuitListener(CircuitListener l) {
      if (mainCircuit != null) mainCircuit.addCircuitListener(l);
      for (val c : components) {
        if (c.getFactory() instanceof SubcircuitFactory) {
          val f = (SubcircuitFactory) c.getFactory();
          f.getSubcircuit().removeCircuitListener(l);
        }
      }
    }

    public void registerComponentListener(ComponentListener l) {
      for (val c : components) c.addComponentListener(l);
    }

    public void deregisterComponentListener(ComponentListener l) {
      for (val c : components) c.addComponentListener(l);
    }

    public String getName() {
      val s = new StringBuilder();
      if (mainCircuit != null) s.append(mainCircuit.getName());
      for (val c : components) {
        if (s.length() != 0) s.append(":");
        s.append(SocSupport.getComponentName(c));
      }
      return s.toString();
    }

    public HierarchyInfo getCopy() {
      val copy = new HierarchyInfo(mainCircuit);
      for (val c : components) copy.addComponent(c);
      return copy;
    }
  }

  void setCircuitState(CircuitState state);

  void setHierarchyName(HierarchyInfo csh);
}
