/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.base;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.val;

class TextAttributes extends AbstractAttributeSet {
  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          Text.ATTR_TEXT, Text.ATTR_FONT, Text.ATTR_HALIGN, Text.ATTR_VALIGN);

  @Getter private String text;
  @Getter private Font font;
  private AttributeOption horizAlign;
  private AttributeOption vertAlign;
  @Getter private Bounds offsetBounds;

  public TextAttributes() {
    text = "";
    font = StdAttr.DEFAULT_LABEL_FONT;
    horizAlign = Text.ATTR_HALIGN.parse("center");
    vertAlign = Text.ATTR_VALIGN.parse("base");
    offsetBounds = null;
  }

  @Override
  protected void copyInto(AbstractAttributeSet destObj) {
    // nothing to do
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return ATTRIBUTES;
  }

  int getHorizontalAlign() {
    return (Integer) horizAlign.getValue();
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    if (attr == Text.ATTR_TEXT) return (V) text;
    if (attr == Text.ATTR_FONT) return (V) font;
    if (attr == Text.ATTR_HALIGN) return (V) horizAlign;
    if (attr == Text.ATTR_VALIGN) return (V) vertAlign;
    return null;
  }

  int getVerticalAlign() {
    return (Integer) vertAlign.getValue();
  }

  boolean setOffsetBounds(Bounds value) {
    val old = offsetBounds;
    val same = Objects.equals(old, value);
    if (!same) {
      offsetBounds = value;
    }
    return !same;
  }

  @Override
  public <V> void setValue(Attribute<V> attr, V value) {
    if (attr == Text.ATTR_TEXT) {
      text = (String) value;
    } else if (attr == Text.ATTR_FONT) {
      font = (Font) value;
    } else if (attr == Text.ATTR_HALIGN) {
      horizAlign = (AttributeOption) value;
    } else if (attr == Text.ATTR_VALIGN) {
      vertAlign = (AttributeOption) value;
    } else {
      throw new IllegalArgumentException("unknown attribute");
    }
    offsetBounds = null;
    fireAttributeValueChanged(attr, value, null);
  }
}
