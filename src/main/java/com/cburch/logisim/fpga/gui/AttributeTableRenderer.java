/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.util.ColorUtil;
import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class AttributeTableRenderer extends JLabel implements TableCellRenderer {
  // This component has HDL support
  public static final String HDL_SUPPORTED = "HDL_SUPPORTED";
  // This component is not HDL supported
  public static final String HDL_NOT_SUPPORTED = "HDL_NOT_SUPPORTED";
  // Unknown state for HDL support for this element
  public static final String HDL_SUPPORT_UNKNOWN = "HDL_UNKNOWN";

  // Value of this field is required to be non-empty for HDL support to deal with this element.
  public static final String HDL_REQUIRED_FIELD = ">_HDL_REQUIRED_FIELD_<";

  // Table columns' meaning.
  protected static final int LABEL = 0;
  protected static final int VALUE = 1;

  protected static final ArrayList<String> CorrectStrings = new ArrayList<>();

  public AttributeTableRenderer() {
    setOpaque(true);
    CorrectStrings.clear();
    CorrectStrings.add(HDL_NOT_SUPPORTED);
    CorrectStrings.add(HDL_SUPPORT_UNKNOWN);
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object infoObj, boolean isSelected,
                                                 boolean hasFocus, int rowIdx, int columnIdx) {
    // Some sanity checks to avoid NPE
    final var value = (infoObj != null) ? (String) infoObj : "???";

    // Set default cell colors
    setBackground(javax.swing.UIManager.getColor("Table.background"));
    setForeground(javax.swing.UIManager.getColor("Table.foreground"));

    // First row is always a FPGS support status.
    if (rowIdx == 0) {
      return renderFpgaSupportRow(columnIdx, value);
    }

    // Render all the rest
    if (value.equals(HDL_REQUIRED_FIELD)) {
      // This field (mostly lables) is required to be set for FGPA support to work.
      // But this is not an error if user do not really care FPGA at this moment, so let's color differently.
      setCellColors(Color.ORANGE);
      setHorizontalAlignment(JLabel.CENTER);
      setText(S.get("FPGAHdlRequired"));
    } else if ((value.length() == 7 || value.length() == 9) && value.indexOf('#') == 0) {
      // Is this #RGB or #RGBA hex color value? If so, render the color
      final var red = Integer.valueOf(value.substring(1, 3), 16);
      final var green = Integer.valueOf(value.substring(3, 5), 16);
      final var blue = Integer.valueOf(value.substring(5, 7), 16);
      final var alpha = value.length() == 7 ? 255 : Integer.valueOf(value.substring(7, 9), 16);
      final var bgColor = new Color(red, green, blue, alpha);
      setCellColors(bgColor);
      setText(value.toUpperCase());
      setHorizontalAlignment(JLabel.CENTER);
    } else {
      if (isSelected) {
        setForeground(javax.swing.UIManager.getColor("Table.selectionForeground"));
        setBackground(javax.swing.UIManager.getColor("Table.selectionBackground"));
      }
      setText(value);
      setHorizontalAlignment(JLabel.LEFT);
    }

    setBorder(null);
    return this;
  }

  /**
   * First attribute table rows always shows component FPGA support status.
   */
  protected Component renderFpgaSupportRow(int columnIdx, String value) {

    if (columnIdx == LABEL) {
      setText(value);
      return this;
    }

    // Let's color the status cell fancy way.
    final String labelKey;
    var bg = javax.swing.UIManager.getColor("Table.background");
    if (!value.equals(HDL_SUPPORT_UNKNOWN)) {
      if (value.equals(HDL_NOT_SUPPORTED)) {
        bg = Color.RED;
        labelKey = "FPGANotSupported";
      } else {
        bg = Color.GREEN;
        labelKey = "FPGASupported";
      }
    } else {
      labelKey = "FPGAUnknown";
    }

    setText(S.get(labelKey));
    setCellColors(bg);
    setHorizontalAlignment(JLabel.CENTER);

    return this;
  }

  /**
   * Sets given BG cell color and complementary B/W foreground.
   */
  protected void setCellColors(Color bgColor) {
    setBackground(bgColor);
    setForeground(ColorUtil.getComplementaryBlackWhite(bgColor));
  }

}
