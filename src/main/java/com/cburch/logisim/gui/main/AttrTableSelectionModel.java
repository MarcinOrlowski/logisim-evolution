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

import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitException;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.PositionComparator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.AttrTableSetException;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.main.Selection.Event;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.SetAttributeAction;
import com.cburch.logisim.util.AutoLabel;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.util.TreeSet;
import lombok.val;

class AttrTableSelectionModel extends AttributeSetTableModel implements Selection.Listener {
  private final Project project;
  private final Frame frame;

  public AttrTableSelectionModel(Project project, Frame frame) {
    super(frame.getCanvas().getSelection().getAttributeSet());
    this.project = project;
    this.frame = frame;
    frame.getCanvas().getSelection().addListener(this);
  }

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    super.attributeValueChanged(e);
    if (e.getAttribute().equals(StdAttr.LABEL)) fireTitleChanged();
  }

  @Override
  public String getTitle() {
    ComponentFactory wireFactory = null;
    ComponentFactory factory = null;
    String label = null;
    Location loc = null;
    var factoryCount = 0;
    var totalCount = 0;
    var variousFound = false;

    val selection = frame.getCanvas().getSelection();
    for (val comp : selection.getComponents()) {
      val fact = comp.getFactory();
      if (fact.equals(factory)) {
        factoryCount++;
      } else if (comp instanceof Wire) {
        wireFactory = fact;
        if (factory == null) {
          factoryCount++;
        }
      } else if (factory == null) {
        factory = fact;
        factoryCount = 1;
        label = comp.getAttributeSet().getValue(StdAttr.LABEL);
        loc = comp.getLocation();
      } else {
        variousFound = true;
      }
      if (!(comp instanceof Wire)) {
        totalCount++;
      }
    }

    if (factory == null) {
      factory = wireFactory;
    }

    if (variousFound) {
      setInstance(factory);
      return S.get("selectionVarious", "" + totalCount);
    } else if (factoryCount == 0) {
      val circ = frame.getCanvas().getCircuit();
      if (circ != null) {
        val circName = circ.getName();
        setInstance(circ.getSubcircuitFactory());
        return S.get("circuitAttrTitle", circName);
      } else {
        val hdl = (VhdlContent) frame.getCanvas().getCurrentHdl();
        val circName = hdl.getName();
        setInstance(null);
        return S.get("hdlAttrTitle", circName);
      }
    } else if (factoryCount == 1) {
      setInstance(factory);
      if (label != null && label.length() > 0) {
        return factory.getDisplayName() + " \"" + label + "\"";
      } else if (loc != null) {
        return factory.getDisplayName() + " " + loc;
      } else {
        return factory.getDisplayName();
      }
    } else {
      setInstance(factory);
      return S.get("selectionMultiple", factory.getDisplayName(), "" + factoryCount);
    }
  }

  //
  // Selection.Listener methods
  @Override
  public void selectionChanged(Event event) {
    fireTitleChanged();
    if (!frame.getEditorView().equals(Frame.EDIT_APPEARANCE)) {
      frame.setAttrTableModel(this);
    }
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) throws AttrTableSetException {
    val selection = frame.getCanvas().getSelection();
    val circuit = frame.getCanvas().getCircuit();
    if (selection.isEmpty() && circuit != null) {
      val circuitModel = new AttrTableCircuitModel(project, circuit);
      circuitModel.setValueRequested(attr, value);
    } else {
      val act = new SetAttributeAction(circuit, S.getter("selectionAttributeAction"));
      AutoLabel labler = null;
      if (attr.equals(StdAttr.LABEL)) {
        labler = new AutoLabel((String) value, circuit);
      }
      val comps = new TreeSet<>(new PositionComparator());
      comps.addAll(selection.getComponents());
      for (val comp : comps) {
        if (!(comp instanceof Wire)) {
          if (comp.getFactory() instanceof SubcircuitFactory) {
            val fac = (SubcircuitFactory) comp.getFactory();
            if (attr.equals(CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE) || attr.equals(CircuitAttributes.NAME_ATTR)) {
              try {
                val mutation = new CircuitMutation(fac.getSubcircuit());
                mutation.setForCircuit(attr, value);
                val action = mutation.toAction(null);
                project.doAction(action);
              } catch (CircuitException ex) {
                OptionPane.showMessageDialog(project.getFrame(), ex.getMessage());
              }
              return;
            }
          }
          if (attr.equals(StdAttr.LABEL)) {
            if (labler.hasNext(circuit)) {
              if (comps.size() > 1) {
                act.set(comp, attr, labler.getNext(circuit, comp.getFactory()));
              } else {
                if (getAttributeSet().getValue(StdAttr.LABEL).equals(value)) return;
                else act.set(comp, attr, labler.getCurrent(circuit, comp.getFactory()));
              }
            } else act.set(comp, attr, "");
          } else act.set(comp, attr, value);
        }
      }
      project.doAction(act);
    }
  }
}
