/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.gui;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.actions.ModelChangeAttributeAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.AttributeSetTableModel;
import java.util.HashMap;
import java.util.Map;
import lombok.val;

class AttrTableSelectionModel extends AttributeSetTableModel implements SelectionListener {
  private final Canvas canvas;

  public AttrTableSelectionModel(Canvas canvas) {
    super(new SelectionAttributes(canvas.getSelection()));
    this.canvas = canvas;
    canvas.getSelection().addSelectionListener(this);
  }

  @Override
  public String getTitle() {
    val sel = canvas.getSelection();
    Class<? extends CanvasObject> commonClass = null;
    var commonCount = 0;
    CanvasObject firstObject = null;
    var totalCount = 0;
    for (val obj : sel.getSelected()) {
      if (firstObject == null) {
        firstObject = obj;
        commonClass = obj.getClass();
        commonCount = 1;
      } else if (obj.getClass() == commonClass) {
        commonCount++;
      } else {
        commonClass = null;
      }
      totalCount++;
    }

    if (firstObject == null) {
      return null;
    } else if (commonClass == null) {
      return S.get("selectionVarious", "" + totalCount);
    } else if (commonCount == 1) {
      return firstObject.getDisplayNameAndLabel();
    }

    return S.get("selectionMultiple", firstObject.getDisplayName(), String.valueOf(commonCount));
  }

  //
  // SelectionListener method
  //
  @Override
  public void selectionChanged(SelectionEvent e) {
    fireTitleChanged();
  }

  @Override
  public void setValueRequested(Attribute<Object> attr, Object value) {
    val attrs = (SelectionAttributes) getAttributeSet();
    val oldVals = new HashMap<AttributeMapKey, Object>();
    val newVals = new HashMap<AttributeMapKey, Object>();
    for (val ent : attrs.entries()) {
      val key = new AttributeMapKey(attr, ent.getValue());
      oldVals.put(key, ent.getKey().getValue(attr));
      newVals.put(key, value);
    }
    val model = canvas.getModel();
    canvas.doAction(new ModelChangeAttributeAction(model, oldVals, newVals));
    fireTitleChanged();
  }
}
