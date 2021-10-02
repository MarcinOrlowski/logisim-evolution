/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;
import java.util.List;

public class BinToBcdHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public BinToBcdHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, BinToBcd.ATTR_BinBits);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    final var nrOfSignalBits = switch (nrOfPorts) {
      case 2 -> 7;
      case 3 -> 11;
      default -> 16;
    };
    final var nrOfSignals = switch (nrOfPorts) {
      case 2 -> 4;
      case 3 -> 7;
      default -> 11;
    };
    for (var signal = 0; signal < nrOfSignals; signal++)
      myWires.addWire(String.format("s_level_%d", signal), nrOfSignalBits);
    myPorts.add(Port.INPUT, "BinValue", NR_OF_BITS_ID, 0);
    for (var i = 1; i <= nrOfPorts; i++)
      myPorts.add(Port.OUTPUT, String.format("BCD%d", (int) (Math.pow(10, i - 1))), 4, i);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("nrOfBits", NR_OF_BITS_STR);
    final var nrOfBits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    if (Hdl.isVhdl()) {
      switch (nrOfPorts) {
        case 2:
          contents
              .add("""
                  s_level_0(6 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');
                  s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;
                  s_level_1(2 DOWNTO 0) <= s_level_0(2 DOWNTO 0);
                  s_level_2(1 DOWNTO 0) <= s_level_1(1 DOWNTO 0);
                  s_level_2(6)          <= s_level_1(6);
                  s_level_3(6 DOWNTO 5) <= s_level_2(6 DOWNTO 5);
                  s_level_3(0)          <= s_level_2(0);

                  BCD1  <= s_level_3( 3 DOWNTO 0);
                  BCD10 <= \"0\"&s_level_3(6 DOWNTO 4);
                  """)
              .add(getAdd3Block("s_level_0", 6, "s_level_1", 6, "C1"))
              .add(getAdd3Block("s_level_1", 5, "s_level_2", 5, "C2"))
              .add(getAdd3Block("s_level_2", 4, "s_level_3", 4, "C3"));
          break;
        case 3:
          contents
              .add("""
                  s_level_0(10 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');
                  s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;
                  s_level_1(10)          <= s_level_0(10);
                  s_level_1( 5 DOWNTO 0) <= s_level_0( 5 DOWNTO 0);
                  s_level_2(10 DOWNTO 9) <= s_level_1(10 DOWNTO 9);
                  s_level_2( 4 DOWNTO 0) <= s_level_1( 4 DOWNTO 0);
                  s_level_3(10 DOWNTO 8) <= s_level_2(10 DOWNTO 8);
                  s_level_3( 3 DOWNTO 0) <= s_level_2( 3 DOWNTO 0);
                  s_level_4( 2 DOWNTO 0) <= s_level_3( 2 DOWNTO 0);
                  s_level_5(10)          <= s_level_4(10);
                  s_level_5( 1 DOWNTO 0) <= s_level_4( 1 DOWNTO 0);
                  s_level_6(10 DOWNTO 9) <= s_level_5(10 DOWNTO 9);
                  s_level_6(0)           <= s_level_5(0);

                  BCD1   <= s_level_6( 3 DOWNTO 0 );
                  BCD10  <= s_level_6( 7 DOWNTO 4 );
                  BCD100 <= "0"&s_level_6(10 DOWNTO 8);
                  """)
              .add(getAdd3Block("s_level_0", 9, "s_level_1", 9, "C0"))
              .add(getAdd3Block("s_level_1", 8, "s_level_2", 8, "C1"))
              .add(getAdd3Block("s_level_2", 7, "s_level_3", 7, "C2"))
              .add(getAdd3Block("s_level_3", 6, "s_level_4", 6, "C3"))
              .add(getAdd3Block("s_level_4", 5, "s_level_5", 5, "C4"))
              .add(getAdd3Block("s_level_5", 4, "s_level_6", 4, "C5"))
              .add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C6"))
              .add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C7"))
              .add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C8"));
          break;
        case 4:
          contents
              .add("""
                  s_level_0(15 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');
                  s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;
                  s_level_1(15 DOWNTO 14)  <= s_level_0(15 DOWNTO 14);
                  s_level_1( 9 DOWNTO  0)  <= s_level_0( 9 DOWNTO  0);
                  s_level_2(15 DOWNTO 13)  <= s_level_1(15 DOWNTO 13);
                  s_level_2( 8 DOWNTO  0)  <= s_level_1( 8 DOWNTO  0);
                  s_level_3(15 DOWNTO 12)  <= s_level_2(15 DOWNTO 12);
                  s_level_3( 7 DOWNTO  0)  <= s_level_2( 7 DOWNTO  0);
                  s_level_4(15)            <= s_level_3(15);
                  s_level_4( 6 DOWNTO  0)  <= s_level_3( 6 DOWNTO  0);
                  s_level_5(15 DOWNTO 14)  <= s_level_4(15 DOWNTO 14);
                  s_level_5( 5 DOWNTO  0)  <= s_level_4( 5 DOWNTO  0);
                  s_level_6(15 DOWNTO 13)  <= s_level_5(15 DOWNTO 13);
                  s_level_6( 4 DOWNTO  0)  <= s_level_5( 4 DOWNTO  0);
                  s_level_7( 3 DOWNTO  0)  <= s_level_6( 3 DOWNTO  0);
                  s_level_8(15)            <= s_level_7(15);
                  s_level_8( 2 DOWNTO  0)  <= s_level_7( 2 DOWNTO  0);
                  s_level_9(15 DOWNTO 14)  <= s_level_8(15 DOWNTO 14);
                  s_level_9( 1 DOWNTO  0)  <= s_level_8( 1 DOWNTO  0);
                  s_level_10(15 DOWNTO 13) <= s_level_9(15 DOWNTO 13);
                  s_level_10(0)            <= s_level_9(0);

                  BCD1    <= s_level_10( 3 DOWNTO  0);
                  BCD10   <= s_level_10( 7 DOWNTO  4);
                  BCD100  <= s_level_10(11 DOWNTO  8);
                  BCD1000 <= s_level_10(15 DOWNTO 12);
                  """)
              .add(getAdd3Block("s_level_0", 13, "s_level_1", 13, "C0"))
              .add(getAdd3Block("s_level_1", 12, "s_level_2", 12, "C1"))
              .add(getAdd3Block("s_level_2", 11, "s_level_3", 11, "C2"))
              .add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C3"))
              .add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C4"))
              .add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C5"))
              .add(getAdd3Block("s_level_6", 7, "s_level_7", 7, "C6"))
              .add(getAdd3Block("s_level_7", 6, "s_level_8", 6, "C7"))
              .add(getAdd3Block("s_level_8", 5, "s_level_9", 5, "C8"))
              .add(getAdd3Block("s_level_9", 4, "s_level_10", 4, "C9"))
              .add(getAdd3Block("s_level_3", 14, "s_level_4", 14, "C10"))
              .add(getAdd3Block("s_level_4", 13, "s_level_5", 13, "C11"))
              .add(getAdd3Block("s_level_5", 12, "s_level_6", 12, "C12"))
              .add(getAdd3Block("s_level_6", 11, "s_level_7", 11, "C13"))
              .add(getAdd3Block("s_level_7", 10, "s_level_8", 10, "C14"))
              .add(getAdd3Block("s_level_8", 9, "s_level_9", 9, "C15"))
              .add(getAdd3Block("s_level_9", 8, "s_level_10", 8, "C16"))
              .add(getAdd3Block("s_level_6", 15, "s_level_7", 15, "C17"))
              .add(getAdd3Block("s_level_7", 14, "s_level_8", 14, "C18"))
              .add(getAdd3Block("s_level_8", 13, "s_level_9", 13, "C19"))
              .add(getAdd3Block("s_level_9", 12, "s_level_10", 12, "C20"));
          break;
      }
    } else {
      Reporter.report.addFatalError("Strange, this should not happen as Verilog is not yet supported!\n");
    }
    return contents;
  }

  private List<String> getAdd3Block(String srcName, int srcStartId, String destName, int destStartId, String processName) {
    return LineBuffer.getBuffer()
        .pair("srcName", srcName)
        .pair("srcStartId", srcStartId)
        .pair("srcDownTo", (srcStartId - 3))
        .pair("destName", destName)
        .pair("destStartId", destStartId)
        .pair("destDownTo", (destStartId - 3))
        .pair("proc", processName)
        .add("""

            ADD3_{{proc}} : PROCESS({{srcName}})
            BEGIN
               CASE ( {{srcName}}( {{srcStartId}} DOWNTO {{srcDownTo}}) ) IS
                  WHEN "0000" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0000";
                  WHEN "0001" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0001";
                  WHEN "0010" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0010";
                  WHEN "0011" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0011";
                  WHEN "0100" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0100";
                  WHEN "0101" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "1000";
                  WHEN "0110" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "1001";
                  WHEN "0111" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "1010";
                  WHEN "1000" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "1011";
                  WHEN "1001" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "1100";
                  WHEN "0000" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= "0000";
               END CASE;
            END PROCESS ADD3_{{proc}};
            """)
        .get();
  }
}