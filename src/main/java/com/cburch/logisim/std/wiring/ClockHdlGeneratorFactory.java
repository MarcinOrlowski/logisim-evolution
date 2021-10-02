/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.SortedMap;
import java.util.TreeMap;

public class ClockHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  public static final int NR_OF_CLOCK_BITS = 5;
  public static final int DERIVED_CLOCK_INDEX = 0;
  public static final int INVERTED_DERIVED_CLOCK_INDEX = 1;
  public static final int POSITIVE_EDGE_TICK_INDEX = 2;
  public static final int NEGATIVE_EDGE_TICK_INDEX = 3;
  public static final int GLOBAL_CLOCK_INDEX = 4;
  private static final String HIGH_TICK_STR = "HighTicks";
  private static final int HIGH_TICK_ID = -1;
  private static final String LOW_TICK_STR = "LowTicks";
  private static final int LOW_TICK_ID = -2;
  private static final String PHASE_STR = "Phase";
  private static final int PHASE_ID = -3;
  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -4;

  public ClockHdlGeneratorFactory() {
    super("base");
    myParametersList
        .add(HIGH_TICK_STR, HIGH_TICK_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_HIGH)
        .add(LOW_TICK_STR, LOW_TICK_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_LOW)
        .add(PHASE_STR, PHASE_ID, HdlParameters.MAP_INT_ATTRIBUTE, Clock.ATTR_PHASE, 1)
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID, HdlParameters.MAP_LN2, Clock.ATTR_HIGH, Clock.ATTR_LOW);
    myWires
        .addWire("s_counter_next", NR_OF_BITS_ID)
        .addWire("s_counter_is_zero", 1)
        .addRegister("s_output_regs", NR_OF_CLOCK_BITS - 1)
        .addRegister("s_buf_regs", 2)
        .addRegister("s_counter_reg", NR_OF_BITS_ID)
        .addRegister("s_derived_clock_reg", PHASE_ID);
    myPorts
        .add(Port.INPUT, "GlobalClock", 1, 0)
        .add(Port.INPUT, "ClockTick", 1, 1)
        .add(Port.OUTPUT, "ClockBus", NR_OF_CLOCK_BITS, 2);
  }

  @Override
  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof netlistComponent)) return map;
    final var componentInfo = (netlistComponent) mapInfo;
    map.put("GlobalClock", TickComponentHdlGeneratorFactory.FPGA_CLOCK);
    map.put("ClockTick", TickComponentHdlGeneratorFactory.FPGA_TICK);
    map.put("ClockBus", getClockNetName(componentInfo.getComponent(), nets));
    return map;
  }

  private static String getClockNetName(Component comp, Netlist theNets) {
    final var contents = new StringBuilder();
    int clockNetId = theNets.getClockSourceId(comp);
    if (clockNetId >= 0) {
      contents.append("s_").append(HdlGeneratorFactory.CLOCK_TREE_NAME).append(clockNetId);
    }
    return contents.toString();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
            .pair("phase", PHASE_STR)
            .pair("nrOfBits", NR_OF_BITS_STR)
            .pair("lowTick", LOW_TICK_STR)
            .pair("highTick", HIGH_TICK_STR)
            .addRemarkBlock("Here the output signals are defines; we synchronize them all on the main clock");

    if (Hdl.isVhdl()) {
      contents.add("""
          ClockBus <= GlobalClock&s_output_regs;
          makeOutputs : PROCESS( GlobalClock )
          BEGIN
             IF (rising_edge(GlobalClock)) THEN
                s_buf_regs(0)     <= s_derived_clock_reg({{phase}} - 1);
                s_buf_regs(1)     <= NOT(s_derived_clock_reg({{phase}} - 1));
                s_output_regs(0)  <= s_buf_regs(0);
                s_output_regs(1)  <= s_buf_regs(1);
                s_output_regs(2)  <= NOT(s_buf_regs(0)) AND s_derived_clock_reg({{phase}} - 1);
                s_output_regs(3)  <= s_buf_regs(0) AND NOT(s_derived_clock_reg({{phase}} - 1));
             END IF;
          END PROCESS makeOutputs;
          """);
    } else {
      contents.add("""
          assign ClockBus = {GlobalClock,s_output_regs};
          always @(posedge GlobalClock)
          begin
             s_buf_regs[0]    <= s_derived_clock_reg[{{phase}} - 1];
             s_buf_regs[1]    <= ~s_derived_clock_reg[{{phase}} - 1];
             s_output_regs[0] <= s_buf_regs[0];
             s_output_regs[1] <= s_output_regs[1];
             s_output_regs[2] <= ~s_buf_regs[0] & s_derived_clock_reg[{{phase}} - 1];
             s_output_regs[3] <= ~s_derived_clock_reg[{{phase}} - 1] & s_buf_regs[0];
          end
          """);
    }
    contents.add("").addRemarkBlock("Here the control signals are defined");
    if (Hdl.isVhdl()) {
      contents.add("""
          s_counter_is_zero <= '1' WHEN s_counter_reg = std_logic_vector(to_unsigned(0,{{nrOfBits}})) ELSE '0';
          s_counter_next    <= std_logic_vector(unsigned(s_counter_reg) - 1)
                                 WHEN s_counter_is_zero = '0' ELSE
                              std_logic_vector(to_unsigned(({{lowTick}}-1), {{nrOfBits}}))
                                 WHEN s_derived_clock_reg(0) = '1' ELSE
                              std_logic_vector(to_unsigned(({{highTick}}-1), {{nrOfBits}}));
          """);
    } else {
      contents.add("""
              assign s_counter_is_zero = (s_counter_reg == 0) ? 1'b1 : 1'b0;
              assign s_counter_next = (s_counter_is_zero == 1'b0)
                                         ? s_counter_reg - 1
                                         : (s_derived_clock_reg[0] == 1'b1)
                                            ? {{lowTick}} - 1
                                            : {{highTick}} - 1;

              """)
          .addRemarkBlock("Here the initial values are defined (for simulation only)")
          .add("""
              initial
              begin
                 s_output_regs = 0;
                 s_derived_clock_reg = 0;
                 s_counter_reg = 0;
              end
              """);
    }
    contents.add("").addRemarkBlock("Here the state registers are defined");
    if (Hdl.isVhdl()) {
      contents.add("""
          makeDerivedClock : PROCESS( GlobalClock , ClockTick , s_counter_is_zero ,
                                      s_derived_clock_reg)
          BEGIN
             IF (rising_edge(GlobalClock)) THEN
                IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only
                   s_derived_clock_reg <= (OTHERS => '1');
                ELSIF (ClockTick = '1') THEN
                   FOR n IN {{phase}}-1 DOWNTO 1 LOOP
                     s_derived_clock_reg(n) <= s_derived_clock_reg(n-1);
                   END LOOP;
                   s_derived_clock_reg(0) <= s_derived_clock_reg(0) XOR s_counter_is_zero;
                END IF;
             END IF;
          END PROCESS makeDerivedClock;

          makeCounter : PROCESS( GlobalClock , ClockTick , s_counter_next ,
                                 s_derived_clock_reg )
          BEGIN
             IF (rising_edge(GlobalClock)) THEN
                IF (s_derived_clock_reg(0) /= '0' AND s_derived_clock_reg(0) /= '1') THEN --For simulation only
                   s_counter_reg <= (OTHERS => '0');
                ELSIF (ClockTick = '1') THEN
                   s_counter_reg <= s_counter_next;
                END IF;
             END IF;
          END PROCESS makeCounter;
          """);
    } else {
      contents.add("""
          integer n;
          always @(posedge GlobalClock)
          begin
             if (ClockTick)
             begin
                s_derived_clock_reg[0] <= s_derived_clock_reg[0] ^ s_counter_is_zero;
                for (n = 1; n < {{phase}}; n = n+1) begin
                   s_derived_clock_reg[n] <= s_derived_clock_reg[n-1];
                end
             end
          end

          always @(posedge GlobalClock)
          begin
             if (ClockTick)
             begin
                s_counter_reg <= s_counter_next;
             end
          end
          """);
    }
    contents.add("");
    return contents;
  }
}