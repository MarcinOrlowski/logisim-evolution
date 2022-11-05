/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;

class AttrTableComponentModel extends AttributeSetTableModel {
  final Project project;
  final Circuit circuit;
  final Component component;

  AttrTableComponentModel(Project proj, Circuit circ, Component comp) {
    super(comp.getAttributeSet());
    this.project = proj;
    this.circuit = circ;
    this.component = comp;
    setInstance(comp.getFactory());
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public Component getComponent() {
    return component;
  }

  @Override
  public String getTitle() {
    final var label = component.getAttributeSet().getValue(StdAttr.LABEL);
    final var location = component.getLocation();
    var title = component.getFactory().getDisplayName();
    if (label != null && label.length() > 0) {
      title += " \"" + label + "\"";
    } else if (location != null) {
      title += " " + location;
    }
    return title;
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    if (!project.getLogisimFile().contains(circuit)) {
      throw new AttrTableSetException(S.get("cannotModifyCircuitError"));
    }

    final var action = new SetAttributeAction(circuit, S.getter("changeAttributeAction"));
    final var compAttrSet = component.getAttributeSet();
    if (compAttrSet != null) {
      final var mayBeChangedList = compAttrSet.attributesMayAlsoBeChanged(attr, value);
      if (mayBeChangedList != null) {
        for (final var mayChangeAttr : mayBeChangedList) {
          // mayChangeAttr is set to its current value to have it restored on undo
          action.set(component, mayChangeAttr, compAttrSet.getValue(mayChangeAttr));
        }
      }
    }
    action.set(component, attr, value);
    project.doAction(action);
  }
}
