/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import java.util.SortedMap;
import java.util.TreeMap;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class Ttl74273 extends AbstractOctalFlops {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74273";

  public static class Ttl74273HDLGenerator extends AbstractOctalFlopsHDLGenerator {

    @Override
    public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
      final var map = new TreeMap<String, String>();
      if (!(mapInfo instanceof NetlistComponent)) return map;
      final var comp = (NetlistComponent) mapInfo;
      map.putAll(super.GetPortMap(nets, comp));
      map.put("nCLKEN", "'0'");
      map.putAll(GetNetMap("nCLR", false, comp, 0, nets));
      return map;
    }
  }

  public Ttl74273() {
    super(
        _ID,
        (byte) 20,
        new byte[] {2, 5, 6, 9, 12, 15, 16, 19},
        new String[] {
          "nCLR", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4", "Q4", "CLK", "Q5", "D5", "D6", "Q6",
          "Q7", "D7", "D8", "Q8"
        },
        new Ttl74273HDLGenerator());
    super.SetWe(false);
  }
}
