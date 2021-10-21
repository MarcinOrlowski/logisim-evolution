/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

public class BoardInformation {

  @Getter private List<FpgaIoInformationContainer> allComponents;
  @Getter @Setter private String boardName;
  /**
   * Board picture.
   */
  @Getter @Setter private BufferedImage image;
  public FpgaClass fpga = new FpgaClass();

  public BoardInformation() {
    this.clear();
  }

  public void addComponent(FpgaIoInformationContainer comp) {
    allComponents.add(comp);
  }

  public void clear() {
    if (allComponents == null) allComponents = new LinkedList<>();
    else allComponents.clear();
    boardName = null;
    fpga.clear();
    image = null;
  }

  public void setComponents(List<FpgaIoInformationContainer> comps) {
    allComponents.clear();
    allComponents.addAll(comps);
  }

  public FpgaIoInformationContainer getComponent(BoardRectangle rect) {
    for (final var comp : allComponents) {
      if (comp.getRectangle().equals(rect)) {
        return comp;
      }
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  public Map<String, ArrayList<Integer>> getComponents() {
    final var result = new HashMap<String, ArrayList<Integer>>();
    final var list = new ArrayList<Integer>();

    var count = 0;
    for (final var type : IoComponentTypes.KNOWN_COMPONENT_SET) {
      count = 0;
      for (final var comp : allComponents) {
        if (comp.getType().equals(type)) {
          list.add(count, comp.getNrOfPins());
          count++;
        }
      }
      if (count > 0) {
        result.put(type.toString(), (ArrayList<Integer>) list.clone());
      }
      list.clear();
    }

    return result;
  }

  public String getComponentType(BoardRectangle rect) {
    for (final var comp : allComponents) {
      if (comp.getRectangle().equals(rect)) {
        return comp.getType().toString();
      }
    }
    return IoComponentTypes.Unknown.toString();
  }

  public String getDriveStrength(BoardRectangle rect) {
    for (final var comp : allComponents) {
      if (comp.getRectangle().equals(rect)) {
        return DriveStrength.GetContraintedDriveStrength(comp.getDrive());
      }
    }
    return "";
  }

  public List<BoardRectangle> getIoComponentsOfType(IoComponentTypes type, int nrOfPins) {
    final var result = new ArrayList<BoardRectangle>();
    for (final var comp : allComponents) {
      if (comp.getType().equals(type)) {
        if (!type.equals(IoComponentTypes.DIPSwitch) || nrOfPins <= comp.getNrOfPins()) {
          if (!type.equals(IoComponentTypes.PortIo) || nrOfPins <= comp.getNrOfPins()) {
            result.add(comp.getRectangle());
          }
        }
      }
    }
    return result;
  }

  public String getIoStandard(BoardRectangle rect) {
    for (final var comp : allComponents) {
      if (comp.getRectangle().equals(rect)) {
        return IoStandards.getConstraintedIoStandard(comp.getIoStandard());
      }
    }
    return "";
  }

  public int getNrOfDefinedComponents() {
    return allComponents.size();
  }

  public String getPullBehavior(BoardRectangle rect) {
    for (final var comp : allComponents) {
      if (comp.getRectangle().equals(rect)) {
        return PullBehaviors.getContraintedPullString(comp.getPullBehavior());
      }
    }
    return "";
  }
}
