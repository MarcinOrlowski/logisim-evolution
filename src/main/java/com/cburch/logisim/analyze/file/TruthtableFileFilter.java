/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.file;

import com.cburch.logisim.util.StringGetter;
import java.io.File;
import javax.swing.filechooser.FileFilter;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class TruthtableFileFilter extends FileFilter {

  final StringGetter description;
  final String extention;

  public TruthtableFileFilter(StringGetter descr, String ext) {
    description = descr;
    extention = ext;
  }

  @Override
  public boolean accept(File f) {
    if (!f.isFile()) return true;
    final var name = f.getName();
    final var i = name.lastIndexOf('.');
    return (i > 0 && name.substring(i).toLowerCase().equals(extention));
  }

  @Override
  public String getDescription() {
    return description.toString();
  }
}
