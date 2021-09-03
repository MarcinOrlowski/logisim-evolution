/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * 
 * https://github.com/logisim-evolution/
 * 
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import lombok.Data;

@Data
public class CaretEvent {
  private final Caret caret;
  private final String oldText;
  private final String text;
}
