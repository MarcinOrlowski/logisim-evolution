/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.contracts.BaseMouseInputListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.file.MouseMappings;
import com.cburch.logisim.file.Options;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CanvasPaneContents;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.EditTool;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.vhdl.base.HdlModel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JViewport;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class Canvas extends JPanel implements LocaleListener, CanvasPaneContents, AdjustmentListener {

  public static final byte ZOOM_BUTTON_SIZE = 52;
  public static final byte ZOOM_BUTTON_MARGIN = 30;
  public static final Color HALO_COLOR = new Color(255, 0, 255);
  // don't bother to update the size if it hasn't changed more than this
  static final double SQRT_2 = Math.sqrt(2.0);
  private static final long serialVersionUID = 1L;
  // pixels shown in canvas beyond outermost boundaries
  private static final int THRESH_SIZE_UPDATE = 10;
  private static final int BUTTONS_MASK =
    InputEvent.BUTTON1_DOWN_MASK | InputEvent.BUTTON2_DOWN_MASK | InputEvent.BUTTON3_DOWN_MASK;
  private static final Color DEFAULT_ERROR_COLOR = new Color(192, 0, 0);
  private static final Color OSC_ERR_COLOR = DEFAULT_ERROR_COLOR;
  private static final Color SIM_EXCEPTION_COLOR = DEFAULT_ERROR_COLOR;
  private static final Font ERR_MSG_FONT = new Font("Sans Serif", Font.BOLD, 18);
  private static final Color TICK_RATE_COLOR = new Color(0, 0, 92, 92);
  private static final Font TICK_RATE_FONT = new Font("Monospaced", Font.PLAIN, 28);
  private static final Color SINGLE_STEP_MSG_COLOR = Color.BLUE;
  private static final Font SINGLE_STEP_MSG_FONT = new Font("Sans Serif", Font.BOLD, 12);
  public static final Color DEFAULT_ZOOM_BUTTON_COLOR = Color.WHITE;
  // public static BufferedImage image;
  private final Project project;
  private final Selection selection;
  private final MyListener myListener = new MyListener();
  private final MyViewport viewport = new MyViewport();
  private final MyProjectListener myProjectListener = new MyProjectListener();
  private final TickCounter tickCounter = new TickCounter();
  private final CanvasPaintThread paintThread;
  private final CanvasPainter painter;
  private final Object repaintLock = new Object(); // for waitForRepaintDone
  private Tool dragTool;
  private Tool tempTool;
  private MouseMappings mappings;
  private CanvasPane canvasPane = null;
  private Bounds oldPreferredSize = null;
  private volatile boolean paintDirty = false; // only for within paintComponent
  private boolean inPaint = false; // only for within paintComponent

  public Canvas(Project project) {
    this.project = project;
    this.selection = new Selection(project, this);
    this.painter = new CanvasPainter(this);
    this.paintThread = new CanvasPaintThread(this);
    this.mappings = project.getOptions().getMouseMappings();

    setBackground(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
    addMouseListener(myListener);
    addMouseMotionListener(myListener);
    addMouseWheelListener(myListener);
    addKeyListener(myListener);

    project.addProjectListener(myProjectListener);
    project.addLibraryListener(myProjectListener);
    project.addCircuitListener(myProjectListener);
    project.getSimulator().addSimulatorListener(tickCounter);
    selection.addListener(myProjectListener);
    LocaleManager.addLocaleListener(this);

    final var options = project.getOptions().getAttributeSet();
    options.addAttributeListener(myProjectListener);
    AppPreferences.COMPONENT_TIPS.addPropertyChangeListener(myListener);
    AppPreferences.GATE_SHAPE.addPropertyChangeListener(myListener);
    AppPreferences.SHOW_TICK_RATE.addPropertyChangeListener(myListener);
    AppPreferences.CANVAS_BG_COLOR.addPropertyChangeListener(myListener);
    loadOptions(options);
    paintThread.start();
  }

  public static boolean autoZoomButtonClicked(Dimension sz, double x, double y) {
    return Point2D.distance(
      x,
      y,
      sz.width - ZOOM_BUTTON_SIZE / 2 - ZOOM_BUTTON_MARGIN,
      sz.height - ZOOM_BUTTON_MARGIN - ZOOM_BUTTON_SIZE / 2)
      <= ZOOM_BUTTON_SIZE / 2;
  }

  public static void paintAutoZoomButton(Graphics g, Dimension sz, Color zoomButtonColor) {
    final var oldColor = g.getColor();
    g.setColor(TICK_RATE_COLOR);
    g.fillOval(
      sz.width - ZOOM_BUTTON_SIZE - 33,
      sz.height - ZOOM_BUTTON_SIZE - 33,
      ZOOM_BUTTON_SIZE + 6,
      ZOOM_BUTTON_SIZE + 6);
    g.setColor(zoomButtonColor);
    g.fillOval(
      sz.width - ZOOM_BUTTON_SIZE - 30,
      sz.height - ZOOM_BUTTON_SIZE - 30,
      ZOOM_BUTTON_SIZE,
      ZOOM_BUTTON_SIZE);
    g.setColor(Value.unknownColor);
    GraphicsUtil.switchToWidth(g, 3);
    int width = sz.width - ZOOM_BUTTON_MARGIN;
    int height = sz.height - ZOOM_BUTTON_MARGIN;
    g.drawOval(
      width - ZOOM_BUTTON_SIZE * 3 / 4,
      height - ZOOM_BUTTON_SIZE * 3 / 4,
      ZOOM_BUTTON_SIZE / 2,
      ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
      width - ZOOM_BUTTON_SIZE / 4 + 4,
      height - ZOOM_BUTTON_SIZE / 2,
      width - ZOOM_BUTTON_SIZE * 3 / 4 - 4,
      height - ZOOM_BUTTON_SIZE / 2);
    g.drawLine(
      width - ZOOM_BUTTON_SIZE / 2,
      height - ZOOM_BUTTON_SIZE / 4 + 4,
      width - ZOOM_BUTTON_SIZE / 2,
      height - ZOOM_BUTTON_SIZE * 3 / 4 - 4);
    g.setColor(oldColor);
  }

  public static void snapToGrid(MouseEvent e) {
    final var oldX = e.getX();
    final var oldY = e.getY();
    final var newX = snapXToGrid(oldX);
    final var newY = snapYToGrid(oldY);
    e.translatePoint(newX - oldX, newY - oldY);
  }

  //
  // static methods
  //
  public static int snapXToGrid(int x) {
    return snapTo(x);
  }

  public static int snapYToGrid(int y) {
    return snapTo(y);
  }

  protected static int snapTo(int val) {
    return (val < 0)
           ? -((-val + 5) / 10) * 10
           : ((val + 5) / 10) * 10;
  }

  public CanvasPane getCanvasPane() {
    return canvasPane;
  }

  //
  // CanvasPaneContents methods
  //
  @Override
  public void setCanvasPane(CanvasPane value) {
    canvasPane = value;
    canvasPane.setViewport(viewport);
    canvasPane.getHorizontalScrollBar().addAdjustmentListener(this);
    canvasPane.getVerticalScrollBar().addAdjustmentListener(this);
    viewport.setView(this);
    setOpaque(false);
    computeSize(true);
  }

  @Override
  public void center() {
    final var gfx = getGraphics();
    final var bounds = (gfx != null)
                       ? project.getCurrentCircuit().getBounds(getGraphics())
                       : project.getCurrentCircuit().getBounds();

    if (bounds.getHeight() == 0 || bounds.getWidth() == 0) {
      setScrollBar(0, 0);
      return;
    }

    final var xpos =
      (int)
        (Math.round(
          bounds.getX() * getZoomFactor()
            - (canvasPane.getViewport().getSize().getWidth()
            - bounds.getWidth() * getZoomFactor())
            / 2));

    final var ypos =
      (int)
        (Math.round(
          bounds.getY() * getZoomFactor()
            - (canvasPane.getViewport().getSize().getHeight()
            - bounds.getHeight() * getZoomFactor())
            / 2));
    setScrollBar(xpos, ypos);
  }

  public void closeCanvas() {
    paintThread.requestStop();
  }

  private void completeAction() {
    if (project.getCurrentCircuit() == null) return;
    computeSize(false);
    // After any interaction, nudge the simulator, which in autoPropagate mode
    // will (if needed) eventually, fire a propagateCompleted event, which will
    // cause a repaint. If not in autoPropagate mode, do the repaint here
    // instead.
    if (!project.getSimulator().nudge()) paintThread.requestRepaint();
  }

  public void computeSize(boolean immediate) {
    if (project.getCurrentCircuit() == null) return;
    final var gfx = getGraphics();
    final var bounds = (gfx != null)
                       ? project.getCurrentCircuit().getBounds(getGraphics())
                       : project.getCurrentCircuit().getBounds();
    var height = 0;
    var width = 0;
    if (bounds != null && viewport != null) {
      width = bounds.getX() + bounds.getWidth() + viewport.getWidth();
      height = bounds.getY() + bounds.getHeight() + viewport.getHeight();
    }
    final var dim = (canvasPane == null)
                    ? new Dimension(width, height)
                    : canvasPane.supportPreferredSize(width, height);
    if (!immediate) {
      final var old = oldPreferredSize;
      if (old != null
        && Math.abs(old.getWidth() - dim.width) < THRESH_SIZE_UPDATE
        && Math.abs(old.getHeight() - dim.height) < THRESH_SIZE_UPDATE) {
        return;
      }
    }
    oldPreferredSize = Bounds.create(0, 0, dim.width, dim.height);
    setPreferredSize(dim);
    revalidate();
  }

  public Rectangle getViewableRect() {
    final Rectangle viewableBase;
    if (canvasPane != null) {
      viewableBase = canvasPane.getViewport().getViewRect();
    } else {
      final var bds = project.getCurrentCircuit().getBounds();
      viewableBase = new Rectangle(0, 0, bds.getWidth(), bds.getHeight());
    }
    double zoom = getZoomFactor();

    final Rectangle viewable;
    if (zoom == 1.0) {
      viewable = viewableBase;
    } else {
      viewable =
        new Rectangle(
          (int) (viewableBase.x / zoom),
          (int) (viewableBase.y / zoom),
          (int) (viewableBase.width / zoom),
          (int) (viewableBase.height / zoom));
    }
    return viewable;
  }

  private void computeViewportContents() {
    final var exceptions = project.getCurrentCircuit().getWidthIncompatibilityData();
    if (exceptions == null || exceptions.size() == 0) {
      viewport.setWidthMessage(null);
      return;
    }
    viewport.setWidthMessage(
      S.get("canvasWidthError") + (exceptions.size() == 1 ? "" : " (" + exceptions.size() + ")"));
    for (final var ex : exceptions) {
      final var point = ex.getPoint(0);
      setArrows(point.getX(), point.getY(), point.getX(), point.getY());
    }
  }

  //
  // access methods
  //
  public Circuit getCircuit() {
    return project.getCurrentCircuit();
  }

  public HdlModel getCurrentHdl() {
    return project.getCurrentHdl();
  }

  public CircuitState getCircuitState() {
    return project.getCircuitState();
  }

  Tool getDragTool() {
    return dragTool;
  }

  public StringGetter getErrorMessage() {
    return viewport.errorMessage;
  }

  public void setErrorMessage(StringGetter message) {
    viewport.setErrorMessage(message, null);
  }

  public void setErrorMessage(StringGetter message, Color color) {
    viewport.setErrorMessage(message, color);
  }

  GridPainter getGridPainter() {
    return painter.getGridPainter();
  }

  Component getHaloedComponent() {
    return painter.getHaloedComponent();
  }

  public int getHorizontalScrollBar() {
    return canvasPane.getHorizontalScrollBar().getValue();
  }

  public int getVerticalScrollBar() {
    return canvasPane.getVerticalScrollBar().getValue();
  }

  public void setVerticalScrollBar(int posY) {
    canvasPane.getVerticalScrollBar().setValue(posY);
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  public Project getProject() {
    return project;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return canvasPane.supportScrollableBlockIncrement(visibleRect, orientation, direction);
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return canvasPane.supportScrollableUnitIncrement(visibleRect, orientation, direction);
  }

  public Selection getSelection() {
    return selection;
  }

  @Override
  public String getToolTipText(MouseEvent event) {
    final var showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
    if (!showTips) {
      return null;
    }

    Canvas.snapToGrid(event);
    final var loc = Location.create(event.getX(), event.getY(), false);
    ComponentUserEvent userEvent = null;
    for (final var comp : getCircuit().getAllContaining(loc)) {
      final var makerObj = comp.getFeature(ToolTipMaker.class);
      if (makerObj instanceof ToolTipMaker maker) {
        if (userEvent == null) {
          userEvent = new ComponentUserEvent(this, loc.getX(), loc.getY());
        }
        final var ret = maker.getToolTip(userEvent);
        if (ret != null) {
          unrepairMouseEvent(event);
          return ret;
        }
      }
    }

    return null;
  }

  //
  // graphics methods
  //
  double getZoomFactor() {
    final var pane = canvasPane;
    return pane == null ? 1.0 : pane.getZoomFactor();
  }

  boolean ifPaintDirtyReset() {
    if (paintDirty) {
      paintDirty = false;
      return false;
    } else {
      return true;
    }
  }

  boolean isPopupMenuUp() {
    return myListener.menuOn;
  }

  private void loadOptions(AttributeSet options) {
    final var showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
    setToolTipText(showTips ? "" : null);

    project.getSimulator().removeSimulatorListener(myProjectListener);
    project.getSimulator().addSimulatorListener(myProjectListener);
  }

  @Override
  public void localeChanged() {
    paintThread.requestRepaint();
  }

  @Override
  public void paintComponent(Graphics gfx) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      final var gfx2 = (Graphics2D) gfx;
      gfx2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      gfx2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    inPaint = true;
    try {
      super.paintComponent(gfx);
      var clear = false;
      do {
        if (clear) {
          /* Kevin Walsh:
           * Clear the screen so we don't get
           * artifacts due to aliasing (e.g. where
           * semi-transparent (gray) pixels on the
           * edges of a line turn would darker if
           * painted a second time.
           */
          gfx.setColor(Color.WHITE);
          gfx.fillRect(0, 0, getWidth(), getHeight());
        }
        clear = true;
        painter.paintContents(gfx, project);
      } while (paintDirty);
      if (canvasPane == null) {
        viewport.paintContents(gfx);
      }
    } finally {
      inPaint = false;
      synchronized (repaintLock) {
        repaintLock.notifyAll();
      }
    }
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    repairMouseEvent(e);
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    repairMouseEvent(e);
    super.processMouseMotionEvent(e);
  }

  @Override
  public void recomputeSize() {
    computeSize(true);
  }

  @Override
  public void repaint() {
    if (inPaint) {
      paintDirty = true;
    } else {
      super.repaint();
    }
  }

  @Override
  public void repaint(int x, int y, int width, int height) {
    final var zoom = getZoomFactor();
    if (zoom < 1.0) {
      final var newX = (int) Math.floor(x * zoom);
      final var newY = (int) Math.floor(y * zoom);
      width += x - newX;
      height += y - newY;
      x = newX;
      y = newY;
    } else if (zoom > 1.0) {
      final var x1 = (int) Math.ceil((x + width) * zoom);
      final var y1 = (int) Math.ceil((y + height) * zoom);
      width = x1 - x;
      height = y1 - y;
    }
    super.repaint(x, y, width, height);
  }

  @Override
  public void repaint(Rectangle r) {
    final var zoom = getZoomFactor();
    if (zoom == 1.0) {
      super.repaint(r);
    } else {
      this.repaint(r.x, r.y, r.width, r.height);
    }
  }

  private void repairMouseEvent(MouseEvent e) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      zoomEvent(e, zoom);
    }
  }

  public void updateArrows() {
    /* Disable for VHDL content */
    if (project.getCurrentCircuit() == null) return;
    final var gfx = getGraphics();
    final var circBds = (gfx != null)
                        ? project.getCurrentCircuit().getBounds(getGraphics())
                        : project.getCurrentCircuit().getBounds();
    // no circuit
    if (circBds == null || circBds.getHeight() == 0 || circBds.getWidth() == 0) {
      return;
    }

    var x = circBds.getX();
    var y = circBds.getY();
    if (x < 0) x = 0;
    if (y < 0) y = 0;
    setArrows(x, y, x + circBds.getWidth(), y + circBds.getHeight());
  }

  public void setArrows(int x0, int y0, int x1, int y1) {
    /* Disable for VHDL content */
    if (project.getCurrentCircuit() == null) return;
    viewport.clearArrows();
    Rectangle viewableBase;
    if (canvasPane != null) {
      viewableBase = canvasPane.getViewport().getViewRect();
    } else {
      final var gfx = getGraphics();
      final var bds = (gfx != null)
                      ? project.getCurrentCircuit().getBounds(getGraphics())
                      : project.getCurrentCircuit().getBounds();
      viewableBase = new Rectangle(0, 0, bds.getWidth(), bds.getHeight());
    }
    final var zoom = getZoomFactor();
    Rectangle viewable;
    if (zoom == 1.0) {
      viewable = viewableBase;
    } else {
      viewable =
        new Rectangle(
          (int) (viewableBase.x / zoom),
          (int) (viewableBase.y / zoom),
          (int) (viewableBase.width / zoom),
          (int) (viewableBase.height / zoom));
    }
    final var isWest = x0 < viewable.x;
    final var isEast = x1 >= viewable.x + viewable.width;
    final var isNorth = y0 < viewable.y;
    final var isSouth = y1 >= viewable.y + viewable.height;

    if (isNorth) {
      if (isEast) viewport.setNortheast(true);
      if (isWest) viewport.setNorthwest(true);
      if (!isWest && !isEast) viewport.setNorth(true);
    }
    if (isSouth) {
      if (isEast) viewport.setSoutheast(true);
      if (isWest) viewport.setSouthwest(true);
      if (!isWest && !isEast) viewport.setSouth(true);
    }
    if (isEast && !viewport.isSoutheast && !viewport.isNortheast) viewport.setEast(true);
    if (isWest && !viewport.isSouthwest && !viewport.isNorthwest) viewport.setWest(true);
  }

  void setHaloedComponent(Circuit circ, Component comp) {
    painter.setHaloedComponent(circ, comp);
  }

  public void setHighlightedWires(WireSet value) {
    painter.setHighlightedWires(value);
  }

  public void setHorizontalScrollBar(int posX) {
    canvasPane.getHorizontalScrollBar().setValue(posX);
  }

  public void setScrollBar(int posX, int posY) {
    setHorizontalScrollBar(posX);
    setVerticalScrollBar(posY);
  }

  public void showPopupMenu(JPopupMenu menu, int x, int y) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      x = (int) Math.round(x * zoom);
      y = (int) Math.round(y * zoom);
    }
    myListener.menuOn = true;
    menu.addPopupMenuListener(myListener);
    menu.show(this, x, y);
  }

  private void unrepairMouseEvent(MouseEvent e) {
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      zoomEvent(e, 1.0 / zoom);
    }
  }

  private void waitForRepaintDone() {
    synchronized (repaintLock) {
      try {
        while (inPaint) {
          repaintLock.wait();
        }
      } catch (InterruptedException ignored) {
      }
    }
  }

  private void zoomEvent(MouseEvent e, double zoom) {
    final var oldx = e.getX();
    final var oldy = e.getY();
    final var newx = (int) Math.round(e.getX() / zoom);
    final var newy = (int) Math.round(e.getY() / zoom);
    e.translatePoint(newx - oldx, newy - oldy);
  }

  @Override
  public void adjustmentValueChanged(AdjustmentEvent e) {
    updateArrows();
  }

  private class MyListener
    implements BaseMouseInputListenerContract,
               KeyListener,
               PopupMenuListener,
               PropertyChangeListener,
               MouseWheelListener {

    boolean menuOn = false;

    private Tool getToolFor(MouseEvent event) {
      if (menuOn) {
        return null;
      }

      final var tool = mappings.getToolFor(event);
      return (tool == null)
             ? project.getTool()
             : tool;
    }

    //
    // KeyListener methods
    //
    @Override
    public void keyPressed(KeyEvent e) {
      final var tool = project.getTool();
      if (tool != null) {
        tool.keyPressed(Canvas.this, e);
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
      final var tool = project.getTool();
      if (tool != null) {
        tool.keyReleased(Canvas.this, e);
      }
    }

    @Override
    public void keyTyped(KeyEvent e) {
      final var tool = project.getTool();
      if (tool != null) {
        tool.keyTyped(Canvas.this, e);
      }
    }

    //
    // MouseListener methods
    //
    @Override
    public void mouseClicked(MouseEvent mouseEvent) {
      // do nothing
    }

    @Override
    public void mouseDragged(MouseEvent event) {
      if (dragTool != null) {
        dragTool.mouseDragged(Canvas.this, getGraphics(), event);
        final var zoomModel = project.getFrame().getZoomModel();
        final var zoomFactor = zoomModel.getZoomFactor();
        scrollRectToVisible(
          new Rectangle((int) (event.getX() * zoomFactor), (int) (event.getY() * zoomFactor), 1, 1));
      }
    }

    @Override
    public void mouseEntered(MouseEvent event) {
      if (dragTool != null) {
        dragTool.mouseEntered(Canvas.this, getGraphics(), event);
      } else {
        final var tool = getToolFor(event);
        if (tool != null) {
          tool.mouseEntered(Canvas.this, getGraphics(), event);
        }
      }
    }

    @Override
    public void mouseExited(MouseEvent event) {
      if (dragTool != null) {
        dragTool.mouseExited(Canvas.this, getGraphics(), event);
      } else {
        final var tool = getToolFor(event);
        if (tool != null) {
          tool.mouseExited(Canvas.this, getGraphics(), event);
        }
      }
    }

    @Override
    public void mouseMoved(MouseEvent event) {
      if ((event.getModifiersEx() & BUTTONS_MASK) != 0) {
        // If the control key is down while the mouse is being
        // dragged, mouseMoved is called instead. This may well be
        // an issue specific to the MacOS Java implementation,
        // but it exists there in the 1.4 and 5.0 versions.
        mouseDragged(event);
        return;
      }

      final var tool = getToolFor(event);
      if (tool != null) {
        tool.mouseMoved(Canvas.this, getGraphics(), event);
      }
    }

    @Override
    public void mousePressed(MouseEvent event) {
      viewport.setErrorMessage(null, null);
      if (project.isStartupScreen()) {
        final var gfx = getGraphics();
        final var bounds = (gfx != null)
                           ? project.getCurrentCircuit().getBounds(getGraphics())
                           : project.getCurrentCircuit().getBounds();
        // set the project as dirty only if it contains something
        if (bounds.getHeight() != 0 || bounds.getWidth() != 0) {
          project.setStartupScreen(false);
        }
      }
      if (event.getButton() == MouseEvent.BUTTON1
        && viewport.zoomButtonVisible
        && autoZoomButtonClicked(
        viewport.getSize(),
        event.getX() * getZoomFactor() - getHorizontalScrollBar(),
        event.getY() * getZoomFactor() - getVerticalScrollBar())) {
        viewport.zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR.darker();
        viewport.repaint();
      } else {
        Canvas.this.requestFocus();
        dragTool = getToolFor(event);
        if (dragTool != null) {
          dragTool.mousePressed(Canvas.this, getGraphics(), event);
          if (event.getButton() != MouseEvent.BUTTON1) {
            tempTool = project.getTool();
            project.setTool(dragTool);
          }
        }
        completeAction();
      }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
      if ((event.getButton() == MouseEvent.BUTTON1
        && viewport.zoomButtonVisible
        && autoZoomButtonClicked(
        viewport.getSize(),
        event.getX() * getZoomFactor() - getHorizontalScrollBar(),
        event.getY() * getZoomFactor() - getVerticalScrollBar())
        && viewport.zoomButtonColor != DEFAULT_ZOOM_BUTTON_COLOR)
        || event.getButton() == MouseEvent.BUTTON2 && event.getClickCount() == 2) {
        center();
        setCursor(project.getTool().getCursor());
      }
      if (dragTool != null) {
        dragTool.mouseReleased(Canvas.this, getGraphics(), event);
        dragTool = null;
      }
      if (tempTool != null) {
        project.setTool(tempTool);
        tempTool = null;
      }
      final var tool = project.getTool();
      if (tool != null && !(tool instanceof EditTool)) {
        tool.mouseMoved(Canvas.this, getGraphics(), event);
        setCursor(tool.getCursor());
      }
      completeAction();

      viewport.zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      final var tool = project.getTool();
      if (mwe.isControlDown()) {
        var zoomControl = project.getFrame().getZoomControl();

        repairMouseEvent(mwe);
        if (mwe.getWheelRotation() < 0) {
          zoomControl.zoomIn();
        } else {
          zoomControl.zoomOut();
        }
        var rect = getViewableRect();
        var zoom = project.getFrame().getZoomModel().getZoomFactor();
        setHorizontalScrollBar((int) ((mwe.getX() - rect.width / 2) * zoom));
        setVerticalScrollBar((int) ((mwe.getY() - rect.height / 2) * zoom));

      } else if (tool instanceof PokeTool && ((PokeTool) tool).isScrollable()) {
        final var id = (mwe.getWheelRotation() < 0) ? KeyEvent.VK_UP : KeyEvent.VK_DOWN;
        final var e = new KeyEvent(mwe.getComponent(), KeyEvent.KEY_PRESSED, mwe.getWhen(), 0, id, '\0');
        tool.keyPressed(Canvas.this, e);
      } else {
        if (mwe.isShiftDown()) {
          canvasPane.getHorizontalScrollBar().setValue(scrollValue(canvasPane.getHorizontalScrollBar(), mwe.getWheelRotation()));
        } else {
          canvasPane.getVerticalScrollBar().setValue(scrollValue(canvasPane.getVerticalScrollBar(), mwe.getWheelRotation()));
        }
      }
    }

    //
    // PopupMenuListener mtehods
    //
    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
      menuOn = false;
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
      menuOn = false;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      // do nothing
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.GATE_SHAPE.isSource(event)
        || AppPreferences.SHOW_TICK_RATE.isSource(event)
        || AppPreferences.AntiAliassing.isSource(event)) {
        paintThread.requestRepaint();
      } else if (AppPreferences.COMPONENT_TIPS.isSource(event)) {
        final var showTips = AppPreferences.COMPONENT_TIPS.getBoolean();
        setToolTipText(showTips ? "" : null);
      } else if (AppPreferences.CANVAS_BG_COLOR.isSource(event)) {
        setBackground(new Color(AppPreferences.CANVAS_BG_COLOR.get()));
      }
    }

    private int scrollValue(JScrollBar bar, int val) {
      if (val > 0) {
        if (bar.getValue() < bar.getMaximum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      } else {
        if (bar.getValue() > bar.getMinimum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      }
      return 0;
    }
  }

  private class MyProjectListener implements ProjectListener, LibraryListener, CircuitListener,
                                             AttributeListener, Simulator.Listener, Selection.Listener {

    @Override
    public void attributeValueChanged(AttributeEvent event) {
      final var attr = event.getAttribute();
      if (attr == Options.ATTR_GATE_UNDEFINED) {
        final var circState = getCircuitState();
        circState.markComponentsDirty(getCircuit().getNonWires());
        // TODO actually, we'd want to mark all components in
        // subcircuits as dirty as well
      }
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      final var act = event.getAction();
      if (act == CircuitEvent.ACTION_REMOVE) {
        final var component = (Component) event.getData();
        if (component == painter.getHaloedComponent()) {
          project.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == CircuitEvent.ACTION_CLEAR) {
        if (painter.getHaloedComponent() != null) {
          project.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == CircuitEvent.ACTION_INVALIDATE) {
        completeAction();
      }
    }

    private Tool findTool(List<? extends Tool> opts) {
      Tool ret = null;
      for (final var tool : opts) {
        if (ret == null && tool != null) {
          ret = tool;
        } else if (tool instanceof EditTool) {
          ret = tool;
        }
      }
      return ret;
    }

    @Override
    public void libraryChanged(LibraryEvent event) {
      if (event.getAction() == LibraryEvent.REMOVE_TOOL) {
        Object t = event.getData();
        Circuit circuit = null;
        if (t instanceof AddTool) {
          t = ((AddTool) t).getFactory();
          if (t instanceof SubcircuitFactory subFact) {
            circuit = subFact.getSubcircuit();
          }
        }

        if (t == project.getCurrentCircuit() && t != null) {
          project.setCurrentCircuit(project.getLogisimFile().getMainCircuit());
        }

        if (project.getTool() == event.getData()) {
          var next = findTool(project.getLogisimFile().getOptions().getToolbarData().getContents());
          if (next == null) {
            for (final var lib : project.getLogisimFile().getLibraries()) {
              next = findTool(lib.getTools());
              if (next != null) {
                break;
              }
            }
          }
          project.setTool(next);
        }

        if (circuit != null) {
          var state = getCircuitState();
          var last = state;
          while (state != null && state.getCircuit() != circuit) {
            last = state;
            state = state.getParentState();
          }
          if (state != null) {
            getProject().setCircuitState(last.cloneState());
          }
        }
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      final var act = event.getAction();
      if (act == ProjectEvent.ACTION_SET_CURRENT) {
        viewport.setErrorMessage(null, null);
        if (painter.getHaloedComponent() != null) {
          project.getFrame().viewComponentAttributes(null, null);
        }
      } else if (act == ProjectEvent.ACTION_SET_FILE) {
        final var old = (LogisimFile) event.getOldData();
        if (old != null) {
          old.getOptions().getAttributeSet().removeAttributeListener(this);
        }
        final var file = (LogisimFile) event.getData();
        if (file != null) {
          final var attrs = file.getOptions().getAttributeSet();
          attrs.addAttributeListener(this);
          loadOptions(attrs);
          mappings = file.getOptions().getMouseMappings();
        }
      } else if (act == ProjectEvent.ACTION_SET_TOOL) {
        viewport.setErrorMessage(null, null);

        final var tool = event.getTool();
        if (tool == null) {
          setCursor(Cursor.getDefaultCursor());
        } else {
          setCursor(tool.getCursor());
        }
      } else if (act == ProjectEvent.ACTION_SET_STATE) {
        final var oldState = (CircuitState) event.getOldData();
        final var newState = (CircuitState) event.getData();
        if (oldState != null && newState != null) {
          final var oldProp = oldState.getPropagator();
          final var newProp = newState.getPropagator();
          if (oldProp != newProp) {
            tickCounter.clear();
          }
        }
      }

      if (act != ProjectEvent.ACTION_SELECTION
        && act != ProjectEvent.ACTION_START
        && act != ProjectEvent.UNDO_START) {
        completeAction();
      }
    }

    @Override
    public void selectionChanged(Selection.Event event) {
      repaint();
    }

    @Override
    public void propagationCompleted(Simulator.Event event) {
      paintThread.requestRepaint();
      if (event.didTick()) waitForRepaintDone();
    }

    @Override
    public void simulatorStateChanged(Simulator.Event event) {
      // do nothing
    }

    @Override
    public void simulatorReset(Simulator.Event event) {
      waitForRepaintDone();
    }
  }

  private class MyViewport extends JViewport {

    private static final long serialVersionUID = 1L;
    StringGetter errorMessage = null;
    String widthMessage = null;
    Color errorColor = DEFAULT_ERROR_COLOR;
    boolean isNorth = false;
    boolean isSouth = false;
    boolean isWest = false;
    boolean isEast = false;
    boolean isNortheast = false;
    boolean isNorthwest = false;
    boolean isSoutheast = false;
    boolean isSouthwest = false;
    boolean zoomButtonVisible = false;
    Color zoomButtonColor = DEFAULT_ZOOM_BUTTON_COLOR;

    MyViewport() {
      // dummy
    }

    void clearArrows() {
      isNorth = false;
      isSouth = false;
      isWest = false;
      isEast = false;
      isNortheast = false;
      isNorthwest = false;
      isSoutheast = false;
      isSouthwest = false;
    }

    @Override
    public Color getBackground() {
      return getView() == null ? super.getBackground() : getView().getBackground();
    }

    @Override
    public void paintChildren(Graphics g) {
      super.paintChildren(g);
      paintContents(g);
    }

    void paintContents(Graphics gfx) {
      /*
       * TODO this is for the SimulatorPrototype class int speed =
       * proj.getSimulator().getSimulationSpeed(); String speedStr; if
       * (speed >= 10000000) { speedStr = (speed / 1000000) + " MHz"; }
       * else if (speed >= 1000000) { speedStr = (speed / 100000) / 10.0 +
       * " MHz"; } else if (speed >= 10000) { speedStr = (speed / 1000) +
       * " kHz"; } else if (speed >= 10000) { speedStr = (speed / 100) /
       * 10.0 + " kHz"; } else { speedStr = speed + " Hz"; } FontMetrics
       * fm = g.getFontMetrics(); g.drawString(speedStr, getWidth() - 10 -
       * fm.stringWidth(speedStr), getHeight() - 10);
       */
      if (AppPreferences.AntiAliassing.getBoolean()) {
        final var g2 = (Graphics2D) gfx;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }

      var msgY = getHeight() - 23;
      final var message = errorMessage;
      if (message != null) {
        gfx.setColor(errorColor);
        msgY = paintString(gfx, msgY, message.toString());
      }

      if (project.getSimulator().isOscillating()) {
        gfx.setColor(OSC_ERR_COLOR);
        msgY = paintString(gfx, msgY, S.get("canvasOscillationError"));
      }

      if (project.getSimulator().isExceptionEncountered()) {
        gfx.setColor(SIM_EXCEPTION_COLOR);
        msgY = paintString(gfx, msgY, S.get("canvasExceptionError"));
      }

      computeViewportContents();
      final var sz = getSize();

      if (widthMessage != null) {
        gfx.setColor(Value.widthErrorColor);
        msgY = paintString(gfx, msgY, widthMessage);
      } else {
        gfx.setColor(TICK_RATE_COLOR);
      }

      if (isNorth
        || isSouth
        || isEast
        || isWest
        || isNortheast
        || isNorthwest
        || isSoutheast
        || isSouthwest) {
        zoomButtonVisible = true;
        paintAutoZoomButton(gfx, getSize(), zoomButtonColor);

        if (isNorth) {
          GraphicsUtil.drawArrow2(
            gfx, sz.width / 2 - 20, 15, sz.width / 2, 5, sz.width / 2 + 20, 15);
        }
        if (isSouth) {
          GraphicsUtil.drawArrow2(
            gfx, sz.width / 2 - 20, sz.height - 15, sz.width / 2, sz.height - 5, sz.width / 2 + 20, sz.height - 15);
        }
        if (isEast) {
          GraphicsUtil.drawArrow2(
            gfx, sz.width - 15, sz.height / 2 + 20, sz.width - 5, sz.height / 2, sz.width - 15, sz.height / 2 - 20);
        }
        if (isWest) {
          GraphicsUtil.drawArrow2(
            gfx, 15, sz.height / 2 + 20, 5, sz.height / 2, 15, sz.height / 2 + (-20));
        }
        if (isNortheast) {
          GraphicsUtil.drawArrow2(
            gfx, sz.width - 30, 5, sz.width - 5, 5, sz.width - 5, 30);
        }
        if (isNorthwest) {
          GraphicsUtil.drawArrow2(gfx, 30, 5, 5, 5, 5, 30);
        }
        if (isSoutheast) {
          GraphicsUtil.drawArrow2(
            gfx, sz.width - 30, sz.height - 5, sz.width - 5, sz.height - 5, sz.width - 5, sz.height - 30);
        }
        if (isSouthwest) {
          GraphicsUtil.drawArrow2(
            gfx, 30, sz.height - 5, 5, sz.height - 5, 5, sz.height - 30);
        }
      } else {
        zoomButtonVisible = false;
      }

      if (AppPreferences.SHOW_TICK_RATE.getBoolean()) {
        final var hz = tickCounter.getTickRate();
        if (hz != null && !hz.equals("")) {
          final var fm = gfx.getFontMetrics();
          final var x = 10;
          final var y = fm.getAscent() + 10;

          gfx.setColor(new Color(AppPreferences.CLOCK_FREQUENCY_COLOR.get()));
          gfx.setFont(TICK_RATE_FONT);
          gfx.drawString(hz, x, y);
        }
      }

      if (!project.getSimulator().isAutoPropagating()) {
        gfx.setColor(SINGLE_STEP_MSG_COLOR);
        final var oldFont = gfx.getFont();
        gfx.setFont(SINGLE_STEP_MSG_FONT);
        gfx.drawString(project.getSimulator().getSingleStepMessage(), 10, 15);
        gfx.setFont(oldFont);
      }

      gfx.setColor(Color.BLACK);
    }

    private int paintString(Graphics gfx, int y, String msg) {
      final var old = gfx.getFont();
      gfx.setFont(ERR_MSG_FONT);
      var fm = gfx.getFontMetrics();
      var x = (getWidth() - fm.stringWidth(msg)) / 2;
      if (x < 0) {
        x = 0;
      }
      gfx.drawString(msg, x, y);
      gfx.setFont(old);

      return y - 23;
    }

    void setEast(boolean value) {
      isEast = value;
    }

    void setErrorMessage(StringGetter msg, Color color) {
      if (errorMessage != msg) {
        errorMessage = msg;
        errorColor = color == null ? DEFAULT_ERROR_COLOR : color;
        paintThread.requestRepaint();
      }
    }

    void setNorth(boolean value) {
      isNorth = value;
    }

    void setNortheast(boolean value) {
      isNortheast = value;
    }

    void setNorthwest(boolean value) {
      isNorthwest = value;
    }

    void setSouth(boolean value) {
      isSouth = value;
    }

    void setSoutheast(boolean value) {
      isSoutheast = value;
    }

    void setSouthwest(boolean value) {
      isSouthwest = value;
    }

    void setWest(boolean value) {
      isWest = value;
    }

    void setWidthMessage(String msg) {
      widthMessage = msg;
    }
  }
}
