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

import com.cburch.draw.toolbar.Toolbar;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.file.LibraryEvent;
import com.cburch.logisim.file.LibraryListener;
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.appear.AppearanceView;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.generic.AttrTableModel;
import com.cburch.logisim.gui.generic.BasicZoomModel;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CardPanel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.generic.RegTabContent;
import com.cburch.logisim.gui.generic.ZoomControl;
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.gui.menu.MainMenuListener;
import com.cburch.logisim.Main;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.HorizontalSplitPane;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.util.VerticalSplitPane;
import com.cburch.logisim.vhdl.base.HdlModel;
import com.cburch.logisim.vhdl.gui.HdlContentView;
import com.cburch.logisim.vhdl.gui.VhdlSimState;
import com.cburch.logisim.vhdl.gui.VhdlSimulatorConsole;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import lombok.Getter;
import lombok.val;

public class Frame extends LFrame.MainWindow implements LocaleListener {
  public static final String EDITOR_VIEW = "editorView";
  public static final String EXPLORER_VIEW = "explorerView";
  public static final String EDIT_LAYOUT = "layout";
  public static final String EDIT_APPEARANCE = "appearance";
  public static final String EDIT_HDL = "hdl";
  private static final long serialVersionUID = 1L;
  private final Timer timer = new Timer();
  private final Project project;
  private final MyProjectListener myProjectListener = new MyProjectListener();
  // GUI elements shared between views
  private final MainMenuListener menuListener;
  private final Toolbar toolbar;
  private final HorizontalSplitPane leftRegion;
  private final HorizontalSplitPane rightRegion;
  private final HorizontalSplitPane editRegion;
  private final VerticalSplitPane mainRegion;
  private final JPanel rightPanel;
  private final JPanel mainPanelSuper;
  private final CardPanel mainPanel;

  // left-side elements
  private final JTabbedPane topTab;
  private final JTabbedPane bottomTab;
  private final Toolbox toolbox;
  private final SimulationExplorer simExplorer;
  private final AttrTable attrTable;

  @Getter private final ZoomControl zoomControl;

  // for the Layout view
  private final LayoutToolbarModel layoutToolbarModel;
  private final Canvas layoutCanvas;

  @Getter private final VhdlSimulatorConsole vhdlSimulatorConsole;
  private final HdlContentView hdlEditor;
  @Getter private final ZoomModel zoomModel;
  private final LayoutEditHandler layoutEditHandler;
  private final AttrTableSelectionModel attrTableSelectionModel;
  // for the Appearance view
  private AppearanceView appearance;
  private Double lastFraction = AppPreferences.WINDOW_RIGHT_SPLIT.get();

  public Frame(Project project) {
    super(project);
    this.project = project;

    setBackground(Color.white);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new MyWindowListener());

    project.addProjectListener(myProjectListener);
    project.addLibraryListener(myProjectListener);
    project.addCircuitListener(myProjectListener);

    // set up elements for the Layout view
    layoutToolbarModel = new LayoutToolbarModel(this, project);
    layoutCanvas = new Canvas(project);
    val canvasPane = new CanvasPane(layoutCanvas);

    zoomModel =
        new BasicZoomModel(
            AppPreferences.LAYOUT_SHOW_GRID,
            AppPreferences.LAYOUT_ZOOM,
            buildZoomSteps(),
            canvasPane);

    layoutCanvas.getGridPainter().setZoomModel(zoomModel);
    layoutEditHandler = new LayoutEditHandler(this);
    attrTableSelectionModel = new AttrTableSelectionModel(project, this);

    // set up menu bar and toolbar
    menuListener = new MainMenuListener(this, menubar);
    menuListener.setEditHandler(layoutEditHandler);
    toolbar = new Toolbar(layoutToolbarModel);

    // set up the left-side components
    toolbox = new Toolbox(project, this, menuListener);
    simExplorer = new SimulationExplorer(project, menuListener);
    bottomTab = new JTabbedPane();
    bottomTab.setFont(AppPreferences.getScaledFont(new Font("Dialog", Font.BOLD, 9)));
    bottomTab.add(attrTable = new AttrTable(this));
    bottomTab.add(new RegTabContent(this));

    zoomControl = new ZoomControl(zoomModel, layoutCanvas);

    // set up the central area
    mainPanelSuper = new JPanel(new BorderLayout());
    canvasPane.setZoomModel(zoomModel);
    mainPanel = new CardPanel();
    mainPanel.addView(EDIT_LAYOUT, canvasPane);
    mainPanel.setView(EDIT_LAYOUT);
    mainPanelSuper.add(mainPanel, BorderLayout.CENTER);

    // set up the contents, split down the middle, with the canvas
    // on the right and a split pane on the left containing the
    // explorer and attribute values.
    val explPanel = new JPanel(new BorderLayout());
    explPanel.add(toolbox, BorderLayout.CENTER);

    val simPanel = new JPanel(new BorderLayout());
    // simPanel.add(new JButton("stuff"), BorderLayout.NORTH);
    simPanel.add(simExplorer, BorderLayout.CENTER);

    topTab = new JTabbedPane();
    topTab.setFont(new Font("Dialog", Font.BOLD, 9));
    topTab.add(explPanel);
    topTab.add(simPanel);

    val attrFooter = new JPanel(new BorderLayout());
    attrFooter.add(zoomControl);

    val bottomTabAndZoom = new JPanel(new BorderLayout());
    bottomTabAndZoom.add(bottomTab, BorderLayout.CENTER);
    bottomTabAndZoom.add(attrFooter, BorderLayout.SOUTH);
    leftRegion = new HorizontalSplitPane(topTab, bottomTabAndZoom, AppPreferences.WINDOW_LEFT_SPLIT.get());

    hdlEditor = new HdlContentView(project);
    vhdlSimulatorConsole = new VhdlSimulatorConsole(project);
    editRegion = new HorizontalSplitPane(mainPanelSuper, hdlEditor, 1.0);
    rightRegion = new HorizontalSplitPane(editRegion, vhdlSimulatorConsole, 1.0);

    rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(rightRegion, BorderLayout.CENTER);

    val state = new VhdlSimState(project);
    state.stateChanged();
    project.getVhdlSimulator().addVhdlSimStateListener(state);

    mainRegion =
        new VerticalSplitPane(
            leftRegion, rightPanel, AppPreferences.WINDOW_MAIN_SPLIT.get());

    getContentPane().add(mainRegion, BorderLayout.CENTER);

    localeChanged();

    this.setSize(
        AppPreferences.WINDOW_WIDTH.get(),
        AppPreferences.WINDOW_HEIGHT.get());
    val prefPoint = getInitialLocation();
    if (prefPoint != null) {
      this.setLocation(prefPoint);
    }
    this.setExtendedState(AppPreferences.WINDOW_STATE.get());

    menuListener.register(mainPanel);
    KeyboardToolSelection.register(toolbar);

    project.setFrame(this);
    if (project.getTool() == null) {
      project.setTool(project.getOptions().getToolbarData().getFirstTool());
    }
    mainPanel.addChangeListener(myProjectListener);
    AppPreferences.TOOLBAR_PLACEMENT.addPropertyChangeListener(myProjectListener);
    placeToolbar();

    LocaleManager.addLocaleListener(this);
    toolbox.updateStructure();
  }

  /**
   * Computes allowed zoom steps.
   *
   * @return
   */
  private ArrayList<Double> buildZoomSteps() {
    class Pair {
      int maxZoom;
      int singleStep;

      Pair(int maxZoom, int step) {
        this.maxZoom = maxZoom;
        this.singleStep = step;
      }
    }
    // Pairs must be in acending order (sorted by maxZoom value).
    val config = new Pair[] {new Pair(50, 5), new Pair(200, 10), new Pair(1000, 20)};

    // Result zoomsteps.
    final var steps = new ArrayList<Double>();

    double zoom = 0;
    for (val pair : config) {
      while (zoom < pair.maxZoom) {
        zoom += pair.singleStep;
        steps.add(zoom);
      }
    }
    return steps;
  }

  private static Point getInitialLocation() {
    val s = AppPreferences.WINDOW_LOCATION.get();
    if (s == null) {
      return null;
    }
    final var comma = s.indexOf(',');
    if (comma < 0) {
      return null;
    }
    try {
      var x = Integer.parseInt(s.substring(0, comma));
      var y = Integer.parseInt(s.substring(comma + 1));
      while (isProjectFrameAt(x, y)) {
        x += 20;
        y += 20;
      }
      Rectangle desired = new Rectangle(x, y, 50, 50);

      var gcBestSize = 0;
      Point gcBestPoint = null;
      val ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
      for (val gd : ge.getScreenDevices()) {
        for (val gc : gd.getConfigurations()) {
          val gcBounds = gc.getBounds();
          if (gcBounds.intersects(desired)) {
            val inter = gcBounds.intersection(desired);
            val size = inter.width * inter.height;
            if (size > gcBestSize) {
              gcBestSize = size;
              val x2 = Math.max(gcBounds.x, Math.min(inter.x, inter.x + inter.width - 50));
              val y2 = Math.max(gcBounds.y, Math.min(inter.y, inter.y + inter.height - 50));
              gcBestPoint = new Point(x2, y2);
            }
          }
        }
      }
      if (gcBestPoint != null) {
        if (isProjectFrameAt(gcBestPoint.x, gcBestPoint.y)) {
          gcBestPoint = null;
        }
      }
      return gcBestPoint;
    } catch (Throwable t) {
      return null;
    }
  }

  private static boolean isProjectFrameAt(int x, int y) {
    for (val current : Projects.getOpenProjects()) {
      val frame = current.getFrame();
      if (frame != null) {
        val loc = frame.getLocationOnScreen();
        val d = Math.abs(loc.x - x) + Math.abs(loc.y - y);
        if (d <= 3) {
          return true;
        }
      }
    }
    return false;
  }

  public void resetLayout() {
    mainRegion.setFraction(0.25);
    leftRegion.setFraction(0.5);
    rightRegion.setFraction(1.0);
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  private void buildTitleString() {
    val circuit = project.getCurrentCircuit();
    val name = project.getLogisimFile().getName();

    val title =
        (circuit != null)
            ? S.get("titleCircFileKnown", circuit.getName(), name)
            : S.get("titleFileKnown", name);

    var dirtyMarkerState = "";
    var dirtyMarker = "";

    if (project.isFileDirty()) {
      dirtyMarker = Main.DIRTY_MARKER + "\u0020"; // keep the space
      // The icon alone may sometimes be missed so we add additional "[UNSAVED]" to the title too.
      dirtyMarkerState = StringUtil.format("[%s]", S.get("titleUnsavedProjectState").toUpperCase());
    }

    this.setTitle(StringUtil.format("%s%s · %s %s", dirtyMarker, title, Main.APP_DISPLAY_NAME, dirtyMarkerState).trim());
    myProjectListener.enableSave();
  }

  public boolean confirmClose() {
    return confirmClose(S.get("confirmCloseTitle"));
  }

  // returns true if user is OK with proceeding
  public boolean confirmClose(String title) {
    val message = S.get("confirmDiscardMessage", project.getLogisimFile().getName());

    if (!project.isFileDirty()) {
      return true;
    }

    toFront();
    String[] options = {S.get("saveOption"), S.get("discardOption"), S.get("cancelOption")};
    val result =
        OptionPane.showOptionDialog(
            this, message, title, 0, OptionPane.QUESTION_MESSAGE, null, options, options[0]);
    boolean ret;
    if (result == 0) {
      ret = ProjectActions.doSave(project);
    } else if (result == 1) {
      // Close the current project
      dispose();
      ret = true;
    } else {
      ret = false;
    }

    return ret;
  }

  public Canvas getCanvas() {
    return layoutCanvas;
  }

  public String getEditorView() {
    return (getHdlEditorView() != null ? EDIT_HDL : mainPanel.getView());
  }

  public void setEditorView(String view) {
    val curView = mainPanel.getView();
    if (hdlEditor.getHdlModel() == null && curView.equals(view)) {
      return;
    }
    editRegion.setFraction(1.0);
    hdlEditor.setHdlModel(null);

    if (view.equals(EDIT_APPEARANCE)) { // appearance view
      var app = appearance;
      if (app == null) {
        app = new AppearanceView();
        app.setCircuit(project, project.getCircuitState());
        mainPanel.addView(EDIT_APPEARANCE, app.getCanvasPane());
        appearance = app;
      }
      toolbar.setToolbarModel(app.getToolbarModel());
      app.getAttrTableDrawManager(attrTable).attributesSelected();
      zoomControl.setZoomModel(app.getZoomModel());
      zoomControl.setAutoZoomButtonEnabled(false);
      menuListener.setEditHandler(app.getEditHandler());
      mainPanel.setView(view);
      app.getCanvas().requestFocus();
    } else { // layout view
      toolbar.setToolbarModel(layoutToolbarModel);
      zoomControl.setZoomModel(zoomModel);
      zoomControl.setAutoZoomButtonEnabled(true);
      menuListener.setEditHandler(layoutEditHandler);
      viewAttributes(project.getTool(), true);
      mainPanel.setView(view);
      layoutCanvas.requestFocus();
    }
  }

  @Override
  public void localeChanged() {
    buildTitleString();
    topTab.setTitleAt(0, S.get("designTab"));
    topTab.setTitleAt(1, S.get("simulateTab"));
    bottomTab.setTitleAt(0, S.get("propertiesTab"));
    bottomTab.setTitleAt(1, S.get("stateTab"));
  }

  private void placeToolbar() {
    val loc = AppPreferences.TOOLBAR_PLACEMENT.get();
    rightPanel.remove(toolbar);
    if (!AppPreferences.TOOLBAR_HIDDEN.equals(loc)) {
      Object value = BorderLayout.NORTH;
      for (val dir : Direction.cardinals) {
        if (dir.toString().equals(loc)) {
          if (dir == Direction.EAST) {
            value = BorderLayout.EAST;
          } else if (dir == Direction.SOUTH) {
            value = BorderLayout.SOUTH;
          } else if (dir == Direction.WEST) {
            value = BorderLayout.WEST;
          } else {
            value = BorderLayout.NORTH;
          }
        }
      }
      rightPanel.add(toolbar, value);
      val vertical = value == BorderLayout.WEST || value == BorderLayout.EAST;
      toolbar.setOrientation(vertical ? Toolbar.VERTICAL : Toolbar.HORIZONTAL);
    }
    getContentPane().validate();
  }

  public void savePreferences() {
    AppPreferences.TICK_FREQUENCY.set(project.getSimulator().getTickFrequency());
    AppPreferences.LAYOUT_SHOW_GRID.setBoolean(zoomModel.getShowGrid());
    AppPreferences.LAYOUT_ZOOM.set(zoomModel.getZoomFactor());
    if (appearance != null) {
      val appearanceZoom = appearance.getZoomModel();
      AppPreferences.APPEARANCE_SHOW_GRID.setBoolean(appearanceZoom.getShowGrid());
      AppPreferences.APPEARANCE_ZOOM.set(appearanceZoom.getZoomFactor());
    }
    val state = getExtendedState() & ~JFrame.ICONIFIED;
    AppPreferences.WINDOW_STATE.set(state);
    val dim = getSize();
    AppPreferences.WINDOW_WIDTH.set(dim.width);
    AppPreferences.WINDOW_HEIGHT.set(dim.height);
    Point loc;
    try {
      loc = getLocationOnScreen();
    } catch (IllegalComponentStateException e) {
      loc = Projects.getLocation(this);
    }
    if (loc != null) AppPreferences.WINDOW_LOCATION.set(loc.x + "," + loc.y);

    if (leftRegion.getFraction() > 0) AppPreferences.WINDOW_LEFT_SPLIT.set(leftRegion.getFraction());
    if (rightRegion.getFraction() < 1.0) AppPreferences.WINDOW_RIGHT_SPLIT.set(rightRegion.getFraction());
    if (mainRegion.getFraction() > 0) AppPreferences.WINDOW_MAIN_SPLIT.set(mainRegion.getFraction());
    AppPreferences.DIALOG_DIRECTORY.set(JFileChoosers.getCurrentDirectory());
  }

  void setAttrTableModel(AttrTableModel value) {
    attrTable.setAttrTableModel(value);
    if (value instanceof AttrTableToolModel) {
      val tool = ((AttrTableToolModel) value).getTool();
      toolbox.setHaloedTool(tool);
      layoutToolbarModel.setHaloedTool(tool);
    } else {
      toolbox.setHaloedTool(null);
      layoutToolbarModel.setHaloedTool(null);
    }
    if (value instanceof AttrTableComponentModel) {
      val circ = ((AttrTableComponentModel) value).getCircuit();
      val comp = ((AttrTableComponentModel) value).getComponent();
      layoutCanvas.setHaloedComponent(circ, comp);
    } else {
      layoutCanvas.setHaloedComponent(null, null);
    }
  }

  public void setVhdlSimulatorConsoleStatus(boolean visible) {
    if (visible) {
      rightRegion.setFraction(lastFraction);
    } else {
      lastFraction = rightRegion.getFraction();
      rightRegion.setFraction(1);
    }
  }

  public HdlModel getHdlEditorView() {
    return hdlEditor.getHdlModel();
  }

  private void setHdlEditorView(HdlModel hdl) {
    hdlEditor.setHdlModel(hdl);
    zoomControl.setZoomModel(null);
    editRegion.setFraction(0.0);
    toolbar.setToolbarModel(hdlEditor.getToolbarModel());
  }

  void viewAttributes(Tool newTool) {
    viewAttributes(null, newTool, false);
  }

  private void viewAttributes(Tool newTool, boolean force) {
    viewAttributes(null, newTool, force);
  }

  private void viewAttributes(Tool oldTool, Tool newTool, boolean force) {
    AttributeSet newAttrs;
    if (newTool == null) {
      newAttrs = null;
      if (!force) {
        return;
      }
    } else {
      newAttrs = newTool.getAttributeSet(layoutCanvas);
    }
    if (newAttrs == null) {
      val oldModel = attrTable.getAttrTableModel();
      val same =
          oldModel instanceof AttrTableToolModel
              && ((AttrTableToolModel) oldModel).getTool() == oldTool;
      if (!force && !same && !(oldModel instanceof AttrTableCircuitModel)) {
        return;
      }
    }
    if (newAttrs == null) {
      val circ = project.getCurrentCircuit();
      if (circ != null) {
        setAttrTableModel(new AttrTableCircuitModel(project, circ));
      } else if (force) {
        setAttrTableModel(null);
      }
    } else if (newAttrs instanceof SelectionAttributes) {
      setAttrTableModel(attrTableSelectionModel);
    } else {
      setAttrTableModel(new AttrTableToolModel(project, newTool));
    }
  }

  public void viewComponentAttributes(Circuit circ, Component comp) {
    if (comp == null) {
      setAttrTableModel(null);
    } else {
      setAttrTableModel(new AttrTableComponentModel(project, circ, comp));
    }
  }

  class MyProjectListener implements ProjectListener, LibraryListener, CircuitListener, PropertyChangeListener, ChangeListener {
    public void attributeListChanged(AttributeEvent e) {}

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
        buildTitleString();
      }
    }

    private void enableSave() {
      val ok = getProject().isFileDirty();
      getRootPane().putClientProperty("windowModified", ok);
    }

    @Override
    public void libraryChanged(LibraryEvent e) {
      if (e.getAction() == LibraryEvent.SET_NAME) {
        buildTitleString();
      } else if (e.getAction() == LibraryEvent.DIRTY_STATE) {
        buildTitleString();
        enableSave();
      }
    }

    @Override
    public void projectChanged(ProjectEvent event) {
      val action = event.getAction();
      if (action == ProjectEvent.ACTION_SET_FILE) {
        buildTitleString();
        project.setTool(project.getOptions().getToolbarData().getFirstTool());
        placeToolbar();
      } else if (action == ProjectEvent.ACTION_SET_STATE) {
        if (event.getData() instanceof CircuitState) {
          val state = (CircuitState) event.getData();
          if (state.getParentState() != null) topTab.setSelectedIndex(1); // sim explorer view
        }
      } else if (action == ProjectEvent.ACTION_SET_CURRENT) {
        if (event.getData() instanceof Circuit) {
          setEditorView(EDIT_LAYOUT);
          if (appearance != null) {
            appearance.setCircuit(project, project.getCircuitState());
          }
        } else if (event.getData() instanceof HdlModel) {
          setHdlEditorView((HdlModel) event.getData());
        }
        viewAttributes(project.getTool());
        buildTitleString();
      } else if (action == ProjectEvent.ACTION_SET_TOOL) {
        if (attrTable == null) {
          return; // for startup
        }
        val oldTool = (Tool) event.getOldData();
        val newTool = (Tool) event.getData();
        if (!getEditorView().equals(EDIT_APPEARANCE)) {
          viewAttributes(oldTool, newTool, false);
        }
      }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
      if (AppPreferences.TOOLBAR_PLACEMENT.isSource(event)) {
        placeToolbar();
      }
    }

    @Override
    public void stateChanged(ChangeEvent event) {
      val source = event.getSource();
      if (source == mainPanel) {
        firePropertyChange(EDITOR_VIEW, "???", getEditorView());
      }
    }
  }

  class MyWindowListener extends WindowAdapter {

    @Override
    public void windowClosing(WindowEvent e) {
      if (confirmClose(S.get("confirmCloseTitle"))) {
        layoutCanvas.closeCanvas();
        timer.cancel();
        Frame.this.dispose();
      }
    }

    @Override
    public void windowOpened(WindowEvent e) {
      layoutCanvas.computeSize(true);
    }
  }
}
