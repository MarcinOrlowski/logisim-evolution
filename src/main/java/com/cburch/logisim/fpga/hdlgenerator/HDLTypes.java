/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HDLTypes {

  private interface HDLType {
    public String getTypeDefinition();
    public String getTypeName();
  }

  private class HDLEnum implements HDLType {
    private final List<String> myEntries = new ArrayList<>();
    private final String myTypeName;

    public HDLEnum(String name) {
      myTypeName = name;
    }

    public HDLEnum add(String entry) {
      for (var item = 0; item < myEntries.size(); item++) 
        if (myEntries.get(item).compareTo(entry) > 0) {
          myEntries.add(item, entry);
          return this;
        }
      myEntries.add(entry);
      return this;
    }

    public String getTypeDefinition() {
      final var contents = new StringBuffer();
      if (HDL.isVHDL())
        contents.append(String.format("TYPE %s IS ( ", myTypeName));
      else
        contents.append("typedef enum { ");
      var first = true;
      for (final var entry : myEntries) {
        if (first)
          first = false;
        else
          contents.append(", ");
        contents.append(entry);
      }
      if (HDL.isVHDL())
        contents.append(");");
      else
        contents.append(String.format("} %s;", myTypeName));
      return contents.toString();
    }
    
    public String getTypeName() {
      return myTypeName;
    }
  }

  private class HDLArray implements HDLType {
    private final String myTypeName;
    private final String myGenericBitWidth;
    private final int myBitWidth;
    private final int myNrOfEntries;
    
    public HDLArray(String name, String genericBitWidth, int nrOfEntries) {
      myTypeName = name;
      myGenericBitWidth = genericBitWidth;
      myBitWidth = -1;
      myNrOfEntries = nrOfEntries;
    }

    public HDLArray(String name, int nrOfBits, int nrOfEntries) {
      myTypeName = name;
      myGenericBitWidth = null;
      myBitWidth = nrOfBits;
      myNrOfEntries = nrOfEntries;
    }

    public String getTypeDefinition() {
      final var contents = new StringBuffer();
      if (HDL.isVHDL()) {
        contents.append(String.format("Type %s IS ARRAY( %d DOWNTO 0 ) OF std_logic_vector( ", myTypeName, myNrOfEntries))
            .append(myGenericBitWidth == null ? Integer.toString(myBitWidth - 1) : String.format("%s - 1", myGenericBitWidth))
            .append(" DOWNTO 0);");
      } else {
        contents.append("typedef logic [")
            .append(myGenericBitWidth == null ? Integer.toString(myBitWidth - 1) : String.format("%s - 1", myGenericBitWidth))
            .append(String.format(":0] %s [%d:0];", myTypeName, myNrOfEntries));
      }
      return contents.toString();
    }

    public String getTypeName() {
      return myTypeName;
    }
  }
  
  private final Map<Integer, HDLType> myTypes = new HashMap<>();
  private final Map<String, Integer> myWires = new HashMap<>();

  public HDLTypes addEnum(int identifier, String name) {
    myTypes.put(identifier, new HDLEnum(name));
    return this;
  }
  
  public HDLTypes addEnumEntry(int identifier, String entry) {
    if (!myTypes.containsKey(identifier)) throw new IllegalArgumentException("Enum type not contained in array");
    final var myEnum = (HDLEnum) myTypes.get(identifier);
    myEnum.add(entry);
    return this;
  }

  public HDLTypes addArray(int identifier, String name, String genericBitWidth, int nrOfEntries) {
    myTypes.put(identifier, new HDLArray(name, genericBitWidth, nrOfEntries));
    return this;
  }

  public HDLTypes addArray(int identifier, String name, int nrOfBits, int nrOfEntries) {
    myTypes.put(identifier, new HDLArray(name, nrOfBits, nrOfEntries));
    return this;
  }
  
  public HDLTypes addWire(String name, int typeIdentifier) {
    myWires.put(name, typeIdentifier);
    return this;
  }

  public int getNrOfTypes() {
    return myTypes.keySet().size();
  }

  public List<String> getTypeDefinitions() {
    final var defs = new ArrayList<String>();
    for (final var entry : myTypes.keySet())
      defs.add(myTypes.get(entry).getTypeDefinition());
    return defs;
  }

  public Map<String, String> getTypedWires() {
    final var contents = new HashMap<String, String>();
    for (final var wire : myWires.keySet()) {
      final var typeId = myWires.get(wire);
      if (!myTypes.containsKey(typeId)) throw new IllegalArgumentException("Enum or array type not contained in array");
      contents.put(wire, myTypes.get(typeId).getTypeName());
    }
    return contents;
  }

  public void clear() {
    myTypes.clear();
    myWires.clear();
  }
}