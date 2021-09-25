/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class HexDigitShape extends SevenSegmentShape {

  public HexDigitShape(int x, int y, DynamicElement.Path p) {
    super(x, y, p);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-hexdigit"));
  }

  @Override
  public String getDisplayName() {
    return S.get("hexDigitComponent");
  }

  @Override
  public String toString() {
    return "Hex Digit:" + getBounds();
  }
}
