/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

@DefaultQualifier(value = Nullable.class, locations = TypeUseLocation.OTHERWISE)
public class OutputStreamBinarySanitizer extends OutputStream {
  protected final Writer out;

  public OutputStreamBinarySanitizer(Writer out) {
    this.out = out;
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  @Override
  public void flush() throws IOException {
    out.flush();
  }

  @SuppressWarnings("checkstyle:AvoidEscapedUnicodeCharacters")
  @Override
  public void write(int c) throws IOException {
    if ((0x20 <= c && c <= 0x7E) || c == '\t' || c == '\n' || c == '\r') out.write((char) c);
    else out.write('\uFFFD');
  }
}
