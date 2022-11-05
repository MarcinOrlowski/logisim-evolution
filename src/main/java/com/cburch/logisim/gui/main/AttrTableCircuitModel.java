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
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.proj.Project;

public class AttrTableCircuitModel extends AttributeSetTableModel {
  private final Project project;
  private final Circuit circuit;

  public AttrTableCircuitModel(Project project, Circuit circuit) {
    super(circuit.getStaticAttributes());
    this.project = project;
    this.circuit = circuit;
  }

  @Override
  public String getTitle() {
    return S.get("circuitAttrTitle", circuit.getName());
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    if (!project.getLogisimFile().contains(circuit)) {
      throw new AttrTableSetException(S.get("cannotModifyCircuitError"));
    }

    final var xn = new CircuitMutation(circuit);
    xn.setForCircuit(attr, value);
    project.doAction(xn.toAction(S.getter("changeCircuitAttrAction")));
  }
}
