/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.shapes;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Graphics;
import java.awt.geom.GeneralPath;
import java.util.List;
import java.util.Random;
import lombok.Getter;
import lombok.val;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class Poly extends FillableCanvasObject {
  @Getter private final boolean closed;
  // "handles" should be immutable - create a new array and change using
  // setHandles rather than changing contents
  private Handle[] handles;
  private GeneralPath path;
  private double[] lens;
  @Getter private Bounds bounds;

  public Poly(boolean closed, List<Location> locations) {
    val hs = new Handle[locations.size()];
    var i = -1;
    for (val loc : locations) {
      i++;
      hs[i] = new Handle(this, loc.getX(), loc.getY());
    }

    this.closed = closed;
    handles = hs;
    recomputeBounds();
  }

  @Override
  public Handle canDeleteHandle(Location loc) {
    var minHandles = closed ? 3 : 2;
    if (handles.length > minHandles) {
      val qx = loc.getX();
      val qy = loc.getY();
      val w = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
      for (val handle : handles) {
        int hx = handle.getX();
        int hy = handle.getY();
        if (LineUtil.distance(qx, qy, hx, hy) < w * w) {
          return handle;
        }
      }
    }
    return null;
  }

  @Override
  public Handle canInsertHandle(Location loc) {
    val result = PolyUtil.getClosestPoint(loc, closed, handles);
    val thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
    if (result.getDistanceSq() >= thresh * thresh) {
      return null;
    }
    val resLoc = result.getLocation();
    return (result.getPreviousHandle().isAt(resLoc) || result.getNextHandle().isAt(resLoc))
        ? null
        : new Handle(this, result.getLocation());
  }

  @Override
  public boolean canMoveHandle(Handle handle) {
    return true;
  }

  /**
   * Clone function taken from Cornell's version of Logisim:
   * http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  @Override
  public Poly clone() {
    var ret = (Poly) super.clone();
    val hs = this.handles.clone();

    for (int i = 0, n = hs.length; i < n; ++i) {
      val oldHandle = hs[i];
      hs[i] = new Handle(ret, oldHandle.getX(), oldHandle.getY());
    }
    ret.handles = hs;
    return ret;
  }

  @Override
  public final boolean contains(Location loc, boolean assumeFilled) {
    var type = getPaintType();
    if (assumeFilled && type == DrawAttr.PAINT_STROKE) {
      type = DrawAttr.PAINT_STROKE_FILL;
    }
    if (type == DrawAttr.PAINT_STROKE) {
      var thresh = Math.max(Line.ON_LINE_THRESH, getStrokeWidth() / 2);
      return PolyUtil.getClosestPoint(loc, closed, handles).getDistanceSq() < thresh * thresh;
    } else if (type == DrawAttr.PAINT_FILL) {
      return getPath().contains(loc.getX(), loc.getY());
    } else { // fill and stroke
      if (getPath().contains(loc.getX(), loc.getY())) return true;
      val width = getStrokeWidth();
      return PolyUtil.getClosestPoint(loc, closed, handles).getDistanceSq() < (width * width) / 4;
    }
  }

  @Override
  public Handle deleteHandle(Handle handle) {
    val hs = handles;
    val n = hs.length;
    val is = new Handle[n - 1];
    Handle previous = null;
    var deleted = false;
    for (var i = 0; i < n; i++) {
      if (deleted) {
        is[i - 1] = hs[i];
      } else if (hs[i].equals(handle)) {
        if (previous == null) {
          previous = hs[n - 1];
        }
        deleted = true;
      } else {
        previous = hs[i];
        is[i] = hs[i];
      }
    }
    setHandles(is);
    return previous;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(getPaintType());
  }

  @Override
  public String getDisplayName() {
    return closed ? S.get("shapePolygon") : S.get("shapePolyline");
  }

  @Override
  public List<Handle> getHandles(HandleGesture gesture) {
    val hs = handles;
    if (gesture == null) {
      return UnmodifiableList.create(hs);
    }

    val g = gesture.getHandle();
    val n = hs.length;
    val ret = new Handle[n];
    for (int i = 0; i < n; i++) {
      val h = hs[i];
      if (h.equals(g)) {
        val x = h.getX() + gesture.getDeltaX();
        val y = h.getY() + gesture.getDeltaY();
        Location r;
        if (gesture.isShiftDown()) {
          var prev = hs[(i + n - 1) % n].getLocation();
          var next = hs[(i + 1) % n].getLocation();
          if (!closed) {
            if (i == 0) prev = null;
            if (i == n - 1) next = null;
          }
          if (prev == null) {
            r = LineUtil.snapTo8Cardinals(next, x, y);
          } else if (next == null) {
            r = LineUtil.snapTo8Cardinals(prev, x, y);
          } else {
            val to = Location.create(x, y);
            val a = LineUtil.snapTo8Cardinals(prev, x, y);
            val b = LineUtil.snapTo8Cardinals(next, x, y);
            val ad = a.manhattanDistanceTo(to);
            val bd = b.manhattanDistanceTo(to);
            r = ad < bd ? a : b;
          }
        } else {
          r = Location.create(x, y);
        }
        ret[i] = new Handle(this, r);
      } else {
        ret[i] = h;
      }
    }
    return UnmodifiableList.create(ret);
  }

  private GeneralPath getPath() {
    var p = path;
    if (p == null) {
      p = new GeneralPath();
      val hs = handles;
      if (hs.length > 0) {
        var first = true;
        for (val handle : hs) {
          if (first) {
            p.moveTo(handle.getX(), handle.getY());
            first = false;
          } else {
            p.lineTo(handle.getX(), handle.getY());
          }
        }
      }
      path = p;
    }
    return p;
  }

  private Location getRandomBoundaryPoint(Bounds bds, Random rand) {
    val hs = handles;
    var ls = lens;
    if (ls == null) {
      ls = new double[hs.length + (closed ? 1 : 0)];
      var total = 0.0D;
      for (var i = 0; i < ls.length; i++) {
        val j = (i + 1) % hs.length;
        total += LineUtil.distance(hs[i].getX(), hs[i].getY(), hs[j].getX(), hs[j].getY());
        ls[i] = total;
      }
      lens = ls;
    }
    val pos = ls[ls.length - 1] * rand.nextDouble();
    for (var i = 0; true; i++) {
      if (pos < ls[i]) {
        val p = hs[i];
        val q = hs[(i + 1) % hs.length];
        val u = Math.random();
        val x = (int) Math.round(p.getX() + u * (q.getX() - p.getX()));
        val y = (int) Math.round(p.getY() + u * (q.getY() - p.getY()));
        return Location.create(x, y);
      }
    }
  }

  @Override
  public final Location getRandomPoint(Bounds bds, Random rand) {
    if (getPaintType() == DrawAttr.PAINT_STROKE) {
      var ret = getRandomBoundaryPoint(bds, rand);
      val w = getStrokeWidth();
      if (w > 1) {
        val dx = rand.nextInt(w) - w / 2;
        val dy = rand.nextInt(w) - w / 2;
        ret = ret.translate(dx, dy);
      }
      return ret;
    } else {
      return super.getRandomPoint(bds, rand);
    }
  }

  @Override
  public void insertHandle(Handle desired, Handle previous) {
    val loc = desired.getLocation();
    val hs = handles;
    Handle prev;
    if (previous == null) {
      prev = PolyUtil.getClosestPoint(loc, closed, hs).getPreviousHandle();
    } else {
      prev = previous;
    }
    val is = new Handle[hs.length + 1];
    var inserted = false;
    for (var i = 0; i < hs.length; i++) {
      if (inserted) {
        is[i + 1] = hs[i];
      } else if (hs[i].equals(prev)) {
        inserted = true;
        is[i] = hs[i];
        is[i + 1] = desired;
      } else {
        is[i] = hs[i];
      }
    }
    if (!inserted) {
      throw new IllegalArgumentException("no such handle");
    }
    setHandles(is);
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (!(other instanceof Poly)) {
      return false;
    }

    val that = (Poly) other;
    val a = this.handles;
    val b = that.handles;
    if (this.closed != that.closed || a.length != b.length) {
      return false;
    }
    for (int i = 0, n = a.length; i < n; i++) {
      if (!a[i].equals(b[i])) {
        return false;
      }
    }
    return super.matches(that);
  }

  @Override
  public int matchesHashCode() {
    var ret = super.matchesHashCode();
    ret = ret * 3 + (closed ? 1 : 0);
    val hs = handles;
    for (val handle : hs) {
      ret = ret * 31 + handle.hashCode();
    }
    return ret;
  }

  @Override
  public Handle moveHandle(HandleGesture gesture) {
    val hs = getHandles(gesture);
    val is = new Handle[hs.size()];
    Handle ret = null;
    var i = -1;
    for (val handle : hs) {
      i++;
      is[i] = handle;
    }
    setHandles(is);
    return ret;
  }

  @Override
  public void paint(Graphics g, HandleGesture gesture) {
    val hs = getHandles(gesture);
    val xs = new int[hs.size()];
    val ys = new int[hs.size()];
    var i = -1;
    for (val handle : hs) {
      i++;
      xs[i] = handle.getX();
      ys[i] = handle.getY();
    }

    if (setForFill(g)) {
      g.fillPolygon(xs, ys, xs.length);
    }
    if (setForStroke(g)) {
      if (closed) g.drawPolygon(xs, ys, xs.length);
      else g.drawPolyline(xs, ys, xs.length);
    }
  }

  private void recomputeBounds() {
    val hs = handles;
    var x0 = hs[0].getX();
    var y0 = hs[0].getY();
    var x1 = x0;
    var y1 = y0;
    for (var i = 1; i < hs.length; i++) {
      val x = hs[i].getX();
      val y = hs[i].getY();
      if (x < x0) x0 = x;
      if (x > x1) x1 = x;
      if (y < y0) y0 = y;
      if (y > y1) y1 = y;
    }
    val bds = Bounds.create(x0, y0, x1 - x0 + 1, y1 - y0 + 1);
    val stroke = getStrokeWidth();
    bounds = stroke < 2 ? bds : bds.expand(stroke / 2);
  }

  private void setHandles(Handle[] hs) {
    handles = hs;
    lens = null;
    path = null;
    recomputeBounds();
  }

  @Override
  public Element toSvgElement(Document doc) {
    return SvgCreator.createPoly(doc, this);
  }

  @Override
  public void translate(int dx, int dy) {
    val hs = handles;
    val is = new Handle[hs.length];
    for (var i = 0; i < hs.length; i++) {
      is[i] = new Handle(this, hs[i].getX() + dx, hs[i].getY() + dy);
    }
    setHandles(is);
  }
}
