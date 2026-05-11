import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.URI;
import java.net.URL;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.JTextComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.util.prefs.Preferences;


//Written by: Michael Zimmer - mike@zimmerdesignservices.com

/*
Copyright 2007-2011 Zimmer Design Services

This file is part of Fizzim.

Fizzim is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

Fizzim is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/* This file was originally created with matisse GUI Builder for MyEclipse.
 * Due to bugs and limitations, in is now being manually edited
 */
public class FizzimGui extends javax.swing.JFrame {

	private static final String APP_TITLE = "Fizzim 2.0";
	private static final int RECENT_FILE_LIMIT = 10;
	private static final String RECENT_FILE_PREFIX = "recentFile.";
	private static final String RECENT_PROJECT_PREFIX = "recentProject.";
	private static final String PREF_LAST_OPEN_TYPE = "lastOpenType";
	private static final String PREF_LAST_OPEN_PATH = "lastOpenPath";
	private static final String PREF_DEFAULT_CLOCK = "defaultClock";
	private static final String PREF_DEFAULT_CLOCK_EDGE = "defaultClockEdge";
	private static final String PREF_DEFAULT_RESET = "defaultReset";
	private static final String PREF_DEFAULT_RESET_EDGE = "defaultResetEdge";
	private static final String PREF_DEFAULT_IMPLIED_LOOPBACK = "defaultImpliedLoopback";
	private static final String PREF_HDL_PERL = "hdlPerlCommand";
	private static final String PREF_HDL_BACKEND = "hdlBackendPath";
	private static final String PREF_HDL_OUTPUT_DIR = "hdlOutputDir";
	private static final String PREF_HDL_USE_MODULE_FILENAME = "hdlUseModuleFilename";
	private static final String PREF_HDL_OUTPUT_FILENAME = "hdlOutputFilename";
	private static final String PREF_HDL_EXTRA_ARGS = "hdlExtraArgs";
	private static final String PREF_HDL_COMPARE_ENABLED = "hdlCompareEnabled";
	private static final String PREF_HDL_COMPARE_COMMAND = "hdlCompareCommand";
	private static final String PREF_HDL_COMPARE_BACKEND = "hdlCompareBackendPath";
	private static final String PREF_HDL_COMPARE_ARGS = "hdlCompareArgs";
	private static final String PREF_HDL_COMPARE_SUFFIX = "hdlCompareSuffix";
	private static final String HDL_STATE_ATTR = "fizzim2_hdl_generated";
	private static final String HDL_OUTPUT_ATTR = "fizzim2_hdl_output";
	private static final Preferences USER_PREFS = Preferences.userNodeForPackage(FizzimGui.class);
	private static int openWindowCount = 0;

	String currVer = "14.02.28";
	
	// pointer to global lists
	LinkedList<ObjAttribute> globalMachineAttributes;
	LinkedList<ObjAttribute> globalInputsAttributes;
	LinkedList<ObjAttribute> globalOutputsAttributes;
	LinkedList<ObjAttribute> globalStateAttributes;
	LinkedList<ObjAttribute> globalTransAttributes;

	LinkedList<LinkedList<ObjAttribute>> globalList;
	int maxH = 1296;
	int maxW = 936;
	boolean loading = false;
	private boolean zoomFitMode = false;
	private boolean hdlGeneratedInSync = false;
	private String lintStatusMode = "stale";
	private int lintErrorCount = 0;
	private int lintWarningCount = 0;
	private KeyEventDispatcher spaceFitDispatcher = null;
	


	/** Creates new form FizzimGui */

	public FizzimGui() {
		openWindowCount++;
		FizzimLocalizer.load(USER_PREFS);

		ImageIcon icon = new ImageIcon("icon.png");
		this.setIconImage(icon.getImage());
		// create global lists
		globalList = new LinkedList<LinkedList<ObjAttribute>>();
		globalMachineAttributes = new LinkedList<ObjAttribute>();
		globalInputsAttributes = new LinkedList<ObjAttribute>();
		globalOutputsAttributes = new LinkedList<ObjAttribute>();
		globalStateAttributes = new LinkedList<ObjAttribute>();
		globalTransAttributes = new LinkedList<ObjAttribute>();
		
		globalList.add(globalMachineAttributes);
		globalList.add(globalInputsAttributes);
		globalList.add(globalOutputsAttributes);
		globalList.add(globalStateAttributes);
		globalList.add(globalTransAttributes);
		

		drawArea1 = new DrawArea(globalList);

		drawArea1.setBackground(new java.awt.Color(255, 255, 255));
		drawArea1.setLogicalSize(maxW, maxH);

		initComponents();
		installSpaceFitShortcut();

		//custom initComponents

		drawArea1.setJFrame(this);
		initGlobal();


		drawArea1.updateStates();
		drawArea1.updateTrans();

	}

	private void initGlobal() {
		// set up required global attributes
                // 0=machine, 1=inputs, 2=outputs, 3=states, 4=trans
		int[] editable = { ObjAttribute.ABS, ObjAttribute.GLOBAL_VAR,
				ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR };
		int[] inputEditable = { ObjAttribute.GLOBAL_FIXED, ObjAttribute.GLOBAL_VAR,
				ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR };
		globalList.get(0).add(new ObjAttribute("name", "def_name", 0, "","",Color.black,"","",
				editable));
		globalList.get(0).add(new ObjAttribute("clock", getDefaultClockName(), 0,
				getDefaultClockEdge(),"",Color.black,"","",editable));
		globalList.get(0).add(new ObjAttribute("reset_signal", getDefaultResetName(), 0,
				getDefaultResetEdge(),"",Color.black,"","",editable));
		globalList.get(0).add(new ObjAttribute("reset_state", "state0", 0,
				"anyvalue","",Color.black,"","",editable));
		globalList.get(0).add(new ObjAttribute("implied_loopback", getDefaultImpliedLoopback() ? "1" : "0", 0,
				"attribute","",Color.black,"","",editable));

		globalList.get(1).add(new ObjAttribute(getDefaultClockName(), "", 0,
				"","",Color.black,"","",inputEditable));
		globalList.get(1).add(new ObjAttribute(getDefaultResetName(), "", 0,
				"","",Color.black,"","",inputEditable));

		globalList.get(3).add(new ObjAttribute("name", "def_name", 1,
				"def_type","",Color.black,"","",editable));

		globalList.get(4).add(new ObjAttribute("name", "def_name", 0,
				"def_type","",Color.black,"","",editable));

		globalList.get(4).add(new ObjAttribute("equation", "1", 1,
				"def_type","",Color.black,"","",editable));

		globalList.get(4).add(new ObjAttribute("priority", "1000", 1,
				"","",Color.black,"","",editable));
	}

	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
	
	

	//GEN-BEGIN:initComponents
	// <editor-fold defaultstate="collapsed" desc=" Generated Code ">
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		FileOpenAction = new MyJFileChooser("fzm");
		FileOpenAction.setMultiSelectionEnabled(true);
		ProjectOpenAction = new MyJFileChooser("fzp");
		ProjectOpenAction.setMultiSelectionEnabled(false);
		ProjectSaveAction = new MyJFileChooser("fzp");
		FileSaveAction = new MyJFileChooser("fzm");
		ExportChooser = new MyJFileChooser("png");
		jPanel3 = new javax.swing.JPanel();
		zoomPanel = new javax.swing.JPanel();
		zoomOutButton = new javax.swing.JButton();
		zoomInButton = new javax.swing.JButton();
		zoomFitButton = new javax.swing.JButton();
		lintButton = new javax.swing.JButton();
		generateHdlButton = new javax.swing.JButton();
		hdlStatusLabel = new javax.swing.JLabel();
		lintStatusLabel = new javax.swing.JLabel();
		zoomPercentLabel = new javax.swing.JLabel();
		selectionStatusLabel = new javax.swing.JLabel();
		propertyInspectorPanel = new javax.swing.JPanel();
		propertyInspectorTitle = new javax.swing.JLabel();
		propertyInspectorTable = new javax.swing.JTable();
		propertyInspectorScroll = new javax.swing.JScrollPane();
		propertyInspectorEditButton = new javax.swing.JButton();
		sideTabbedPane = new javax.swing.JTabbedPane();
		projectPanel = new javax.swing.JPanel();
		projectTitleLabel = new javax.swing.JLabel();
		projectTreeRoot = new DefaultMutableTreeNode("Project");
		projectTree = new javax.swing.JTree(projectTreeRoot);
		projectScroll = new javax.swing.JScrollPane();
		projectButtonPanel = new javax.swing.JPanel();
		projectOpenButton = new javax.swing.JButton();
		projectAddButton = new javax.swing.JButton();
		projectBuildButton = new javax.swing.JButton();
		projectLintButton = new javax.swing.JButton();
		lintPanel = new javax.swing.JPanel();
		lintIssueModel = new javax.swing.DefaultListModel();
		lintIssueList = new javax.swing.JList(lintIssueModel);
		lintReportText = new javax.swing.JTextArea();
		lintTabs = new javax.swing.JTabbedPane();
		lintRerunButton = new javax.swing.JButton();
		lintCloseButton = new javax.swing.JButton();
		jTabbedPane1 = new MyJTabbedPane();
		jScrollPane1 = new javax.swing.JScrollPane();
		jPanel1 = new javax.swing.JPanel();
		MenuBar = new javax.swing.JMenuBar();
		FileMenu = new javax.swing.JMenu();
		FileItemNew = new javax.swing.JMenuItem();
		FileItemOpen = new javax.swing.JMenuItem();
		FileOpenRecent = new javax.swing.JMenu();
		FileProjectMenu = new javax.swing.JMenu();
		FileProjectNew = new javax.swing.JMenuItem();
		FileProjectOpen = new javax.swing.JMenuItem();
		FileProjectOpenRecent = new javax.swing.JMenu();
		FileProjectSave = new javax.swing.JMenuItem();
		FileProjectSaveAs = new javax.swing.JMenuItem();
		FileProjectAddCurrent = new javax.swing.JMenuItem();
		FileProjectAddDiagrams = new javax.swing.JMenuItem();
		FileProjectBuildAll = new javax.swing.JMenuItem();
		FileProjectLintAll = new javax.swing.JMenuItem();
		FileItemSave = new javax.swing.JMenuItem();
		FileItemSaveAs = new javax.swing.JMenuItem();
		FileExport = new javax.swing.JMenu("Export to...");
		FileExportClipboard = new javax.swing.JMenuItem();
		FileExportPNG = new javax.swing.JMenuItem();
		FileExportJPEG = new javax.swing.JMenuItem();
		jSeparator1 = new javax.swing.JSeparator();
		FilePref = new javax.swing.JMenuItem();
		jSeparator2 = new javax.swing.JSeparator();
		FileItemExit = new javax.swing.JMenuItem();
		EditMenu = new javax.swing.JMenu();
		EditItemUndo = new javax.swing.JMenuItem();
		EditItemRedo = new javax.swing.JMenuItem();
		EditItemDelete = new javax.swing.JMenuItem();
		settingsMenu = new javax.swing.JMenu();
		defaultsItem = new javax.swing.JMenuItem();
		hdlSettingsItem = new javax.swing.JMenuItem();
		languageMenu = new javax.swing.JMenu();
		languageEnglishItem = new javax.swing.JRadioButtonMenuItem();
		languageJapaneseItem = new javax.swing.JRadioButtonMenuItem();
		languageChineseSimplifiedItem = new javax.swing.JRadioButtonMenuItem();
		languageChineseTraditionalItem = new javax.swing.JRadioButtonMenuItem();
		languageKoreanItem = new javax.swing.JRadioButtonMenuItem();
		languageGermanItem = new javax.swing.JRadioButtonMenuItem();
		languageFrenchItem = new javax.swing.JRadioButtonMenuItem();
		languageSpanishItem = new javax.swing.JRadioButtonMenuItem();
		languagePortugueseItem = new javax.swing.JRadioButtonMenuItem();
		languageHindiItem = new javax.swing.JRadioButtonMenuItem();
		languageRussianItem = new javax.swing.JRadioButtonMenuItem();
		toolsMenu = new javax.swing.JMenu();
		lintItem = new javax.swing.JMenuItem();
		generateHdlItem = new javax.swing.JMenuItem();
		cleanupMenu = new javax.swing.JMenu();
		resetLabelsItem = new javax.swing.JMenuItem();
		cleanRoutesItem = new javax.swing.JMenuItem();
		cleanSelectedRoutesItem = new javax.swing.JMenuItem();
		alignHorizontalItem = new javax.swing.JMenuItem();
		alignVerticalItem = new javax.swing.JMenuItem();
		distributeHorizontalItem = new javax.swing.JMenuItem();
		distributeVerticalItem = new javax.swing.JMenuItem();
		GlobalMenu = new javax.swing.JMenu();
		GlobalItemMachine = new javax.swing.JMenuItem();
		GlobalItemStates = new javax.swing.JMenuItem();
		GlobalItemTransitions = new javax.swing.JMenuItem();
		jSeparator3 = new javax.swing.JSeparator();
		GlobalItemInputs = new javax.swing.JMenuItem();
		GlobalItemParameters = new javax.swing.JMenuItem();
		GlobalItemOutputs = new javax.swing.JMenuItem();
		GlobalItemInternals = new javax.swing.JMenuItem();
		HelpMenu = new javax.swing.JMenu();
		HelpItemHelp = new javax.swing.JMenuItem();
		jSeparator4 = new javax.swing.JSeparator();
		HelpItemAbout = new javax.swing.JMenuItem();

		FileOpenAction.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileOpenActionActionPerformed(evt);
			}
		});

		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		updateWindowTitle();
		addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent evt) {
				formComponentResized(evt);
			}
		});
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});

		
		jPanel3.setLayout(new java.awt.GridBagLayout());

		jPanel3.setMinimumSize(new java.awt.Dimension(100, 100));
		jTabbedPane1.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
		//jTabbedPane1.setMinimumSize(getScrollPaneSize());
		//jTabbedPane1.setPreferredSize(new java.awt.Dimension(1000, 685));
		//jScrollPane1.setMaximumSize(new Dimension(maxW, maxH));
		//jScrollPane1.setMinimumSize(new Dimension(maxW, maxH));
		//jScrollPane1.setPreferredSize(new Dimension(maxW, maxH));
		jPanel1 = drawArea1;
		org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(
				jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout.setHorizontalGroup(jPanel1Layout.createParallelGroup(
				org.jdesktop.layout.GroupLayout.LEADING).add(0, 1294,
				Short.MAX_VALUE));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(
				org.jdesktop.layout.GroupLayout.LEADING).add(0, 1277,
				Short.MAX_VALUE));
		jScrollPane1.setViewportView(jPanel1);

		//pages
		/*

		System.err.println("test");
		
		try {
            throw new RuntimeException("Test");
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
		
		jTabbedPane1.addBlankTab("Create New Page", new JPanel());
		jTabbedPane1.addTab("Page 1", jScrollPane1);
		jTabbedPane1.setSelectedIndex(1);
		jTabbedPane1.setBackgroundAt(0, new Color(200,200,200));
		jTabbedPane1.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				TabChanged(e);
			}
		});

		jTabbedPane1.addMouseListener(new MouseListener(){

			public void mouseClicked(MouseEvent arg0) {	
				if(arg0.getButton() == MouseEvent.BUTTON3 || arg0.getModifiers() == 20)
				{
					JTabbedPane tabbedPane = (JTabbedPane)arg0.getSource();
					int tab = tabbedPane.indexAtLocation(arg0.getX(), arg0.getY());
					if(tab > 0)
						renameTab(tab);
				}
			}

			public void mouseEntered(MouseEvent arg0) {
			}

			public void mouseExited(MouseEvent arg0) {	
			}

			public void mousePressed(MouseEvent arg0) {		
			}

			public void mouseReleased(MouseEvent arg0) {			
			}
			
		});


		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		jPanel3.add(jTabbedPane1, gridBagConstraints);

		getContentPane().add(jPanel3, java.awt.BorderLayout.CENTER);

		buildSidePanel();
		getContentPane().add(sideTabbedPane, java.awt.BorderLayout.WEST);

		zoomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 4, 3));
		selectionStatusLabel.setText("No selection");
		selectionStatusLabel.setPreferredSize(new java.awt.Dimension(260, 22));
		selectionStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
		zoomPanel.add(selectionStatusLabel);
		hdlStatusLabel.setOpaque(true);
		hdlStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		hdlStatusLabel.setPreferredSize(new java.awt.Dimension(112, 22));
		hdlStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(140, 120, 40)));
		markHdlOutOfSync();
		zoomPanel.add(hdlStatusLabel);
		lintStatusLabel.setOpaque(true);
		lintStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		lintStatusLabel.setPreferredSize(new java.awt.Dimension(112, 22));
		lintStatusLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		lintStatusLabel.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				showLintPanel();
			}
		});
		markLintStale();
		zoomPanel.add(lintStatusLabel);
		zoomOutButton.setText("-");
		zoomOutButton.setToolTipText("Zoom out");
		zoomOutButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ZoomOutActionPerformed(evt);
			}
		});
		zoomPanel.add(zoomOutButton);
		zoomPercentLabel.setText("100%");
		zoomPercentLabel.setPreferredSize(new java.awt.Dimension(48, 22));
		zoomPercentLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		zoomPanel.add(zoomPercentLabel);
		zoomInButton.setText("+");
		zoomInButton.setToolTipText("Zoom in");
		zoomInButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ZoomInActionPerformed(evt);
			}
		});
		zoomPanel.add(zoomInButton);
		zoomFitButton.setText("Fit");
		zoomFitButton.setToolTipText("Zoom to fit the current page");
		zoomFitButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ZoomFitActionPerformed(evt);
			}
		});
		zoomPanel.add(zoomFitButton);
		lintButton.setText("Lint");
		lintButton.setToolTipText("Validate and lint the current diagram");
		lintButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsLintActionPerformed(evt);
			}
		});
		zoomPanel.add(lintButton);
		generateHdlButton.setText("Generate");
		generateHdlButton.setToolTipText("Generate HDL using the configured backend");
		generateHdlButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsGenerateHdlActionPerformed(evt);
			}
		});
		zoomPanel.add(generateHdlButton);
		getContentPane().add(zoomPanel, java.awt.BorderLayout.NORTH);

		buildLintPanel();
		getContentPane().add(lintPanel, java.awt.BorderLayout.SOUTH);
		
		
		jTabbedPane1.setMinimumSize(new Dimension(100, 100));
		jPanel3.doLayout();
		jPanel3.repaint();

		FileMenu.setText("File");
		FileMenu.setMnemonic(java.awt.event.KeyEvent.VK_F);
		FileItemNew.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_N,
				java.awt.event.InputEvent.CTRL_MASK));
		FileItemNew.setMnemonic(java.awt.event.KeyEvent.VK_N);
		FileItemNew.setText("New");
		FileItemNew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileItemNewActionPerformed(evt);
			}
		});

		FileMenu.add(FileItemNew);

		FileItemOpen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_O,
				java.awt.event.InputEvent.CTRL_MASK));
		FileItemOpen.setMnemonic(java.awt.event.KeyEvent.VK_O);
		FileItemOpen.setText("Open");
		FileItemOpen.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileItemOpenActionPerformed(evt);
			}
		});

		FileMenu.add(FileItemOpen);

		FileOpenRecent.setText("Open Recent");
		FileOpenRecent.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent evt) {
				rebuildRecentFilesMenu();
			}
			public void menuDeselected(MenuEvent evt) {}
			public void menuCanceled(MenuEvent evt) {}
		});
		rebuildRecentFilesMenu();
		FileMenu.add(FileOpenRecent);
		FileProjectOpenRecent.setText("Open Recent Project");
		FileProjectOpenRecent.addMenuListener(new MenuListener() {
			public void menuSelected(MenuEvent evt) {
				rebuildRecentProjectsMenu();
			}
			public void menuDeselected(MenuEvent evt) {}
			public void menuCanceled(MenuEvent evt) {}
		});
		rebuildRecentProjectsMenu();
		FileMenu.add(FileProjectOpenRecent);
		FileProjectMenu.setText("Project");
		FileProjectNew.setText("New Project");
		FileProjectNew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectNewActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectNew);
		FileProjectOpen.setText("Open Project...");
		FileProjectOpen.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectOpenActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectOpen);
		FileProjectSave.setText("Save Project");
		FileProjectSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectSaveActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectSave);
		FileProjectSaveAs.setText("Save Project As...");
		FileProjectSaveAs.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectSaveAsActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectSaveAs);
		FileProjectMenu.addSeparator();
		FileProjectAddCurrent.setText("Add Current Diagram");
		FileProjectAddCurrent.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectAddCurrentActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectAddCurrent);
		FileProjectAddDiagrams.setText("Add Diagrams...");
		FileProjectAddDiagrams.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectAddDiagramsActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectAddDiagrams);
		FileProjectMenu.addSeparator();
		FileProjectBuildAll.setText("Build All");
		FileProjectBuildAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectBuildAllActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectBuildAll);
		FileProjectLintAll.setText("Lint All");
		FileProjectLintAll.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ProjectLintAllActionPerformed(evt);
			}
		});
		FileProjectMenu.add(FileProjectLintAll);
		FileMenu.add(FileProjectMenu);

		FileItemSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_S,
				java.awt.event.InputEvent.CTRL_MASK));
		FileItemSave.setMnemonic(java.awt.event.KeyEvent.VK_S);
		FileItemSave.setText("Save");
		FileItemSave.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileItemSaveActionPerformed(evt);
			}
		});

		FileMenu.add(FileItemSave);


		FileItemSaveAs.setText("Save As");
		FileItemSaveAs.setMnemonic(java.awt.event.KeyEvent.VK_A);
		FileItemSaveAs.setDisplayedMnemonicIndex(5);
		FileItemSaveAs.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileItemSaveAsActionPerformed(evt);
			}
		});

		FileMenu.add(FileItemSaveAs);
		
		//export
		
		
		FileExportClipboard.setText("Clipboard");
		FileExportClipboard.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_F2, 0));
		FileExportClipboard.setMnemonic(java.awt.event.KeyEvent.VK_C);
		FileExportClipboard.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileExportClipboardActionPerformed(evt);
			}
		});		
		FileExport.add(FileExportClipboard);
		
		
		FileExportPNG.setText("PNG");
		FileExportPNG.setMnemonic(java.awt.event.KeyEvent.VK_P);
		FileExportPNG.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileExportPNGActionPerformed(evt);
			}
		});
		FileExport.add(FileExportPNG);
		
		FileExportJPEG.setText("JPEG");
		FileExportJPEG.setMnemonic(java.awt.event.KeyEvent.VK_J);
		FileExportJPEG.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileExportJPEGActionPerformed(evt);
			}
		});
		FileExport.add(FileExportJPEG);
		FileExport.setMnemonic(java.awt.event.KeyEvent.VK_E);


		FileMenu.add(FileExport);

		FilePref.setText("View Settings");
		FilePref.setMnemonic(java.awt.event.KeyEvent.VK_V);
		FilePref.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						FilePrefActionPerformed(evt);
					}
		});

		FileMenu.add(jSeparator2);

		FileItemExit.setText("Exit");
		FileItemExit.setMnemonic(java.awt.event.KeyEvent.VK_X);
		FileItemExit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				FileItemExitActionPerformed(evt);
			}
		});

		FileMenu.add(FileItemExit);

		MenuBar.add(FileMenu);

		EditMenu.setText("Edit");
		EditMenu.setMnemonic(java.awt.event.KeyEvent.VK_E);
		EditItemUndo.setMnemonic(java.awt.event.KeyEvent.VK_U);
		EditItemUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Z,
				java.awt.event.InputEvent.CTRL_MASK));
		EditItemUndo.setText("Undo");
		EditItemUndo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				EditItemUndoActionPerformed(evt);
			}
		});

		EditMenu.add(EditItemUndo);
		EditItemRedo.setMnemonic(java.awt.event.KeyEvent.VK_R);
		EditItemRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Y,
				java.awt.event.InputEvent.CTRL_MASK));
		EditItemRedo.setText("Redo");
		EditItemRedo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				EditItemRedoActionPerformed(evt);
			}
		});

		EditMenu.add(EditItemRedo);
		EditItemDelete.setMnemonic(java.awt.event.KeyEvent.VK_D);
		EditItemDelete.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_DELETE, 0));
		EditItemDelete.setText("Delete");
		//EditItemDelete.setVisible(false);
		EditItemDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				EditItemDeleteActionPerformed(evt);
			}
		});

		EditMenu.add(EditItemDelete);

		MenuBar.add(EditMenu);

		settingsMenu.setText("Settings");
		settingsMenu.setMnemonic(java.awt.event.KeyEvent.VK_S);
		settingsMenu.add(FilePref);

		defaultsItem.setText("Diagram Defaults");
		defaultsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsNewFsmDefaultsActionPerformed(evt);
			}
		});
		settingsMenu.add(defaultsItem);
		hdlSettingsItem.setText("HDL Generation");
		hdlSettingsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsHdlGenerationSettingsActionPerformed(evt);
			}
		});
		settingsMenu.add(hdlSettingsItem);
		languageMenu.setText("Language");
		ButtonGroup languageGroup = new ButtonGroup();
		addLanguageMenuItem(languageGroup, languageEnglishItem, "en");
		addLanguageMenuItem(languageGroup, languageJapaneseItem, "ja");
		addLanguageMenuItem(languageGroup, languageChineseSimplifiedItem, "zh_CN");
		addLanguageMenuItem(languageGroup, languageChineseTraditionalItem, "zh_TW");
		addLanguageMenuItem(languageGroup, languageKoreanItem, "ko");
		addLanguageMenuItem(languageGroup, languageGermanItem, "de");
		addLanguageMenuItem(languageGroup, languageFrenchItem, "fr");
		addLanguageMenuItem(languageGroup, languageSpanishItem, "es");
		addLanguageMenuItem(languageGroup, languagePortugueseItem, "pt");
		addLanguageMenuItem(languageGroup, languageHindiItem, "hi");
		addLanguageMenuItem(languageGroup, languageRussianItem, "ru");
		settingsMenu.add(languageMenu);
		MenuBar.add(settingsMenu);

		toolsMenu.setText("Tools");
		toolsMenu.setMnemonic(java.awt.event.KeyEvent.VK_T);

		lintItem.setText("Validate / Lint Diagram");
		lintItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsLintActionPerformed(evt);
			}
		});
		toolsMenu.add(lintItem);

		generateHdlItem.setText("Generate HDL");
		generateHdlItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsGenerateHdlActionPerformed(evt);
			}
		});
		toolsMenu.add(generateHdlItem);

		cleanupMenu.setText("Clean Up Diagram");
		resetLabelsItem.setText("Reset Transition Labels");
		resetLabelsItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsResetTransitionLabelsActionPerformed(evt);
			}
		});
		cleanupMenu.add(resetLabelsItem);

		cleanRoutesItem.setText("Clean Transition Routes");
		cleanRoutesItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsCleanTransitionRoutesActionPerformed(evt);
			}
		});
		cleanupMenu.add(cleanRoutesItem);
		cleanSelectedRoutesItem.setText("Clean Selected Routes");
		cleanSelectedRoutesItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsCleanSelectedRoutesActionPerformed(evt);
			}
		});
		cleanupMenu.add(cleanSelectedRoutesItem);
		cleanupMenu.addSeparator();
		alignHorizontalItem.setText("Align Selected Horizontally");
		alignHorizontalItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsAlignSelectedHorizontalActionPerformed(evt);
			}
		});
		cleanupMenu.add(alignHorizontalItem);
		alignVerticalItem.setText("Align Selected Vertically");
		alignVerticalItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsAlignSelectedVerticalActionPerformed(evt);
			}
		});
		cleanupMenu.add(alignVerticalItem);
		distributeHorizontalItem.setText("Distribute Selected Horizontally");
		distributeHorizontalItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsDistributeSelectedHorizontalActionPerformed(evt);
			}
		});
		cleanupMenu.add(distributeHorizontalItem);
		distributeVerticalItem.setText("Distribute Selected Vertically");
		distributeVerticalItem.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				ToolsDistributeSelectedVerticalActionPerformed(evt);
			}
		});
		cleanupMenu.add(distributeVerticalItem);
		toolsMenu.add(cleanupMenu);

		MenuBar.add(toolsMenu);

		GlobalMenu.setText("FSM Interface");
		GlobalMenu.setMnemonic(java.awt.event.KeyEvent.VK_G);
		
		GlobalItemMachine.setText("State Machine");
		GlobalItemMachine.setMnemonic(java.awt.event.KeyEvent.VK_M);
		GlobalItemMachine
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						GlobalItemMachineActionPerformed(evt);
					}
				});

		GlobalMenu.add(GlobalItemMachine);

		GlobalItemInputs.setMnemonic(java.awt.event.KeyEvent.VK_I);
		GlobalItemInputs.setText("Inputs");
		GlobalItemInputs.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				GlobalItemInputsActionPerformed(evt);
			}
		});

		GlobalMenu.add(GlobalItemInputs);

		GlobalItemParameters.setMnemonic(java.awt.event.KeyEvent.VK_P);
		GlobalItemParameters.setText("Parameters");
		GlobalItemParameters.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				GlobalItemParametersActionPerformed(evt);
			}
		});

		GlobalMenu.add(GlobalItemParameters);

		GlobalItemOutputs.setMnemonic(java.awt.event.KeyEvent.VK_O);
		GlobalItemOutputs.setText("Outputs");
		GlobalItemOutputs
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						GlobalItemOutputsActionPerformed(evt);
					}
				});

		GlobalMenu.add(GlobalItemOutputs);

		GlobalItemInternals.setMnemonic(java.awt.event.KeyEvent.VK_N);
		GlobalItemInternals.setText("Internals");
		GlobalItemInternals
				.addActionListener(new java.awt.event.ActionListener() {
					public void actionPerformed(java.awt.event.ActionEvent evt) {
						GlobalItemInternalsActionPerformed(evt);
					}
				});

		GlobalMenu.add(GlobalItemInternals);

		MenuBar.add(GlobalMenu);

		HelpMenu.setMnemonic(java.awt.event.KeyEvent.VK_H);
		HelpMenu.setText("Help");
		HelpItemHelp.setMnemonic(java.awt.event.KeyEvent.VK_H);
		HelpItemHelp.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_F1, 0));
		HelpItemHelp.setText("Help");
		HelpItemHelp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				HelpItemHelpActionPerformed(evt);
			}
		});
		
		
		
		HelpMenu.add(HelpItemHelp);

		HelpMenu.add(jSeparator4);
		
		HelpItemAbout.setMnemonic(java.awt.event.KeyEvent.VK_A);
		HelpItemAbout.setText("About");
		
		HelpItemAbout.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				HelpItemAboutActionPerformed(evt);
			}
		});
		HelpMenu.add(HelpItemAbout);

		
		
		MenuBar.add(HelpMenu);

		setJMenuBar(MenuBar);
		applyLocalization();

		pack();
	}// </editor-fold>//GEN-END:initComponents

	private void buildSidePanel() {
		sideTabbedPane.setPreferredSize(new Dimension(300, 420));
		buildPropertyInspectorPanel();
		buildProjectPanel();
		sideTabbedPane.addTab("Properties", propertyInspectorPanel);
		sideTabbedPane.addTab("Project", projectPanel);
	}

	private String t(String key) {
		return FizzimLocalizer.t(key);
	}

	private String tf(String key, Object... args) {
		return MessageFormat.format(t(key), args);
	}

	private void setGuiLanguage(String language) {
		FizzimLocalizer.setLanguage(language, USER_PREFS);
		applyLocalization();
	}

	private void addLanguageMenuItem(ButtonGroup group, JRadioButtonMenuItem item, final String language) {
		group.add(item);
		item.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				setGuiLanguage(language);
			}
		});
		languageMenu.add(item);
	}

	private void applyLocalization() {
		FileMenu.setText(t("menu.file"));
		FileItemNew.setText(t("menu.file.new"));
		FileItemOpen.setText(t("menu.file.open"));
		FileOpenRecent.setText(t("menu.file.openRecent"));
		FileProjectOpenRecent.setText(t("menu.file.openRecentProject"));
		FileProjectMenu.setText(t("menu.project"));
		FileProjectNew.setText(t("menu.project.new"));
		FileProjectOpen.setText(t("menu.project.open"));
		FileProjectSave.setText(t("menu.project.save"));
		FileProjectSaveAs.setText(t("menu.project.saveAs"));
		FileProjectAddCurrent.setText(t("menu.project.addCurrent"));
		FileProjectAddDiagrams.setText(t("menu.project.addDiagrams"));
		FileProjectBuildAll.setText(t("menu.project.buildAll"));
		FileProjectLintAll.setText(t("menu.project.lintAll"));
		FileItemSave.setText(t("menu.file.save"));
		FileItemSaveAs.setText(t("menu.file.saveAs"));
		FileExport.setText(t("menu.file.export"));
		FileExportClipboard.setText(t("menu.file.export.clipboard"));
		FileExportPNG.setText(t("menu.file.export.png"));
		FileExportJPEG.setText(t("menu.file.export.jpeg"));
		FilePref.setText(t("menu.settings.view"));
		FileItemExit.setText(t("menu.file.exit"));
		EditMenu.setText(t("menu.edit"));
		EditItemUndo.setText(t("menu.edit.undo"));
		EditItemRedo.setText(t("menu.edit.redo"));
		EditItemDelete.setText(t("menu.edit.delete"));
		settingsMenu.setText(t("menu.settings"));
		defaultsItem.setText(t("menu.settings.diagramDefaults"));
		hdlSettingsItem.setText(t("menu.settings.hdl"));
		languageMenu.setText(t("menu.settings.language"));
		languageEnglishItem.setText(t("menu.settings.language.english"));
		languageJapaneseItem.setText(t("menu.settings.language.japanese"));
		languageChineseSimplifiedItem.setText(t("menu.settings.language.chineseSimplified"));
		languageChineseTraditionalItem.setText(t("menu.settings.language.chineseTraditional"));
		languageKoreanItem.setText(t("menu.settings.language.korean"));
		languageGermanItem.setText(t("menu.settings.language.german"));
		languageFrenchItem.setText(t("menu.settings.language.french"));
		languageSpanishItem.setText(t("menu.settings.language.spanish"));
		languagePortugueseItem.setText(t("menu.settings.language.portuguese"));
		languageHindiItem.setText(t("menu.settings.language.hindi"));
		languageRussianItem.setText(t("menu.settings.language.russian"));
		String language = FizzimLocalizer.getLanguage();
		languageEnglishItem.setSelected(language.equals("en"));
		languageJapaneseItem.setSelected(language.equals("ja"));
		languageChineseSimplifiedItem.setSelected(language.equals("zh_CN"));
		languageChineseTraditionalItem.setSelected(language.equals("zh_TW"));
		languageKoreanItem.setSelected(language.equals("ko"));
		languageGermanItem.setSelected(language.equals("de"));
		languageFrenchItem.setSelected(language.equals("fr"));
		languageSpanishItem.setSelected(language.equals("es"));
		languagePortugueseItem.setSelected(language.equals("pt"));
		languageHindiItem.setSelected(language.equals("hi"));
		languageRussianItem.setSelected(language.equals("ru"));
		toolsMenu.setText(t("menu.tools"));
		lintItem.setText(t("menu.tools.lint"));
		generateHdlItem.setText(t("menu.tools.generate"));
		cleanupMenu.setText(t("menu.tools.cleanup"));
		resetLabelsItem.setText(t("menu.tools.cleanup.resetLabels"));
		cleanRoutesItem.setText(t("menu.tools.cleanup.cleanRoutes"));
		cleanSelectedRoutesItem.setText(t("menu.tools.cleanup.cleanSelectedRoutes"));
		alignHorizontalItem.setText(t("menu.tools.alignH"));
		alignVerticalItem.setText(t("menu.tools.alignV"));
		distributeHorizontalItem.setText(t("menu.tools.distributeH"));
		distributeVerticalItem.setText(t("menu.tools.distributeV"));
		GlobalMenu.setText(t("menu.interface"));
		GlobalItemMachine.setText(t("menu.interface.machine"));
		GlobalItemParameters.setText(t("menu.interface.parameters"));
		GlobalItemInputs.setText(t("menu.interface.inputs"));
		GlobalItemOutputs.setText(t("menu.interface.outputs"));
		GlobalItemInternals.setText(t("menu.interface.internals"));
		HelpMenu.setText(t("menu.help"));
		HelpItemHelp.setText(t("menu.help.wiki"));
		HelpItemAbout.setText(t("menu.help.about"));

		zoomOutButton.setText(t("toolbar.zoomOut"));
		zoomOutButton.setToolTipText(t("toolbar.zoomOut.tip"));
		zoomInButton.setText(t("toolbar.zoomIn"));
		zoomInButton.setToolTipText(t("toolbar.zoomIn.tip"));
		zoomFitButton.setText(t("toolbar.zoomFit"));
		zoomFitButton.setToolTipText(t("toolbar.zoomFit.tip"));
		lintButton.setText(t("toolbar.lint"));
		lintButton.setToolTipText(t("toolbar.lint.tip"));
		generateHdlButton.setText(t("toolbar.generate"));
		generateHdlButton.setToolTipText(t("toolbar.generate.tip"));

		if(inspectorSelectedObjects == null || inspectorSelectedObjects.size() == 0)
			selectionStatusLabel.setText(t("status.selection.none"));
		sideTabbedPane.setTitleAt(0, t("tabs.properties"));
		sideTabbedPane.setTitleAt(1, t("tabs.project"));
		propertyInspectorEditButton.setText(t("properties.openFullEditor"));
		updatePropertyInspector(inspectorSelectedObjects);
		projectOpenButton.setText(t("project.open"));
		projectAddButton.setText(t("project.addDiagrams"));
		projectBuildButton.setText(t("project.buildAll"));
		projectLintButton.setText(t("project.lintAll"));
		lintTabs.setTitleAt(0, t("lint.issues"));
		lintTabs.setTitleAt(1, t("lint.report"));
		lintRerunButton.setText(t("lint.rerun"));
		lintCloseButton.setText(t("lint.close"));
		updateHdlStatusIndicator();
		applyLintStatusText();
		rebuildRecentFilesMenu();
		rebuildRecentProjectsMenu();
	}

	private void showProjectPane() {
		if(sideTabbedPane != null && projectPanel != null)
			sideTabbedPane.setSelectedComponent(projectPanel);
	}

	private void installSpaceFitShortcut() {
		spaceFitDispatcher = new KeyEventDispatcher() {
			public boolean dispatchKeyEvent(KeyEvent event) {
				if(event.getID() != KeyEvent.KEY_PRESSED || event.getKeyCode() != KeyEvent.VK_SPACE
						|| event.getModifiersEx() != 0)
					return false;
				Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
				if(focusOwner == null || SwingUtilities.getWindowAncestor(focusOwner) != FizzimGui.this)
					return false;
				if(shouldLetFocusedComponentUseSpace(focusOwner))
					return false;
				fitDiagramShortcut();
				event.consume();
				return true;
			}
		};
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(spaceFitDispatcher);
	}

	private boolean shouldLetFocusedComponentUseSpace(Component focusOwner) {
		return focusOwner instanceof JTextComponent
				|| focusOwner instanceof JTable
				|| focusOwner instanceof JComboBox
				|| focusOwner instanceof JMenuItem;
	}

	private void buildPropertyInspectorPanel() {
		propertyInspectorPanel.setLayout(new BorderLayout(4, 4));
		propertyInspectorPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		propertyInspectorTitle.setText(t("properties.title"));
		propertyInspectorTitle.setFont(propertyInspectorTitle.getFont().deriveFont(Font.BOLD));
		propertyInspectorPanel.add(propertyInspectorTitle, BorderLayout.NORTH);
		propertyInspectorTable.setFont(FizzimFonts.tableFont());
		propertyInspectorTable.setRowHeight(propertyInspectorTable.getRowHeight() + 3);
		propertyInspectorScroll.setViewportView(propertyInspectorTable);
		propertyInspectorPanel.add(propertyInspectorScroll, BorderLayout.CENTER);
		propertyInspectorEditButton.setText("Open Full Editor");
		propertyInspectorEditButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openInspectorFullEditor();
			}
		});
		propertyInspectorPanel.add(propertyInspectorEditButton, BorderLayout.SOUTH);
		updatePropertyInspector(new LinkedList<GeneralObj>());
	}

	private void buildProjectPanel() {
		projectPanel.setLayout(new BorderLayout(4, 4));
		projectPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		projectTitleLabel.setFont(projectTitleLabel.getFont().deriveFont(Font.BOLD));
		projectPanel.add(projectTitleLabel, BorderLayout.NORTH);
		projectTree.setFont(FizzimFonts.tableFont());
		projectTree.setRootVisible(true);
		projectTree.setShowsRootHandles(true);
		projectTree.getSelectionModel().setSelectionMode(javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION);
		projectTree.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				if(evt.isPopupTrigger())
				{
					showProjectContextMenu(evt);
					return;
				}
				if(evt.getClickCount() == 2)
					openSelectedProjectDiagram();
			}
			public void mousePressed(MouseEvent evt) {
				if(evt.isPopupTrigger())
					showProjectContextMenu(evt);
			}
			public void mouseReleased(MouseEvent evt) {
				if(evt.isPopupTrigger())
					showProjectContextMenu(evt);
			}
		});
		projectScroll.setViewportView(projectTree);
		projectPanel.add(projectScroll, BorderLayout.CENTER);
		projectButtonPanel.setLayout(new java.awt.GridLayout(0, 1, 4, 4));
		projectOpenButton.setText("Open");
		projectOpenButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				openSelectedProjectDiagram();
			}
		});
		projectButtonPanel.add(projectOpenButton);
		projectAddButton.setText("Add Diagrams");
		projectAddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ProjectAddDiagramsActionPerformed(evt);
			}
		});
		projectButtonPanel.add(projectAddButton);
		projectBuildButton.setText("Build All");
		projectBuildButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ProjectBuildAllActionPerformed(evt);
			}
		});
		projectButtonPanel.add(projectBuildButton);
		projectLintButton.setText("Lint All");
		projectLintButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				ProjectLintAllActionPerformed(evt);
			}
		});
		projectButtonPanel.add(projectLintButton);
		projectPanel.add(projectButtonPanel, BorderLayout.SOUTH);
		updateProjectPanel();
	}

	private void buildLintPanel() {
		lintPanel.setLayout(new BorderLayout(4, 4));
		lintPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
		lintPanel.setPreferredSize(new Dimension(800, 220));
		lintPanel.setVisible(false);
		lintIssueList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		lintIssueList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
			public void valueChanged(javax.swing.event.ListSelectionEvent e) {
				if(e.getValueIsAdjusting())
					return;
				DrawArea.LintIssue issue = (DrawArea.LintIssue)lintIssueList.getSelectedValue();
				drawArea1.selectLintIssue(issue);
			}
		});
		lintReportText.setEditable(false);
		lintReportText.setLineWrap(true);
		lintReportText.setWrapStyleWord(true);
		lintTabs.addTab("Issues", new JScrollPane(lintIssueList));
		lintTabs.addTab("Report", new JScrollPane(lintReportText));
		lintPanel.add(lintTabs, BorderLayout.CENTER);
		JPanel lintButtons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		lintRerunButton.setText("Rerun");
		lintRerunButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showLintPanel();
			}
		});
		lintCloseButton.setText("Close");
		lintCloseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				lintPanel.setVisible(false);
				drawArea1.clearLintHighlights();
				revalidateForPaneChange();
			}
		});
		lintButtons.add(lintRerunButton);
		lintButtons.add(lintCloseButton);
		lintPanel.add(lintButtons, BorderLayout.SOUTH);
	}


	protected void HelpItemAboutActionPerformed(ActionEvent evt) {
		new SplashWindow("splash.png",this,60000);
		//new SplashWindowTest(this);
	}

	protected void HelpItemHelpActionPerformed(ActionEvent evt) {
		String url = "https://github.com/cookacounty/fizzim/wiki";
		try {
			if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE))
				Desktop.getDesktop().browse(new URI(url));
			else
				JOptionPane.showMessageDialog(this,
						t("help.wiki.unavailable") + "\n" + url,
						t("menu.help.wiki"),
						JOptionPane.INFORMATION_MESSAGE);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this,
					t("help.wiki.unavailable") + "\n" + url,
					t("menu.help.wiki"),
					JOptionPane.INFORMATION_MESSAGE);
		}
	}

	protected void FilePrefActionPerformed(ActionEvent evt) {
		new Pref(this, true, drawArea1).setVisible(true);
		
	}

	private void ToolsLintActionPerformed(ActionEvent evt) {
		showLintPanel();
	}

	private void showLintPanel() {
		String report = drawArea1.lintDiagram();
		final java.util.LinkedList<DrawArea.LintIssue> issues = drawArea1.getLastLintIssues();
		lintIssueModel.clear();
		if(issues.size() == 0)
			lintIssueModel.addElement(new DrawArea.LintIssue("PASS",
					"No lint issues found. The FSM structure looks ready for backend generation.", null));
		else
		{
			for(int i = 0; i < issues.size(); i++)
				lintIssueModel.addElement(issues.get(i));
		}
		lintReportText.setText(report);
		lintReportText.setCaretPosition(0);
		drawArea1.highlightAllLintIssues();
		lintPanel.setVisible(true);
		revalidateForPaneChange();
	}

	private void ToolsResetTransitionLabelsActionPerformed(ActionEvent evt) {
		drawArea1.resetTransitionLabelPositions();
	}

	private void ToolsCleanTransitionRoutesActionPerformed(ActionEvent evt) {
		drawArea1.cleanTransitionRoutes();
	}

	private void ToolsCleanSelectedRoutesActionPerformed(ActionEvent evt) {
		drawArea1.cleanSelectedTransitionRoutes();
	}

	private void ToolsAlignSelectedHorizontalActionPerformed(ActionEvent evt) {
		drawArea1.alignSelectedCenters(true);
	}

	private void ToolsAlignSelectedVerticalActionPerformed(ActionEvent evt) {
		drawArea1.alignSelectedCenters(false);
	}

	private void ToolsDistributeSelectedHorizontalActionPerformed(ActionEvent evt) {
		drawArea1.distributeSelectedCenters(true);
	}

	private void ToolsDistributeSelectedVerticalActionPerformed(ActionEvent evt) {
		drawArea1.distributeSelectedCenters(false);
	}

	private void ToolsNewFsmDefaultsActionPerformed(ActionEvent evt) {
		JTextField clockField = new JTextField(getDefaultClockName(), 12);
		JComboBox clockEdge = new JComboBox(new String[] {"posedge", "negedge"});
		clockEdge.setSelectedItem(getDefaultClockEdge());
		JTextField resetField = new JTextField(getDefaultResetName(), 12);
		JComboBox resetEdge = new JComboBox(new String[] {"negedge", "posedge", "negative", "positive"});
		resetEdge.setSelectedItem(getDefaultResetEdge());
		JCheckBox impliedLoopback = new JCheckBox("Enable implied loopback", getDefaultImpliedLoopback());
		JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
		panel.add(new JLabel("Clock name:"));
		panel.add(clockField);
		panel.add(new JLabel("Clock edge:"));
		panel.add(clockEdge);
		panel.add(new JLabel("Reset name:"));
		panel.add(resetField);
		panel.add(new JLabel("Reset edge/polarity:"));
		panel.add(resetEdge);
		panel.add(new JLabel(""));
		panel.add(impliedLoopback);
		int result = JOptionPane.showConfirmDialog(this, panel, "Diagram Defaults", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if(result == JOptionPane.OK_OPTION)
		{
			USER_PREFS.put(PREF_DEFAULT_CLOCK, clockField.getText().trim().equals("") ? "clk" : clockField.getText().trim());
			USER_PREFS.put(PREF_DEFAULT_CLOCK_EDGE, (String)clockEdge.getSelectedItem());
			USER_PREFS.put(PREF_DEFAULT_RESET, resetField.getText().trim().equals("") ? "rst_l" : resetField.getText().trim());
			USER_PREFS.put(PREF_DEFAULT_RESET_EDGE, (String)resetEdge.getSelectedItem());
			USER_PREFS.putBoolean(PREF_DEFAULT_IMPLIED_LOOPBACK, impliedLoopback.isSelected());
		}
	}

	private void ToolsHdlGenerationSettingsActionPerformed(ActionEvent evt) {
		showHdlGenerationSettingsDialog(false);
	}

	private void ToolsGenerateHdlActionPerformed(ActionEvent evt) {
		generateHdlFromGui();
	}

	private boolean showHdlGenerationSettingsDialog(boolean generationContext) {
		JTextField perlField = new JTextField(getHdlPerlCommand(), 24);
		JTextField backendField = new JTextField(getHdlBackendPath(), 24);
		JTextField outputDirField = new JTextField(getHdlOutputDir(), 24);
		JCheckBox useModuleName = new JCheckBox("Use module name for filename", getHdlUseModuleFilename());
		JTextField outputFileField = new JTextField(getHdlOutputFilename(), 24);
		JTextField extraArgsField = new JTextField(getHdlExtraArgs(), 24);
		JCheckBox compareEnabled = new JCheckBox("Generate comparison HDL and diff", getHdlCompareEnabled());
		JTextField compareCommandField = new JTextField(getHdlCompareCommand(), 24);
		JTextField compareBackendField = new JTextField(getHdlCompareBackendPath(), 24);
		JTextField compareArgsField = new JTextField(getHdlCompareArgs(), 24);
		JTextField compareSuffixField = new JTextField(getHdlCompareSuffix(), 24);
		outputFileField.setEnabled(!useModuleName.isSelected());
		useModuleName.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				outputFileField.setEnabled(!useModuleName.isSelected());
			}
		});
		ActionListener compareToggle = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean enabled = compareEnabled.isSelected();
				compareCommandField.setEnabled(enabled);
				compareBackendField.setEnabled(enabled);
				compareArgsField.setEnabled(enabled);
				compareSuffixField.setEnabled(enabled);
			}
		};
		compareEnabled.addActionListener(compareToggle);
		compareToggle.actionPerformed(null);

		JPanel panel = new JPanel(new java.awt.GridLayout(0, 2, 6, 6));
		panel.add(new JLabel("Perl command:"));
		panel.add(perlField);
		panel.add(new JLabel("Backend script:"));
		panel.add(backendField);
		panel.add(new JLabel("Output directory:"));
		panel.add(outputDirField);
		panel.add(new JLabel(""));
		panel.add(useModuleName);
		panel.add(new JLabel("Output filename:"));
		panel.add(outputFileField);
		panel.add(new JLabel("Backend options:"));
		panel.add(extraArgsField);
		panel.add(new JLabel(""));
		panel.add(compareEnabled);
		panel.add(new JLabel("Compare command:"));
		panel.add(compareCommandField);
		panel.add(new JLabel("Compare backend:"));
		panel.add(compareBackendField);
		panel.add(new JLabel("Compare options:"));
		panel.add(compareArgsField);
		panel.add(new JLabel("Compare file suffix:"));
		panel.add(compareSuffixField);
		int result = JOptionPane.showConfirmDialog(this, panel,
				generationContext ? "HDL Generation Settings" : "Configure HDL Generation",
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if(result != JOptionPane.OK_OPTION)
			return false;
		USER_PREFS.put(PREF_HDL_PERL, blankDefault(perlField.getText(), "perl"));
		USER_PREFS.put(PREF_HDL_BACKEND, blankDefault(backendField.getText(), "fizzim.pl"));
		USER_PREFS.put(PREF_HDL_OUTPUT_DIR, blankDefault(outputDirField.getText(), "."));
		USER_PREFS.putBoolean(PREF_HDL_USE_MODULE_FILENAME, useModuleName.isSelected());
		USER_PREFS.put(PREF_HDL_OUTPUT_FILENAME, blankDefault(outputFileField.getText(), ""));
		USER_PREFS.put(PREF_HDL_EXTRA_ARGS, extraArgsField.getText().trim());
		USER_PREFS.putBoolean(PREF_HDL_COMPARE_ENABLED, compareEnabled.isSelected());
		USER_PREFS.put(PREF_HDL_COMPARE_COMMAND, blankDefault(compareCommandField.getText(), "java"));
		USER_PREFS.put(PREF_HDL_COMPARE_BACKEND, blankDefault(compareBackendField.getText(), "FizzimJavaBackend"));
		USER_PREFS.put(PREF_HDL_COMPARE_ARGS, blankDefault(compareArgsField.getText(), getHdlExtraArgs()));
		USER_PREFS.put(PREF_HDL_COMPARE_SUFFIX, blankDefault(compareSuffixField.getText(), ".java"));
		return true;
	}

	private void generateHdlFromGui() {
		if(currFile == null)
		{
			JOptionPane.showMessageDialog(this, "Save the diagram before generating HDL.", "Generate HDL", JOptionPane.INFORMATION_MESSAGE);
			FileItemSaveAsActionPerformed(null);
			if(currFile == null)
				return;
		}
		if(drawArea1.getFileModifed())
		{
			int result = JOptionPane.showConfirmDialog(this,
					"Save changes before generating HDL?",
					"Generate HDL", JOptionPane.OK_CANCEL_OPTION);
			if(result != JOptionPane.OK_OPTION)
				return;
			if(!saveFile(currFile))
				return;
		}
		try {
			File output = resolveHdlOutputFile(currFile, getMachineName());
			File parent = output.getParentFile();
			if(parent != null && !parent.exists() && !parent.mkdirs())
			{
				JOptionPane.showMessageDialog(this, "Could not create output directory:\n" + parent.getAbsolutePath(),
						"Generate HDL", JOptionPane.ERROR_MESSAGE);
				return;
			}
			HdlGenerationResult result = runHdlBackend(currFile, output);
			if(result.exitCode == 0)
			{
				boolean generatedOk = true;
				if(getHdlCompareEnabled())
					generatedOk = runHdlComparison(currFile, output);
				else
					JOptionPane.showMessageDialog(this,
							"Generated HDL:\n" + output.getAbsolutePath(),
							"Generate HDL", JOptionPane.INFORMATION_MESSAGE);
				if(generatedOk)
					markHdlGeneratedInSync(output);
			}
			else
			{
				JOptionPane.showMessageDialog(this,
						"HDL generation failed with exit code " + result.exitCode + "\n\n" + result.stderr,
						"Generate HDL", JOptionPane.ERROR_MESSAGE);
			}
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this,
					"HDL generation failed:\n" + ex.getMessage(),
					"Generate HDL", JOptionPane.ERROR_MESSAGE);
			ex.printStackTrace();
		}
	}

	public void markHdlOutOfSync() {
		hdlGeneratedInSync = false;
		setMachineAttributeValue(HDL_STATE_ATTR, "0");
		updateHdlStatusIndicator();
		markLintStale();
		updateWindowTitle();
	}

	private void markHdlGeneratedInSync(File output) {
		hdlGeneratedInSync = true;
		setMachineAttributeValue(HDL_STATE_ATTR, "1");
		if(output != null)
			setMachineAttributeValue(HDL_OUTPUT_ATTR, pathRelativeToFzm(output));
		updateHdlStatusIndicator();
		drawArea1.setFileModifiedPreserveHdlStatus(true);
	}

	public void updateWindowTitle() {
		String title = APP_TITLE;
		if(currFile != null)
			title += " - " + currFile.getName();
		if(drawArea1 != null && drawArea1.getFileModifed())
			title = "*" + title;
		setTitle(title);
		updateProjectPanel();
	}

	private void updateHdlStatusIndicator() {
		if(hdlStatusLabel == null)
			return;
		if(hdlGeneratedInSync)
		{
			hdlStatusLabel.setText(t("status.hdl.sync"));
			hdlStatusLabel.setToolTipText(t("status.hdl.sync.tip"));
			hdlStatusLabel.setBackground(new Color(190, 235, 190));
			hdlStatusLabel.setForeground(new Color(20, 85, 20));
			hdlStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(80, 145, 80)));
		}
		else
		{
			hdlStatusLabel.setText(t("status.hdl.stale"));
			hdlStatusLabel.setToolTipText(t("status.hdl.stale.tip"));
			hdlStatusLabel.setBackground(new Color(255, 235, 150));
			hdlStatusLabel.setForeground(new Color(100, 75, 0));
			hdlStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(170, 135, 30)));
		}
	}

	private void markLintStale() {
		if(lintStatusLabel == null)
			return;
		lintStatusMode = "stale";
		lintErrorCount = 0;
		lintWarningCount = 0;
		applyLintStatusText();
	}

	private void updateLintStatus() {
		if(lintStatusLabel == null || drawArea1 == null)
			return;
		drawArea1.lintDiagram();
		LinkedList<DrawArea.LintIssue> issues = drawArea1.getLastLintIssues();
		int errors = 0;
		int warnings = 0;
		for(int i = 0; i < issues.size(); i++)
		{
			DrawArea.LintIssue issue = issues.get(i);
			if(issue.severity.equals("ERROR"))
				errors++;
			else if(issue.severity.equals("WARN"))
				warnings++;
		}
		if(errors > 0)
		{
			lintStatusMode = "errors";
		}
		else if(warnings > 0)
		{
			lintStatusMode = "warn";
		}
		else
		{
			lintStatusMode = "clean";
		}
		lintErrorCount = errors;
		lintWarningCount = warnings;
		applyLintStatusText();
	}

	private void applyLintStatusText() {
		if(lintStatusLabel == null)
			return;
		if(lintStatusMode.equals("errors"))
		{
			lintStatusLabel.setText(t("status.lint.errors"));
			lintStatusLabel.setBackground(new Color(255, 205, 205));
			lintStatusLabel.setForeground(new Color(120, 20, 20));
			lintStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(185, 70, 70)));
			lintStatusLabel.setToolTipText(tf("status.lint.tip", new Integer(lintErrorCount), new Integer(lintWarningCount)));
		}
		else if(lintStatusMode.equals("warn"))
		{
			lintStatusLabel.setText(t("status.lint.warn"));
			lintStatusLabel.setBackground(new Color(255, 235, 150));
			lintStatusLabel.setForeground(new Color(100, 75, 0));
			lintStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(170, 135, 30)));
			lintStatusLabel.setToolTipText(tf("status.lint.tip", new Integer(lintErrorCount), new Integer(lintWarningCount)));
		}
		else if(lintStatusMode.equals("clean"))
		{
			lintStatusLabel.setText(t("status.lint.clean"));
			lintStatusLabel.setBackground(new Color(190, 235, 190));
			lintStatusLabel.setForeground(new Color(20, 85, 20));
			lintStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(80, 145, 80)));
			lintStatusLabel.setToolTipText(tf("status.lint.tip", new Integer(lintErrorCount), new Integer(lintWarningCount)));
		}
		else
		{
			lintStatusLabel.setText(t("status.lint.stale"));
			lintStatusLabel.setToolTipText(t("status.lint.stale.tip"));
			lintStatusLabel.setBackground(new Color(225, 230, 235));
			lintStatusLabel.setForeground(new Color(75, 85, 99));
			lintStatusLabel.setBorder(BorderFactory.createLineBorder(new Color(150, 160, 170)));
		}
	}

	private boolean runHdlComparison(File fzmFile, File primaryOutput) throws IOException, InterruptedException {
		File compareOutput = comparisonOutputFile(primaryOutput);
		HdlGenerationResult compare = runConfiguredHdlBackend(compareOutput, getHdlCompareCommand(),
				getHdlCompareBackendPath(), getHdlCompareArgs(), fzmFile);
		if(compare.exitCode != 0)
		{
			JOptionPane.showMessageDialog(this,
					"Primary HDL generated:\n" + primaryOutput.getAbsolutePath()
					+ "\n\nComparison generation failed with exit code " + compare.exitCode + "\n\n" + compare.stderr,
					"Generate HDL", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		File diffFile = new File(primaryOutput.getParentFile(), stripExtension(primaryOutput.getName()) + ".diff.txt");
		String diff = diffFiles(primaryOutput, compareOutput);
		if(diff.equals(""))
		{
			if(diffFile.exists())
				diffFile.delete();
			JOptionPane.showMessageDialog(this,
					"Generated HDL matched comparison output.\n\nPrimary:\n" + primaryOutput.getAbsolutePath()
					+ "\n\nComparison:\n" + compareOutput.getAbsolutePath(),
					"Generate HDL", JOptionPane.INFORMATION_MESSAGE);
			return true;
		}
		else
		{
			Files.write(diffFile.toPath(), diff.getBytes(StandardCharsets.UTF_8));
			JOptionPane.showMessageDialog(this,
					"Generated HDL mismatch detected.\n\nPrimary:\n" + primaryOutput.getAbsolutePath()
					+ "\n\nComparison:\n" + compareOutput.getAbsolutePath()
					+ "\n\nDiff:\n" + diffFile.getAbsolutePath(),
					"Generate HDL", JOptionPane.WARNING_MESSAGE);
			return false;
		}
	}

	private HdlGenerationResult runHdlBackend(File fzmFile, File output) throws IOException, InterruptedException {
		return runConfiguredHdlBackend(output, getHdlPerlCommand(), getHdlBackendPath(), getHdlExtraArgs(), fzmFile);
	}

	private HdlGenerationResult runConfiguredHdlBackend(File output, String commandName, String backendPath, String backendArgs, File fzmFile) throws IOException, InterruptedException {
		File fzmDir = fzmFile.getAbsoluteFile().getParentFile();
		File errorFile = File.createTempFile("fizzim-hdl-generation", ".log");
		ArrayList<String> command = new ArrayList<String>();
		if(isJavaBackendClass(commandName, backendPath))
		{
			command.add(resolveJavaCommand(commandName));
			command.add("-cp");
			command.add(applicationClassPath());
			command.add(backendPath.trim());
			command.addAll(splitCommandArgs(backendArgs));
			command.add(fzmFile.getName());
		}
		else
		{
			File backend = resolveRelativeToApp(backendPath);
			if(!backend.exists())
				throw new IOException("Backend script not found: " + backend.getAbsolutePath());
			command.add(commandName);
			command.add(backend.getAbsolutePath());
			command.addAll(splitCommandArgs(backendArgs));
			command.add(fzmFile.getName());
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(fzmDir);
		builder.redirectOutput(output);
		builder.redirectError(errorFile);
		Process process = builder.start();
		int exit = process.waitFor();
		String stderr = new String(Files.readAllBytes(errorFile.toPath()), StandardCharsets.UTF_8);
		errorFile.delete();
		return new HdlGenerationResult(exit, stderr);
	}

	private File comparisonOutputFile(File primaryOutput) {
		String suffix = getHdlCompareSuffix();
		if(suffix == null || suffix.trim().equals(""))
			suffix = ".java";
		String base = stripExtension(primaryOutput.getName());
		return new File(primaryOutput.getParentFile(), base + suffix + ".v");
	}

	private String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		if(dot <= 0)
			return filename;
		return filename.substring(0, dot);
	}

	private String diffFiles(File primary, File comparison) throws IOException {
		java.util.List<String> primaryLines = Files.readAllLines(primary.toPath(), StandardCharsets.UTF_8);
		java.util.List<String> comparisonLines = Files.readAllLines(comparison.toPath(), StandardCharsets.UTF_8);
		StringBuffer diff = new StringBuffer();
		int max = Math.max(primaryLines.size(), comparisonLines.size());
		for(int i = 0; i < max; i++)
		{
			String a = i < primaryLines.size() ? primaryLines.get(i) : null;
			String b = i < comparisonLines.size() ? comparisonLines.get(i) : null;
			if(a == null || b == null || !a.equals(b))
			{
				diff.append("@@ line ").append(i + 1).append(" @@\n");
				diff.append("- ").append(a == null ? "<missing>" : a).append("\n");
				diff.append("+ ").append(b == null ? "<missing>" : b).append("\n");
			}
		}
		return diff.toString();
	}

	private File resolveHdlOutputFile(File fzmFile, String machineName) {
		File fzmDir = fzmFile.getAbsoluteFile().getParentFile();
		File outputDir = resolveRelativeToFzm(fzmFile, getHdlOutputDir());
		String filename;
		if(getHdlUseModuleFilename())
			filename = sanitizeHdlFilename(machineName) + ".v";
		else
			filename = getHdlOutputFilename();
		if(filename == null || filename.trim().equals(""))
			filename = sanitizeHdlFilename(machineName) + ".v";
		if(!filename.toLowerCase().endsWith(".v"))
			filename += ".v";
		File output = new File(filename);
		if(output.isAbsolute())
			return output;
		return new File(outputDir == null ? fzmDir : outputDir, filename);
	}

	private File resolveRelativeToFzm(String path) {
		return resolveRelativeToFzm(currFile, path);
	}

	private File resolveRelativeToFzm(File fzmFile, String path) {
		File candidate = new File(path);
		if(candidate.isAbsolute())
			return candidate;
		File fzmDir = fzmFile == null ? new File(System.getProperty("user.dir")) : fzmFile.getAbsoluteFile().getParentFile();
		return new File(fzmDir, path);
	}

	private String pathRelativeToFzm(File file) {
		if(file == null)
			return "";
		try {
			File fzmDir = currFile == null ? new File(System.getProperty("user.dir")) : currFile.getAbsoluteFile().getParentFile();
			return fzmDir.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).toString();
		} catch (Exception ex) {
			return file.getAbsolutePath();
		}
	}

	private void restorePersistedHdlStatus() {
		String generated = getMachineAttributeValue(HDL_STATE_ATTR);
		String output = getMachineAttributeValue(HDL_OUTPUT_ATTR);
		hdlGeneratedInSync = generated.equals("1") && !output.equals("") && resolveRelativeToFzm(output).exists();
		updateHdlStatusIndicator();
	}

	private String getMachineAttributeValue(String name) {
		ObjAttribute attr = findMachineAttribute(name);
		return attr == null || attr.getValue() == null ? "" : attr.getValue().trim();
	}

	private void setMachineAttributeValue(String name, String value) {
		if(globalList == null || globalList.size() == 0)
			return;
		ObjAttribute attr = findMachineAttribute(name);
		if(attr == null)
		{
			int[] editable = { ObjAttribute.ABS, ObjAttribute.GLOBAL_VAR,
					ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR,
					ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR };
			attr = new ObjAttribute(name, value, 0, "attribute", "", Color.black, "", "", editable);
			globalList.get(0).add(attr);
		}
		else
			attr.setValue(value);
	}

	private ObjAttribute findMachineAttribute(String name) {
		if(globalList == null || globalList.size() == 0)
			return null;
		for(int i = 0; i < globalList.get(0).size(); i++)
		{
			ObjAttribute attr = globalList.get(0).get(i);
			if(attr.getName().equals(name))
				return attr;
		}
		return null;
	}

	private File resolveRelativeToApp(String path) {
		File candidate = new File(path);
		if(candidate.isAbsolute())
			return candidate;
		File appRelative = new File(applicationDirectory(), path);
		if(appRelative.exists())
			return appRelative;
		return new File(System.getProperty("user.dir"), path);
	}

	private File applicationDirectory() {
		File classPath = new File(applicationClassPath());
		if(classPath.isFile())
			return classPath.getParentFile();
		return classPath;
	}

	private String applicationClassPath() {
		try {
			return new File(FizzimGui.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
		} catch (URISyntaxException ex) {
			return new File(System.getProperty("user.dir")).getAbsolutePath();
		}
	}

	private boolean isJavaBackendClass(String commandName, String backendPath) {
		if(backendPath == null || backendPath.trim().equals(""))
			return false;
		String backend = backendPath.trim();
		String command = commandName == null ? "" : commandName.trim().toLowerCase();
		return backend.equals("FizzimJavaBackend")
				&& (command.equals("") || command.equals("java") || command.equals("javaw") || command.endsWith("\\java.exe")
						|| command.endsWith("/java") || command.endsWith("/java.exe"));
	}

	private String resolveJavaCommand(String commandName) {
		String command = commandName == null ? "" : commandName.trim();
		String lower = command.toLowerCase();
		if(!command.equals("") && !lower.equals("java") && !lower.equals("javaw"))
			return command;
		String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
		return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
	}

	private String getMachineName() {
		for(int i = 0; i < globalList.get(0).size(); i++)
		{
			ObjAttribute attr = globalList.get(0).get(i);
			if(attr.getName().equals("name"))
				return attr.getValue();
		}
		return "fsm";
	}

	private String sanitizeHdlFilename(String name) {
		if(name == null || name.trim().equals(""))
			return "fsm";
		return name.trim().replaceAll("[^A-Za-z0-9_.$-]", "_");
	}

	private ArrayList<String> splitCommandArgs(String args) {
		ArrayList<String> parts = new ArrayList<String>();
		if(args == null || args.trim().equals(""))
			return parts;
		StringTokenizer tokenizer = new StringTokenizer(args);
		while(tokenizer.hasMoreTokens())
			parts.add(tokenizer.nextToken());
		return parts;
	}

	private String blankDefault(String value, String defaultValue) {
		if(value == null || value.trim().equals(""))
			return defaultValue;
		return value.trim();
	}

	private static String getHdlPerlCommand() {
		return USER_PREFS.get(PREF_HDL_PERL, "perl");
	}

	private static String getHdlBackendPath() {
		return USER_PREFS.get(PREF_HDL_BACKEND, "fizzim.pl");
	}

	private static String getHdlOutputDir() {
		return USER_PREFS.get(PREF_HDL_OUTPUT_DIR, ".");
	}

	private static boolean getHdlUseModuleFilename() {
		return USER_PREFS.getBoolean(PREF_HDL_USE_MODULE_FILENAME, true);
	}

	private static String getHdlOutputFilename() {
		return USER_PREFS.get(PREF_HDL_OUTPUT_FILENAME, "");
	}

	private static String getHdlExtraArgs() {
		return USER_PREFS.get(PREF_HDL_EXTRA_ARGS, "-noaddversion");
	}

	private static boolean getHdlCompareEnabled() {
		return USER_PREFS.getBoolean(PREF_HDL_COMPARE_ENABLED, false);
	}

	private static String getHdlCompareCommand() {
		return USER_PREFS.get(PREF_HDL_COMPARE_COMMAND, "java");
	}

	private static String getHdlCompareBackendPath() {
		return USER_PREFS.get(PREF_HDL_COMPARE_BACKEND, "FizzimJavaBackend");
	}

	private static String getHdlCompareArgs() {
		String args = USER_PREFS.get(PREF_HDL_COMPARE_ARGS, "");
		if(args == null || args.trim().equals(""))
			return getHdlExtraArgs();
		return args;
	}

	private static String getHdlCompareSuffix() {
		return USER_PREFS.get(PREF_HDL_COMPARE_SUFFIX, ".java");
	}

	private static class HdlGenerationResult {
		int exitCode;
		String stderr;

		HdlGenerationResult(int code, String err) {
			exitCode = code;
			stderr = err == null ? "" : err;
		}
	}

	private void ZoomOutActionPerformed(ActionEvent evt) {
		leaveZoomFitMode();
		drawArea1.setZoom(drawArea1.getZoom() / 1.25);
	}

	private void ZoomInActionPerformed(ActionEvent evt) {
		leaveZoomFitMode();
		drawArea1.setZoom(drawArea1.getZoom() * 1.25);
	}

	private void ZoomFitActionPerformed(ActionEvent evt) {
		enterZoomFitMode();
		scheduleFitDiagramToViewport(true);
	}

	public void fitDiagramShortcut() {
		enterZoomFitMode();
		scheduleFitDiagramToViewport(true);
	}

	private void fitDiagramToViewport(boolean allowZoomIn) {
		getContentPane().doLayout();
		jPanel3.doLayout();
		jTabbedPane1.doLayout();
		jScrollPane1.doLayout();
		Dimension viewport = jScrollPane1.getViewport().getExtentSize();
		drawArea1.fitDiagramToViewport(viewport, allowZoomIn);
	}

	private void scheduleFitDiagramToViewport() {
		scheduleFitDiagramToViewport(true);
	}

	private void scheduleFitDiagramToViewport(final boolean allowZoomIn) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				fitDiagramToViewport(allowZoomIn);
			}
		});
	}

	public void fitDiagramOnlyIfZoomOutNeeded() {
		if(!zoomFitMode || jScrollPane1 == null || drawArea1 == null)
			return;
		Dimension viewport = jScrollPane1.getViewport().getExtentSize();
		Dimension diagram = drawArea1.getPreferredSize();
		if(diagram.width > viewport.width || diagram.height > viewport.height)
			scheduleFitDiagramToViewport(false);
	}

	private void revalidateForPaneChange() {
		getContentPane().invalidate();
		getContentPane().validate();
		jPanel3.revalidate();
		jTabbedPane1.revalidate();
		jScrollPane1.revalidate();
		repaint();
		if(zoomFitMode)
			scheduleFitDiagramToViewport();
	}

	private void enterZoomFitMode() {
		zoomFitMode = true;
	}

	private void leaveZoomFitMode() {
		zoomFitMode = false;
	}

	public void viewManuallyChanged() {
		leaveZoomFitMode();
	}

	public void updateZoomControls() {
		if(zoomPercentLabel != null)
			zoomPercentLabel.setText(Integer.toString((int)Math.round(drawArea1.getZoom() * 100)) + "%");
	}

	public void updateSelectionStatus(String text) {
		if(selectionStatusLabel != null)
			selectionStatusLabel.setText(text);
	}

	public void updatePropertyInspector(LinkedList<GeneralObj> selected) {
		if(propertyInspectorPanel == null || propertyInspectorTable == null)
			return;
		inspectorSelectedObjects = selected;
		if(selected == null || selected.size() == 0)
		{
			propertyInspectorTitle.setText(t("properties.title"));
			propertyInspectorTable.setModel(new ReadOnlyInspectorTableModel(
					new Object[][] {{t("properties.noSelection"), ""}},
					new Object[] {t("properties.field"), t("properties.value")}));
			propertyInspectorEditButton.setEnabled(false);
			return;
		}
		if(selected.size() > 1)
		{
			propertyInspectorTitle.setText(t("properties.title") + " - " + selected.size() + " " + t("properties.objects"));
			if(canBatchEditInInspector(selected))
			{
				propertyInspectorTable.setModel(new BatchInspectorTableModel(drawArea1, selected, globalList));
				propertyInspectorEditButton.setEnabled(false);
				return;
			}
			propertyInspectorTable.setModel(new ReadOnlyInspectorTableModel(
					new Object[][] {{t("properties.selection"), selected.size() + " " + t("properties.objects")}},
					new Object[] {t("properties.field"), t("properties.value")}));
			propertyInspectorEditButton.setEnabled(false);
			return;
		}

		GeneralObj obj = selected.getFirst();
		propertyInspectorTitle.setText(t("properties.title") + " - " + objectTypeName(obj));
		propertyInspectorEditButton.setEnabled(true);
		if(obj.getType() == 3)
		{
			propertyInspectorTable.setModel(new ReadOnlyInspectorTableModel(
					new Object[][] {{"Text", ((TextObj)obj).getText()}},
					new Object[] {"Field", "Value"}));
			return;
		}
		if(obj.getAttributeList() == null || obj.getAttributeList().size() == 0)
		{
			propertyInspectorTable.setModel(new ReadOnlyInspectorTableModel(
					new Object[][] {{"Name", obj.getName()}},
					new Object[] {"Field", "Value"}));
			return;
		}
		propertyInspectorTable.setModel(new InspectorTableModel(drawArea1, obj, globalList));
	}

	private boolean canBatchEditInInspector(LinkedList<GeneralObj> selected) {
		for(int i = 0; i < selected.size(); i++)
		{
			GeneralObj obj = selected.get(i);
			if(obj.getType() != 0 && obj.getType() != 1 && obj.getType() != 2 && obj.getType() != 5)
				return false;
			if(obj.getAttributeList() == null || obj.getAttributeList().size() == 0)
				return false;
		}
		return true;
	}

	private String objectTypeName(GeneralObj obj) {
		if(obj.getType() == 0)
			return "State " + obj.getName();
		if(obj.getType() == 1)
			return "Transition " + obj.getName();
		if(obj.getType() == 2)
			return "Loopback " + obj.getName();
		if(obj.getType() == 3)
			return "Text";
		if(obj.getType() == 4)
			return "Fork " + obj.getName();
		if(obj.getType() == 5)
			return "State Group " + obj.getName();
		return obj.getName();
	}

	private void openInspectorFullEditor() {
		if(inspectorSelectedObjects == null || inspectorSelectedObjects.size() != 1)
			return;
		GeneralObj obj = inspectorSelectedObjects.getFirst();
		if(obj.getType() == 0 || obj.getType() == 4 || obj.getType() == 5)
		{
			new StateProperties(drawArea1, this, true, (StateObj)obj).setVisible(true);
		}
		else if(obj.getType() == 1 || obj.getType() == 2)
		{
			Vector<StateObj> stateObjs = drawArea1.getTransitionEndpointObjects();
			if(obj.getType() == 2)
			{
				for(int i = stateObjs.size() - 1; i >= 0; i--)
				{
					if(stateObjs.get(i).getType() != 0)
						stateObjs.remove(i);
				}
			}
			new TransProperties(drawArea1, this, true, (TransitionObj)obj, stateObjs, obj.getType() == 2, null).setVisible(true);
		}
		else if(obj.getType() == 3)
		{
			drawArea1.editText((TextObj)obj);
		}
		updatePropertyInspector(drawArea1.getSelectedObjectsForInspector());
	}

	protected void FileExportPNGActionPerformed(ActionEvent evt) {
		
		try {
			ExportChooser.setCurrentDirectory(currFile);
			ExportChooser.showSaveDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(ExportChooser.getSelected())
			tryToSave(ExportChooser.getSelectedFile(), "png",true);
	
	}
	
	private void exportFile(File file, String type)
	{
		try {
			ImageIO.write(getImage(),type,file);
			} catch (IOException e) {
		    }
	}

	protected void FileExportJPEGActionPerformed(ActionEvent evt) {
		try {
			ExportChooser.setCurrentDirectory(currFile);
			ExportChooser.showSaveDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(ExportChooser.getSelected())
			tryToSave(ExportChooser.getSelectedFile(),"jpg",true);				
	}

	protected void FileExportClipboardActionPerformed(ActionEvent evt) {
		drawArea1.updateCanvasExtents();
		BufferedImage bufferedImage = new BufferedImage(drawArea1.getLogicalWidth(), drawArea1.getLogicalHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D tempG = bufferedImage.createGraphics();
		drawArea1.unselectObjs();
		drawArea1.paintUnscaled(tempG);
		
		//get rid of white space
		int lX = -1;
		int rX = -1;
		int tY = -1;
		int bY = -1;
		int h = bufferedImage.getHeight()-1;
		int w = bufferedImage.getWidth()-1;

		//get top
		for(int y = 0; y < h; y++)
			for(int x = 0; x < w; x++)
				if(bufferedImage.getRGB(x, y) != -1)
					bY = y;

		// get bottom
		for(int y = h; y >= 0; y--)
			for(int x = 0; x < w; x++)
				if(bufferedImage.getRGB(x, y) != -1)
					tY = y;
		
		//get left
		for(int x = 0; x < w; x++)
			for(int y = 0; y < h; y++)
				if(bufferedImage.getRGB(x, y) != -1)
					rX = x;
		
		//get right
		for(int x = w; x >= 0; x--)
			for(int y = 0; y < h; y++)
				if(bufferedImage.getRGB(x, y) != -1)
					lX = x;
		
		//if it worked correctly, make a cropped bufferedimage
		if(lX != -1 && rX != -1 && tY != -1 && bY != -1)
		{
			//System.out.println(lX +" "+rX+" "+tY+" "+bY);
			//if(lX-1<0)
			//	lX=1;
			//if(tY-1<0)
			bufferedImage = bufferedImage.getSubimage(lX-1, tY-1, rX-lX+3, bY-tY+3);
		}
		

		ImageToClip imageToClip = new ImageToClip(bufferedImage);
	
	}
	
	private RenderedImage getImage()
	{
		drawArea1.updateCanvasExtents();
		BufferedImage bufferedImage = new BufferedImage(drawArea1.getLogicalWidth(), drawArea1.getLogicalHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D tempG = bufferedImage.createGraphics();
		drawArea1.paintUnscaled(tempG);
		
		return bufferedImage;
	}

	protected void renameTab(int tab) {

			String s = (String)JOptionPane.showInputDialog(
			        this,
			        "Edit Tab Name:\n",
			        "Edit Tab Name",
			        JOptionPane.PLAIN_MESSAGE,
			        null,
			        null,
			        jTabbedPane1.getTitleAt(tab));
		
		if(s != null)
		{
			if(getPageIndex(s) == -1)
				jTabbedPane1.setTitleAt(tab,s);
			else
				JOptionPane.showMessageDialog(this,
                        "Page must have unique name",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
		}
	}


	protected void TabChanged(ChangeEvent e) {
		
		if(!loading)
		{
			JTabbedPane pane = (JTabbedPane)e.getSource();
		  
	        // Get current tab
	        int sel = pane.getSelectedIndex();
	        //fill all but current tab with empty panels
	        for(int i = 1; i < jTabbedPane1.getTabCount(); i++)
	        {
	        	if(i != sel)
	        		jTabbedPane1.setComponentAt(i, new JPanel());
	        }
	        
	        if(sel == 0)
	        {
	        	int index = jTabbedPane1.getTabCount();
	        	jTabbedPane1.addTab("Page " + String.valueOf(index), jScrollPane1);
	        	jTabbedPane1.setSelectedIndex(index);
	        	drawArea1.setCurrPage(index);
	        }
	        else
	        {    
		        //set current tab
		        drawArea1.setCurrPage(sel);
		        jTabbedPane1.setComponentAt(sel,jScrollPane1);
	        }
	        drawArea1.unselectObjs();
		}
	}

	private void movePageTab(int fromIndex, int toIndex) {
		if(fromIndex == toIndex || fromIndex < 1 || toIndex < 1 || fromIndex >= jTabbedPane1.getTabCount()
				|| toIndex >= jTabbedPane1.getTabCount())
			return;

		loading = true;
		int selected = jTabbedPane1.getSelectedIndex();
		int newSelected = selected;
		if(selected == fromIndex)
			newSelected = toIndex;
		else if(fromIndex < selected && selected <= toIndex)
			newSelected = selected - 1;
		else if(toIndex <= selected && selected < fromIndex)
			newSelected = selected + 1;

		String title = jTabbedPane1.getTitleAt(fromIndex);
		Icon icon = jTabbedPane1.getIconAt(fromIndex);
		Component component = jTabbedPane1.getComponentAt(fromIndex);
		String tooltip = jTabbedPane1.getToolTipTextAt(fromIndex);
		boolean enabled = jTabbedPane1.isEnabledAt(fromIndex);
		jTabbedPane1.removeTabAt(fromIndex);
		jTabbedPane1.insertTab(title, icon, component, tooltip, toIndex);
		jTabbedPane1.setEnabledAt(toIndex, enabled);
		drawArea1.reorderPages(fromIndex, toIndex);
		jTabbedPane1.setSelectedIndex(newSelected);
		for(int i = 1; i < jTabbedPane1.getTabCount(); i++)
		{
			if(i != newSelected)
				jTabbedPane1.setComponentAt(i, new JPanel());
		}
		jTabbedPane1.setComponentAt(newSelected, jScrollPane1);
		drawArea1.setCurrPage(newSelected);
		drawArea1.setFileModifed(true);
		drawArea1.unselectObjs();
		loading = false;
		jTabbedPane1.revalidate();
		jTabbedPane1.repaint();
	}

	protected void FileOpenActionActionPerformed(ActionEvent evt) {
	}

	//GEN-FIRST:event_formComponentResized
	private void formComponentResized(java.awt.event.ComponentEvent evt) {
		//this method makes sure that the draw area size is mostly restricted
		// to dimensions set in page setup
		
		jTabbedPane1.setMinimumSize(new Dimension(100, 100));
		drawArea1.updateCanvasExtents();
		jPanel3.doLayout();
		jPanel3.repaint();
		if(zoomFitMode)
			scheduleFitDiagramToViewport();

	}//GEN-LAST:event_formComponentResized

	protected void formWindowClosing(WindowEvent evt) {
		formWindowClosing();

	}

	//GEN-FIRST:event_FileItemSaveActionPerformed
	private void FileItemSaveActionPerformed(java.awt.event.ActionEvent evt) {
		if (currFile == null) {
			try {
                                // Default to cwd
				//FileSaveAction.setCurrentDirectory(new java.io.File("").getAbsoluteFile());
				FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
				FileSaveAction.showSaveDialog(this);
			} catch (java.awt.HeadlessException e1) {
				e1.printStackTrace();
			}
			if(FileSaveAction.getSelected())
				if(tryToSave(FileSaveAction.getSelectedFile(), "fzm", true))
					updateWindowTitle();
					
			} else {
			saveFile(currFile);
		}
	}//GEN-LAST:event_FileItemSaveActionPerformed
	
	public boolean tryToSave(File file, String type, boolean overrideCheck)
	{
		//checks file for correct pathname
		String temp = file.getName().toLowerCase();

		if (!temp.endsWith("." + type))
			file = new File(file.getAbsolutePath() + "." + type);

		if(type.equals("fzm") && !drawArea1.validateStateGroupMembership(true))
			return false;
		
		//checks permission to write
		if(file.isDirectory())
		{
			JOptionPane.showMessageDialog(this,
					"Must be a file, not directory", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if(file.exists() && !file.canWrite())
		{
			JOptionPane.showMessageDialog(this,
					"Cannot write to file, permission denied.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else if(overrideCheck)
		{
			if (file.exists()) {
				int choice = JOptionPane.showConfirmDialog(this,
						"Overwrite file?", "Save As",
						JOptionPane.YES_NO_OPTION);
				if (choice == JOptionPane.NO_OPTION)
					return false;
			}
		}
		else if(!file.exists())
		{
			try {
				file.createNewFile();				
			} catch (IOException e) {
				JOptionPane.showMessageDialog(this,
						"Cannot write to file, permission denied.", "Error",
						JOptionPane.ERROR_MESSAGE);
				return false;
			}
		}
		if(type.equals("fzm"))
		{
			if(!saveFile(file))
				return false;
		}
		else
			exportFile(file,type);
		return true;
	}

	//perform checks before window is closed
	private void formWindowClosing() {
		if (drawArea1.getFileModifed()) {
			if (currFile == null) {
				Object[] options = { "Yes", "No", "Cancel" };

				int n = JOptionPane
						.showOptionDialog(this, "Save file before closing?",
								APP_TITLE, JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (n == JOptionPane.YES_OPTION) {
					try {
				                FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
						FileSaveAction.showSaveDialog(this);
					} catch (java.awt.HeadlessException e1) {
						e1.printStackTrace();
					}
					if(FileSaveAction.getSelected())
					{
						if(tryToSave(FileSaveAction.getSelectedFile(), "fzm", true))
							closeWindow();
					}
				} else if (n == JOptionPane.NO_OPTION) {
					closeWindow();
				}
			} else {
				Object[] options = { "Yes", "No", "Cancel" };

				int n = JOptionPane
						.showOptionDialog(this, "Save changes to "
								+ currFile.getName() + "?", APP_TITLE,
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (n == JOptionPane.YES_OPTION) {
					if(saveFile(currFile))
						closeWindow();
				} else if (n == JOptionPane.NO_OPTION) {
					closeWindow();
				}
			}
		} else
			closeWindow();
	}//GEN-LAST:event_formWindowClosing

	private void closeWindow() {
		if(closed)
			return;
		closed = true;
		if(spaceFitDispatcher != null)
		{
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(spaceFitDispatcher);
			spaceFitDispatcher = null;
		}
		openWindowCount--;
		dispose();
		if(openWindowCount <= 0)
			System.exit(0);
	}

	//GEN-FIRST:event_EditItemDeleteActionPerformed
	private void EditItemDeleteActionPerformed(java.awt.event.ActionEvent evt) {
		drawArea1.delete();
	}//GEN-LAST:event_EditItemDeleteActionPerformed

	//GEN-FIRST:event_EditItemRedoActionPerformed
	private void EditItemRedoActionPerformed(java.awt.event.ActionEvent evt) {
		drawArea1.redo();
		updateLintStatus();
	}//GEN-LAST:event_EditItemRedoActionPerformed

	//GEN-FIRST:event_EditItemUndoActionPerformed
	private void EditItemUndoActionPerformed(java.awt.event.ActionEvent evt) {
		drawArea1.undo();
		updateLintStatus();
	}//GEN-LAST:event_EditItemUndoActionPerformed


	//GEN-FIRST:event_FileItemExitActionPerformed
	private void FileItemExitActionPerformed(java.awt.event.ActionEvent evt) {
		formWindowClosing();
	}//GEN-LAST:event_FileItemExitActionPerformed

	//GEN-FIRST:event_FileItemNewActionPerformed
	private void FileItemNewActionPerformed(java.awt.event.ActionEvent evt) {
		
		boolean createNew = true;
		if (drawArea1.getFileModifed()) {
			if (currFile == null) {
				Object[] options = { "Yes", "No", "Cancel" };

				int n = JOptionPane
						.showOptionDialog(this, "Save file before creating new file?",
								APP_TITLE, JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);

				if (n == JOptionPane.YES_OPTION) {
					try {
				                FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
						FileSaveAction.showSaveDialog(this);
					} catch (java.awt.HeadlessException e1) {
						e1.printStackTrace();
					}
					if(FileSaveAction.getSelected())
						tryToSave(FileSaveAction.getSelectedFile(),"fzm", true);
				}
				else if(n == JOptionPane.CANCEL_OPTION || n == -1)
					createNew = false;
			} else {
				Object[] options = { "Yes", "No", "Cancel" };

				int n = JOptionPane
						.showOptionDialog(this, "Save changes to "
								+ currFile.getName() + "?", APP_TITLE,
								JOptionPane.YES_NO_CANCEL_OPTION,
								JOptionPane.QUESTION_MESSAGE, null, options,
								options[0]);
				if (n == JOptionPane.YES_OPTION) 
				{
					if(!saveFile(currFile))
						createNew = false;
				}
				else if(n == JOptionPane.CANCEL_OPTION || n == -1)
					createNew = false;
			}
		}
		if(createNew)
		{
			for(int i = jTabbedPane1.getTabCount()-1; i > 1; i--)
			{
				jTabbedPane1.remove(i);
			}
			jTabbedPane1.setComponentAt(1,jScrollPane1);
			currFile = null;
			updateWindowTitle();
			for(int i = 0; i < globalList.size(); i++)
			{
				globalList.get(i).clear();
			}
			initGlobal();
			drawArea1.open(globalList);
			markHdlOutOfSync();
		}

	}//GEN-LAST:event_FileItemNewActionPerformed
	
	public void resetTabs() {
		for(int i = jTabbedPane1.getTabCount()-1; i > 0; i--)
		{
			jTabbedPane1.remove(i);
		}
		
	}

	public void addNewTab(String line) {
		jTabbedPane1.addTab(line, new JPanel());
		
	}
	

	
	private void GlobalItemMachineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GlobalItemMachineActionPerformed
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 0)
				.setVisible(true);
	}//GEN-LAST:event_GlobalItemMachineActionPerformed
	
	private void GlobalItemInputsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GlobalItemsInputsActionPerformed
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 1)
				.setVisible(true);
	}//GEN-LAST:event_GlobalItemsInputsActionPerformed

	private void GlobalItemParametersActionPerformed(java.awt.event.ActionEvent evt) {
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 6)
				.setVisible(true);
	}

	private void GlobalItemOutputsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GlobalItemOutputsActionPerformed
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 2)
				.setVisible(true);
		/*GlobalAttributesFrame.setSize(600, 300);
		GlobalAttributesFrame.getRootPane().setDefaultButton(GACancel);
		GlobalAttributesTabbedPane.setSelectedComponent(GAOutputsScrollPane);
		GlobalAttributesFrame.show();*/

	}//GEN-LAST:event_GlobalItemOutputsActionPerformed

	private void GlobalItemInternalsActionPerformed(java.awt.event.ActionEvent evt) {
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 5)
				.setVisible(true);
	}

	private void GlobalItemStatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GlobalItemStatesActionPerformed
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 3)
				.setVisible(true);
	}//GEN-LAST:event_GlobalItemStatesActionPerformed

	private void GlobalItemTransitionsActionPerformed(
			java.awt.event.ActionEvent evt) {//GEN-FIRST:event_GlobalItemTransitionsActionPerformed
		globalList = drawArea1.setUndoPoint();
		new GlobalProperties(drawArea1, this, true, globalList, 4)
				.setVisible(true);
	}//GEN-LAST:event_GlobalItemTransitionsActionPerformed





	private void FileItemSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			if(currFile == null)
			{
                                // Default to cwd
				FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			}
			else
				FileSaveAction.setSelectedFile(currFile);

		        FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			FileSaveAction.showSaveDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(FileSaveAction.getSelected())
			if(tryToSave(FileSaveAction.getSelectedFile(), "fzm", true))
				updateWindowTitle();
				
	}

	private void FileItemOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileItemOpenActionPerformed
		try {
			if(currFile == null)
				FileOpenAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			else
				FileOpenAction.setCurrentDirectory(currFile);
			FileOpenAction.showOpenDialog(null);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}

		if(FileOpenAction.getSelected())
		{
			File[] selectedFiles = FileOpenAction.getSelectedFiles();
			if(selectedFiles == null || selectedFiles.length == 0)
				selectedFiles = new File[] { FileOpenAction.getSelectedFile() };

			for(int i = 0; i < selectedFiles.length; i++)
			{
				File tempFile = selectedFiles[i];
				if(!isFizzimFile(tempFile))
				{
					JOptionPane.showMessageDialog(this, "File must end with .fzm",
							"error", JOptionPane.ERROR_MESSAGE);
					continue;
				}

				if(i == 0 && canReuseThisWindowForOpen())
					openFile(tempFile);
				else
					openFileInNewWindow(tempFile);
			}
		}

		
	}//GEN-LAST:event_FileItemOpenActionPerformed

	private boolean isFizzimFile(File file) {
		if(file == null || file.isDirectory())
			return false;
		return file.getName().toLowerCase().endsWith(".fzm");
	}

	private void rememberLastOpened(String type, File file) {
		if(type == null || file == null)
			return;
		USER_PREFS.put(PREF_LAST_OPEN_TYPE, type);
		USER_PREFS.put(PREF_LAST_OPEN_PATH, file.getAbsoluteFile().getAbsolutePath());
		try {
			USER_PREFS.flush();
		} catch (Exception e) { }
	}

	private void clearLastOpenedIfMatches(File file) {
		if(file == null)
			return;
		String path = USER_PREFS.get(PREF_LAST_OPEN_PATH, "");
		if(path.equals(file.getAbsoluteFile().getAbsolutePath()))
		{
			USER_PREFS.remove(PREF_LAST_OPEN_TYPE);
			USER_PREFS.remove(PREF_LAST_OPEN_PATH);
			try {
				USER_PREFS.flush();
			} catch (Exception e) { }
		}
	}

	private void reopenLastOpenedItem() {
		String type = USER_PREFS.get(PREF_LAST_OPEN_TYPE, "");
		String path = USER_PREFS.get(PREF_LAST_OPEN_PATH, "");
		if(path == null || path.equals(""))
			return;
		File file = new File(path);
		if(type.equals("project"))
		{
			if(isProjectFile(file) && file.exists())
				openProject(file, false);
			else
				clearLastOpenedIfMatches(file);
		}
		else if(type.equals("diagram"))
		{
			if(isFizzimFile(file) && file.exists())
				openFile(file);
			else
				clearLastOpenedIfMatches(file);
		}
	}

	private boolean canReuseThisWindowForOpen() {
		return currFile == null && !drawArea1.getFileModifed();
	}

	private void rebuildRecentFilesMenu() {
		FileOpenRecent.removeAll();
		LinkedList<File> recentFiles = getRecentFiles();
		boolean pruned = false;
		for(int i = recentFiles.size() - 1; i >= 0; i--)
		{
			File recentFile = recentFiles.get(i);
			if(!isFizzimFile(recentFile) || !recentFile.exists())
			{
				recentFiles.remove(i);
				pruned = true;
			}
		}
		if(pruned)
			storeRecentFiles(recentFiles);
		if(recentFiles.size() == 0)
		{
			JMenuItem emptyItem = new JMenuItem(t("menu.file.openRecent.empty"));
			emptyItem.setEnabled(false);
			FileOpenRecent.add(emptyItem);
			return;
		}

		for(int i = 0; i < recentFiles.size(); i++)
		{
			final File recentFile = recentFiles.get(i);
			JMenuItem item = new JMenuItem((i + 1) + " " + recentFile.getName());
			item.setToolTipText(recentFile.getAbsolutePath());
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					openRecentFile(recentFile);
				}
			});
			FileOpenRecent.add(item);
		}

		FileOpenRecent.addSeparator();
		JMenuItem clearItem = new JMenuItem(t("menu.file.openRecent.clear"));
		clearItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				clearRecentFiles();
			}
		});
		FileOpenRecent.add(clearItem);
	}

	private LinkedList<File> getRecentFiles() {
		LinkedList<File> recentFiles = new LinkedList<File>();
		for(int i = 0; i < RECENT_FILE_LIMIT; i++)
		{
			String path = USER_PREFS.get(RECENT_FILE_PREFIX + i, "");
			if(path != null && !path.equals(""))
				recentFiles.add(new File(path));
		}
		return recentFiles;
	}

	private void rememberRecentFile(File file) {
		if(file == null)
			return;

		File absoluteFile = file.getAbsoluteFile();
		LinkedList<File> recentFiles = getRecentFiles();
		for(int i = recentFiles.size() - 1; i >= 0; i--)
		{
			if(recentFiles.get(i).getAbsolutePath().equals(absoluteFile.getAbsolutePath()))
				recentFiles.remove(i);
		}
		recentFiles.addFirst(absoluteFile);
		while(recentFiles.size() > RECENT_FILE_LIMIT)
			recentFiles.removeLast();
		storeRecentFiles(recentFiles);
		rebuildRecentFilesMenu();
	}

	private void forgetRecentFile(File file) {
		if(file == null)
			return;

		LinkedList<File> recentFiles = getRecentFiles();
		for(int i = recentFiles.size() - 1; i >= 0; i--)
		{
			if(recentFiles.get(i).getAbsolutePath().equals(file.getAbsolutePath()))
				recentFiles.remove(i);
		}
		storeRecentFiles(recentFiles);
		rebuildRecentFilesMenu();
	}

	private void clearRecentFiles() {
		storeRecentFiles(new LinkedList<File>());
		rebuildRecentFilesMenu();
	}

	private void rebuildRecentProjectsMenu() {
		FileProjectOpenRecent.removeAll();
		LinkedList<File> recentProjects = getRecentProjects();
		boolean pruned = false;
		for(int i = recentProjects.size() - 1; i >= 0; i--)
		{
			File recentProject = recentProjects.get(i);
			if(!isProjectFile(recentProject) || !recentProject.exists())
			{
				recentProjects.remove(i);
				pruned = true;
			}
		}
		if(pruned)
			storeRecentProjects(recentProjects);
		if(recentProjects.size() == 0)
		{
			JMenuItem emptyItem = new JMenuItem(t("menu.file.openRecentProject.empty"));
			emptyItem.setEnabled(false);
			FileProjectOpenRecent.add(emptyItem);
			return;
		}

		for(int i = 0; i < recentProjects.size(); i++)
		{
			final File recentProject = recentProjects.get(i);
			JMenuItem item = new JMenuItem((i + 1) + " " + recentProject.getName());
			item.setToolTipText(recentProject.getAbsolutePath());
			item.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					openRecentProject(recentProject);
				}
			});
			FileProjectOpenRecent.add(item);
		}

		FileProjectOpenRecent.addSeparator();
		JMenuItem clearItem = new JMenuItem(t("menu.file.openRecentProject.clear"));
		clearItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				clearRecentProjects();
			}
		});
		FileProjectOpenRecent.add(clearItem);
	}

	private LinkedList<File> getRecentProjects() {
		LinkedList<File> recentProjects = new LinkedList<File>();
		for(int i = 0; i < RECENT_FILE_LIMIT; i++)
		{
			String path = USER_PREFS.get(RECENT_PROJECT_PREFIX + i, "");
			if(path != null && !path.equals(""))
				recentProjects.add(new File(path));
		}
		return recentProjects;
	}

	private void rememberRecentProject(File file) {
		if(file == null)
			return;

		File absoluteFile = file.getAbsoluteFile();
		LinkedList<File> recentProjects = getRecentProjects();
		for(int i = recentProjects.size() - 1; i >= 0; i--)
		{
			if(recentProjects.get(i).getAbsolutePath().equals(absoluteFile.getAbsolutePath()))
				recentProjects.remove(i);
		}
		recentProjects.addFirst(absoluteFile);
		while(recentProjects.size() > RECENT_FILE_LIMIT)
			recentProjects.removeLast();
		storeRecentProjects(recentProjects);
		rebuildRecentProjectsMenu();
	}

	private void forgetRecentProject(File file) {
		if(file == null)
			return;

		LinkedList<File> recentProjects = getRecentProjects();
		for(int i = recentProjects.size() - 1; i >= 0; i--)
		{
			if(recentProjects.get(i).getAbsolutePath().equals(file.getAbsolutePath()))
				recentProjects.remove(i);
		}
		storeRecentProjects(recentProjects);
		rebuildRecentProjectsMenu();
	}

	private void clearRecentProjects() {
		storeRecentProjects(new LinkedList<File>());
		rebuildRecentProjectsMenu();
	}

	private void ProjectNewActionPerformed(java.awt.event.ActionEvent evt) {
		createNewProjectWithPrompt(true);
	}

	private void ProjectOpenActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			if(currProjectFile == null)
				ProjectOpenAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			else
				ProjectOpenAction.setSelectedFile(currProjectFile);
			ProjectOpenAction.showOpenDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(ProjectOpenAction.getSelected())
			openProject(ProjectOpenAction.getSelectedFile());
	}

	private void ProjectSaveActionPerformed(java.awt.event.ActionEvent evt) {
		if(currProjectFile == null)
			ProjectSaveAsActionPerformed(evt);
		else
			saveProject(currProjectFile, true);
	}

	private void ProjectSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
		try {
			ProjectSaveAction.setDialogTitle("Save Project As (.fzp)");
			ProjectSaveAction.setApproveButtonText("Save Project");
			if(currProjectFile == null)
				ProjectSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			else
				ProjectSaveAction.setSelectedFile(currProjectFile);
			ProjectSaveAction.showSaveDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(ProjectSaveAction.getSelected())
		{
			File selectedFile = ProjectSaveAction.getSelectedFile();
			File file = ensureProjectExtension(selectedFile);
			if(projectExtensionWasAdded(selectedFile))
			{
				int choice = JOptionPane.showConfirmDialog(this,
						"Project files use the .fzp extension.\n\nFizzim will save this project as:\n"
						+ file.getAbsolutePath(),
						"Project Extension", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
				if(choice != JOptionPane.OK_OPTION)
					return;
			}
			saveProject(file, true);
		}
	}

	private void ProjectAddCurrentActionPerformed(java.awt.event.ActionEvent evt) {
		if(!ensureProjectReadyForAdd())
			return;
		if(currFile == null)
		{
			JOptionPane.showMessageDialog(this, "Save or open a diagram before adding it to the project.",
					"Project", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		if(addProjectDiagram(currFile))
		{
			autosaveProject();
			JOptionPane.showMessageDialog(this, "Added diagram to project:\n" + currFile.getAbsolutePath()
					+ "\n\nProject diagrams: " + projectDiagramFiles.size(), "Project", JOptionPane.INFORMATION_MESSAGE);
		}
	}

	private void ProjectAddDiagramsActionPerformed(java.awt.event.ActionEvent evt) {
		if(!ensureProjectReadyForAdd())
			return;
		try {
			if(currProjectFile != null)
				FileOpenAction.setCurrentDirectory(currProjectFile.getAbsoluteFile().getParentFile());
			else if(currFile != null)
				FileOpenAction.setCurrentDirectory(currFile.getAbsoluteFile().getParentFile());
			else
				FileOpenAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			FileOpenAction.showOpenDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(FileOpenAction.getSelected())
		{
			File[] selectedFiles = FileOpenAction.getSelectedFiles();
			if(selectedFiles == null || selectedFiles.length == 0)
				selectedFiles = new File[] { FileOpenAction.getSelectedFile() };
			int added = 0;
			for(int i = 0; i < selectedFiles.length; i++)
				if(isFizzimFile(selectedFiles[i]))
					if(addProjectDiagram(selectedFiles[i]))
						added++;
			if(added > 0)
			{
				autosaveProject();
				JOptionPane.showMessageDialog(this, "Added " + added + " diagram" + (added == 1 ? "" : "s")
						+ " to project.\n\nProject diagrams: " + projectDiagramFiles.size(),
						"Project", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	private void ProjectBuildAllActionPerformed(java.awt.event.ActionEvent evt) {
		buildProject();
	}

	private void ProjectLintAllActionPerformed(java.awt.event.ActionEvent evt) {
		lintProject();
	}

	private boolean createNewProjectWithPrompt(boolean showMessage) {
		try {
			ProjectSaveAction.setDialogTitle("New Project (.fzp)");
			ProjectSaveAction.setApproveButtonText("Create Project");
			if(currProjectFile == null)
			{
				if(currFile != null)
					ProjectSaveAction.setCurrentDirectory(currFile.getAbsoluteFile().getParentFile());
				else
					ProjectSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
			}
			else
				ProjectSaveAction.setSelectedFile(currProjectFile);
			ProjectSaveAction.showSaveDialog(this);
		} catch (java.awt.HeadlessException e1) {
			e1.printStackTrace();
		}
		if(!ProjectSaveAction.getSelected())
			return false;
		File selectedFile = ProjectSaveAction.getSelectedFile();
		File file = ensureProjectExtension(selectedFile);
		if(projectExtensionWasAdded(selectedFile))
		{
			int choice = JOptionPane.showConfirmDialog(this,
					"Project files use the .fzp extension.\n\nFizzim will save this project as:\n"
					+ file.getAbsolutePath(),
					"Project Extension", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
			if(choice != JOptionPane.OK_OPTION)
				return false;
		}
		if(file.exists())
		{
			int choice = JOptionPane.showConfirmDialog(this,
					"Replace existing project file?\n" + file.getAbsolutePath(),
					"New Project", JOptionPane.YES_NO_OPTION);
			if(choice != JOptionPane.YES_OPTION)
				return false;
		}
		projectDiagramFiles.clear();
		currProjectFile = file.getAbsoluteFile();
		return saveProject(currProjectFile, showMessage);
	}

	private boolean ensureProjectReadyForAdd() {
		if(currProjectFile != null)
			return true;
		Object[] options = { "Create Project...", "Cancel" };
		int choice = JOptionPane.showOptionDialog(this,
				"Create and save a project before adding diagrams?\nThe project file location is used for relative diagram paths.",
				"Project", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);
		if(choice != JOptionPane.YES_OPTION)
			return false;
		return createNewProjectWithPrompt(false);
	}

	private void openProject(File projectFile) {
		openProject(projectFile, true);
	}

	private void openProject(File projectFile, boolean showMessage) {
		try {
			File file = ensureProjectExtension(projectFile).getAbsoluteFile();
			LinkedList<File> diagrams = new LinkedList<File>();
			File projectDir = file.getParentFile();
			java.util.List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
			for(int i = 0; i < lines.size(); i++)
			{
				String line = lines.get(i).trim();
				if(line.equals("") || line.startsWith("#"))
					continue;
				File diagram = new File(line);
				if(!diagram.isAbsolute())
					diagram = new File(projectDir, line);
				diagrams.add(diagram.getAbsoluteFile());
			}
			projectDiagramFiles = diagrams;
			currProjectFile = file;
			updateProjectPanel();
			showProjectPane();
			rememberRecentProject(currProjectFile);
			rememberLastOpened("project", currProjectFile);
			if(showMessage)
				JOptionPane.showMessageDialog(this, "Opened project:\n" + file.getAbsolutePath()
						+ "\n\nDiagrams: " + projectDiagramFiles.size(), "Project", JOptionPane.INFORMATION_MESSAGE);
		} catch (IOException ex) {
			if(showMessage)
				JOptionPane.showMessageDialog(this, "Could not open project:\n" + ex.getMessage(),
						"Project", JOptionPane.ERROR_MESSAGE);
			else
				clearLastOpenedIfMatches(projectFile);
		}
	}

	private boolean saveProject(File projectFile) {
		return saveProject(projectFile, true);
	}

	private boolean saveProject(File projectFile, boolean showMessage) {
		try {
			File file = ensureProjectExtension(projectFile).getAbsoluteFile();
			File parent = file.getParentFile();
			if(parent != null && !parent.exists() && !parent.mkdirs())
			{
				JOptionPane.showMessageDialog(this, "Could not create project directory:\n" + parent.getAbsolutePath(),
						"Project", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			StringBuffer text = new StringBuffer();
			text.append("# Fizzim 2.0 project\n");
			text.append("# One .fzm diagram path per line. Relative paths are resolved from this project file.\n");
			for(int i = 0; i < projectDiagramFiles.size(); i++)
				text.append(pathRelativeToProject(file, projectDiagramFiles.get(i))).append("\n");
			Files.write(file.toPath(), text.toString().getBytes(StandardCharsets.UTF_8));
			currProjectFile = file;
			updateProjectPanel();
			rememberRecentProject(currProjectFile);
			rememberLastOpened("project", currProjectFile);
			if(showMessage)
				JOptionPane.showMessageDialog(this, "Saved project:\n" + file.getAbsolutePath()
						+ "\n\nDiagrams: " + projectDiagramFiles.size(), "Project", JOptionPane.INFORMATION_MESSAGE);
			return true;
		} catch (IOException ex) {
			JOptionPane.showMessageDialog(this, "Could not save project:\n" + ex.getMessage(),
					"Project", JOptionPane.ERROR_MESSAGE);
			return false;
		}
	}

	private boolean autosaveProject() {
		if(currProjectFile == null)
			return false;
		return saveProject(currProjectFile, false);
	}

	private boolean addProjectDiagram(File diagram) {
		File absolute = diagram.getAbsoluteFile();
		for(int i = 0; i < projectDiagramFiles.size(); i++)
			if(projectDiagramFiles.get(i).getAbsolutePath().equals(absolute.getAbsolutePath()))
				return false;
		projectDiagramFiles.add(absolute);
		sortProjectDiagramFiles();
		updateProjectPanel();
		return true;
	}

	private void sortProjectDiagramFiles() {
		Collections.sort(projectDiagramFiles, new Comparator<File>() {
			public int compare(File a, File b) {
				return projectDisplayPath(a).replace('\\', '/').compareToIgnoreCase(projectDisplayPath(b).replace('\\', '/'));
			}
		});
	}

	private void updateProjectPanel() {
		if(projectTree == null || projectOpenButton == null)
			return;
		String rootName = currProjectFile == null ? "Project" : currProjectFile.getName();
		projectTreeRoot = new DefaultMutableTreeNode(rootName);
		for(int i = 0; i < projectDiagramFiles.size(); i++)
			addProjectTreePath(projectDiagramFiles.get(i));
		if(projectDiagramFiles.size() == 0 && currProjectFile == null)
		{
			projectTreeRoot.add(new DefaultMutableTreeNode("No project open"));
			projectTreeRoot.add(new DefaultMutableTreeNode("Use File > Project > New Project..."));
		}
		else if(projectDiagramFiles.size() == 0)
			projectTreeRoot.add(new DefaultMutableTreeNode("(No diagrams in project)"));
		projectTree.setModel(new DefaultTreeModel(projectTreeRoot));
		for(int i = 0; i < projectTree.getRowCount(); i++)
			projectTree.expandRow(i);
		String title = currProjectFile == null ? "No project" : currProjectFile.getName();
		projectTitleLabel.setText(title + " - " + projectDiagramFiles.size() + " diagram" + (projectDiagramFiles.size() == 1 ? "" : "s"));
		boolean hasProjectItem = projectDiagramFiles.size() > 0;
		projectOpenButton.setEnabled(hasProjectItem);
		projectBuildButton.setEnabled(hasProjectItem);
		FileProjectSave.setEnabled(currProjectFile != null);
		FileProjectBuildAll.setEnabled(hasProjectItem);
		FileProjectLintAll.setEnabled(hasProjectItem);
		projectLintButton.setEnabled(hasProjectItem);
		selectCurrentProjectFileInTree();
	}

	private void addProjectTreePath(File diagram) {
		String displayPath = projectDisplayPath(diagram).replace('\\', '/');
		String[] parts = displayPath.split("/");
		DefaultMutableTreeNode parent = projectTreeRoot;
		for(int i = 0; i < parts.length; i++)
		{
			if(parts[i].equals(""))
				continue;
			boolean filePart = i == parts.length - 1;
			DefaultMutableTreeNode child = findProjectTreeChild(parent, parts[i], filePart);
			if(child == null)
			{
				String label = parts[i];
				if(filePart && isCurrentDirtyProjectDiagram(diagram))
					label = "*" + label;
				child = new DefaultMutableTreeNode(filePart ? new ProjectTreeFile(label, diagram) : label);
				parent.add(child);
			}
			parent = child;
		}
	}

	private DefaultMutableTreeNode findProjectTreeChild(DefaultMutableTreeNode parent, String label, boolean filePart) {
		for(int i = 0; i < parent.getChildCount(); i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode)parent.getChildAt(i);
			Object user = child.getUserObject();
			if(filePart)
			{
				if(user instanceof ProjectTreeFile && ((ProjectTreeFile)user).label.equals(label))
					return child;
			}
			else if(user instanceof String && user.equals(label))
				return child;
		}
		return null;
	}

	private String projectDisplayPath(File diagram) {
		if(currProjectFile != null)
			return pathRelativeToProject(currProjectFile, diagram);
		return diagram.getPath();
	}

	private void openSelectedProjectDiagram() {
		File file = selectedProjectFile();
		if(file == null)
			return;
		if(!isFizzimFile(file) || !file.exists())
		{
			JOptionPane.showMessageDialog(this, "Project diagram could not be opened:\n" + file.getAbsolutePath(),
					"Project", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(confirmSaveCurrentDiagramBeforeSwitch())
		{
			openFile(file, false);
			rememberLastOpened("project", currProjectFile);
		}
	}

	private void openSelectedProjectDiagramInNewWindow() {
		File file = selectedProjectFile();
		if(file == null)
			return;
		if(!isFizzimFile(file) || !file.exists())
		{
			JOptionPane.showMessageDialog(this, "Project diagram could not be opened:\n" + file.getAbsolutePath(),
					"Project", JOptionPane.ERROR_MESSAGE);
			return;
		}
		openFileInNewWindow(file, false);
		rememberLastOpened("project", currProjectFile);
	}

	private File selectedProjectFile() {
		TreePath path = projectTree.getSelectionPath();
		if(path == null)
			return null;
		DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
		Object user = node.getUserObject();
		if(user instanceof ProjectTreeFile)
			return ((ProjectTreeFile)user).file;
		return null;
	}

	private void removeSelectedProjectDiagram() {
		File file = selectedProjectFile();
		if(file == null)
			return;
		boolean removed = false;
		for(int i = projectDiagramFiles.size() - 1; i >= 0; i--)
			if(sameFile(projectDiagramFiles.get(i), file))
			{
				projectDiagramFiles.remove(i);
				removed = true;
			}
		updateProjectPanel();
		if(removed)
			autosaveProject();
	}

	private void showProjectContextMenu(MouseEvent evt) {
		int row = projectTree.getRowForLocation(evt.getX(), evt.getY());
		if(row < 0)
			return;
		projectTree.setSelectionRow(row);
		if(selectedProjectFile() == null)
			return;
		JPopupMenu menu = new JPopupMenu();
		JMenuItem openItem = new JMenuItem(t("project.open"));
		openItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openSelectedProjectDiagram();
			}
		});
		menu.add(openItem);
		JMenuItem openNewItem = new JMenuItem(t("project.context.openNew"));
		openNewItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openSelectedProjectDiagramInNewWindow();
			}
		});
		menu.add(openNewItem);
		menu.addSeparator();
		JMenuItem removeItem = new JMenuItem(t("project.context.remove"));
		removeItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				removeSelectedProjectDiagram();
			}
		});
		menu.add(removeItem);
		menu.show(projectTree, evt.getX(), evt.getY());
	}

	private boolean confirmSaveCurrentDiagramBeforeSwitch() {
		if(drawArea1 == null || !drawArea1.getFileModifed())
			return true;

		Object[] options = { "Save", "Discard", "Cancel" };
		String message = currFile == null ? "Save file before opening another diagram?"
				: "Save changes to " + currFile.getName() + " before opening another diagram?";
		int choice = JOptionPane.showOptionDialog(this, message, APP_TITLE,
				JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);
		if(choice == JOptionPane.YES_OPTION)
		{
			if(currFile != null)
				return saveFile(currFile);
			try {
				FileSaveAction.setCurrentDirectory(new java.io.File(System.getProperty("user.dir")).getAbsoluteFile());
				FileSaveAction.showSaveDialog(this);
			} catch (java.awt.HeadlessException e1) {
				e1.printStackTrace();
			}
			return FileSaveAction.getSelected() && tryToSave(FileSaveAction.getSelectedFile(), "fzm", true);
		}
		return choice == JOptionPane.NO_OPTION;
	}

	private boolean confirmSaveCurrentProjectDiagramBeforeBuild() {
		return confirmSaveCurrentProjectDiagramForDiskOperation("Build All", "Build All");
	}

	private boolean confirmSaveCurrentProjectDiagramBeforeLintAll() {
		return confirmSaveCurrentProjectDiagramForDiskOperation("Lint All", "Lint All");
	}

	private boolean confirmSaveCurrentProjectDiagramForDiskOperation(String title, String operationName) {
		if(currFile == null || !drawArea1.getFileModifed() || !projectContainsFile(currFile))
			return true;
		String message = "Save changes to " + currFile.getName() + " before " + operationName
				+ "?\n" + operationName + " uses diagram files from disk.";
		Object[] options = { "Save", "Cancel" };
		int choice = JOptionPane.showOptionDialog(this, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, options, options[0]);
		return choice == JOptionPane.YES_OPTION && saveFile(currFile);
	}

	private boolean projectContainsFile(File file) {
		for(int i = 0; i < projectDiagramFiles.size(); i++)
			if(sameFile(projectDiagramFiles.get(i), file))
				return true;
		return false;
	}

	private boolean isCurrentDirtyProjectDiagram(File diagram) {
		return currFile != null && drawArea1 != null && drawArea1.getFileModifed() && sameFile(diagram, currFile);
	}

	private boolean sameFile(File a, File b) {
		return a != null && b != null && a.getAbsoluteFile().equals(b.getAbsoluteFile());
	}

	private void selectCurrentProjectFileInTree() {
		if(currFile == null || projectTree == null || projectTreeRoot == null)
			return;
		TreePath path = findProjectTreePath(projectTreeRoot, currFile);
		if(path != null)
		{
			projectTree.setSelectionPath(path);
			projectTree.scrollPathToVisible(path);
		}
	}

	private TreePath findProjectTreePath(DefaultMutableTreeNode node, File target) {
		Object user = node.getUserObject();
		if(user instanceof ProjectTreeFile && sameFile(((ProjectTreeFile)user).file, target))
			return new TreePath(node.getPath());
		for(int i = 0; i < node.getChildCount(); i++)
		{
			TreePath path = findProjectTreePath((DefaultMutableTreeNode)node.getChildAt(i), target);
			if(path != null)
				return path;
		}
		return null;
	}

	private void buildProject() {
		if(projectDiagramFiles.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "The current project has no diagrams.",
					"Build All", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		if(!confirmSaveCurrentProjectDiagramBeforeBuild())
			return;
		StringBuffer report = new StringBuffer();
		int pass = 0;
		int fail = 0;
		for(int i = 0; i < projectDiagramFiles.size(); i++)
		{
			File fzm = projectDiagramFiles.get(i);
			if(!fzm.exists())
			{
				fail++;
				report.append("FAIL ").append(fzm.getAbsolutePath()).append("\n  Missing diagram file\n");
				continue;
			}
			try {
				String machineName = getMachineNameFromFile(fzm);
				File output = resolveHdlOutputFile(fzm, machineName);
				File parent = output.getParentFile();
				if(parent != null && !parent.exists() && !parent.mkdirs())
					throw new IOException("Could not create output directory: " + parent.getAbsolutePath());
				HdlGenerationResult result = runHdlBackend(fzm, output);
				if(result.exitCode != 0)
				{
					fail++;
					report.append("FAIL ").append(fzm.getName()).append("\n  ").append(result.stderr).append("\n");
					continue;
				}
				boolean comparisonOk = true;
				if(getHdlCompareEnabled())
					comparisonOk = runProjectHdlComparison(fzm, output, report);
				if(comparisonOk)
				{
					pass++;
					report.append("PASS ").append(fzm.getName()).append(" -> ")
							.append(pathRelativeToFile(fzm, output)).append("\n");
					if(currFile != null && currFile.getAbsoluteFile().equals(fzm.getAbsoluteFile()))
						markHdlGeneratedInSync(output);
				}
				else
					fail++;
			} catch (Exception ex) {
				fail++;
				report.append("FAIL ").append(fzm.getName()).append("\n  ").append(ex.getMessage()).append("\n");
			}
		}
		JTextArea text = new JTextArea(report.toString(), 22, 90);
		text.setEditable(false);
		text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JOptionPane.showMessageDialog(this, new JScrollPane(text),
				"Build All: " + pass + " passed, " + fail + " failed",
				fail == 0 ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
	}

	private void lintProject() {
		if(projectDiagramFiles.size() == 0)
		{
			JOptionPane.showMessageDialog(this, "The current project has no diagrams.",
					"Lint All", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		if(!confirmSaveCurrentProjectDiagramBeforeLintAll())
			return;

		StringBuffer report = new StringBuffer();
		report.append("Fizzim Project Lint Report\n");
		report.append("==========================\n\n");
		if(currProjectFile != null)
			report.append("Project: ").append(currProjectFile.getAbsolutePath()).append("\n");
		report.append("Diagrams: ").append(projectDiagramFiles.size()).append("\n\n");

		int clean = 0;
		int warn = 0;
		int error = 0;
		int fail = 0;
		for(int i = 0; i < projectDiagramFiles.size(); i++)
		{
			File fzm = projectDiagramFiles.get(i);
			if(!fzm.exists())
			{
				fail++;
				report.append("FAIL ").append(projectDisplayPath(fzm)).append("\n  Missing diagram file\n\n");
				continue;
			}
			try {
				ProjectLintResult result = lintProjectDiagram(fzm);
				if(result.errorCount > 0)
					error++;
				else if(result.warnCount > 0)
					warn++;
				else
					clean++;
				report.append(result.statusLine(projectDisplayPath(fzm))).append("\n");
				if(result.issueCount() > 0)
				{
					for(int j = 0; j < result.issues.size(); j++)
						report.append("  ").append(result.issues.get(j).toString()).append("\n");
				}
				else
					report.append("  No lint issues found.\n");
				report.append("\n");
			} catch (Exception ex) {
				fail++;
				report.append("FAIL ").append(projectDisplayPath(fzm)).append("\n  ")
						.append(ex.getMessage()).append("\n\n");
			}
		}

		report.insert(report.indexOf("\n\n") + 2, "Clean: " + clean + "  Warn: " + warn
				+ "  Error: " + error + "  Failed: " + fail + "\n");
		JTextArea text = new JTextArea(report.toString(), 24, 100);
		text.setEditable(false);
		text.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		text.setCaretPosition(0);
		JOptionPane.showMessageDialog(this, new JScrollPane(text),
				"Lint All: " + clean + " clean, " + warn + " warning, " + error + " error, " + fail + " failed",
				(error == 0 && fail == 0) ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.WARNING_MESSAGE);
	}

	private ProjectLintResult lintProjectDiagram(File fzm) {
		FizzimGui checker = new FizzimGui();
		try {
			checker.openFile(fzm, false);
			String lintReport = checker.drawArea1.lintDiagram();
			ProjectLintResult result = new ProjectLintResult();
			result.report = lintReport;
			LinkedList<DrawArea.LintIssue> issues = checker.drawArea1.getLastLintIssues();
			for(int i = 0; i < issues.size(); i++)
			{
				DrawArea.LintIssue issue = issues.get(i);
				result.issues.add(issue);
				if("ERROR".equals(issue.severity))
					result.errorCount++;
				else if("WARN".equals(issue.severity))
					result.warnCount++;
			}
			return result;
		} finally {
			checker.closeWindow();
		}
	}

	private boolean runProjectHdlComparison(File fzmFile, File primaryOutput, StringBuffer report) throws IOException, InterruptedException {
		File compareOutput = comparisonOutputFile(primaryOutput);
		HdlGenerationResult compare = runConfiguredHdlBackend(compareOutput, getHdlCompareCommand(),
				getHdlCompareBackendPath(), getHdlCompareArgs(), fzmFile);
		if(compare.exitCode != 0)
		{
			report.append("FAIL ").append(fzmFile.getName()).append("\n  Comparison generation failed: ")
					.append(compare.stderr).append("\n");
			return false;
		}
		String diff = diffFiles(primaryOutput, compareOutput);
		File diffFile = new File(primaryOutput.getParentFile(), stripExtension(primaryOutput.getName()) + ".diff.txt");
		if(diff.equals(""))
		{
			if(diffFile.exists())
				diffFile.delete();
			return true;
		}
		Files.write(diffFile.toPath(), diff.getBytes(StandardCharsets.UTF_8));
		report.append("FAIL ").append(fzmFile.getName()).append("\n  Comparison mismatch: ")
				.append(diffFile.getAbsolutePath()).append("\n");
		return false;
	}

	private File ensureProjectExtension(File file) {
		if(file.getName().toLowerCase().endsWith(".fzp"))
			return file;
		return new File(file.getAbsolutePath() + ".fzp");
	}

	private boolean projectExtensionWasAdded(File file) {
		return file != null && !file.getName().toLowerCase().endsWith(".fzp");
	}

	private String pathRelativeToProject(File projectFile, File file) {
		return pathRelativeToDirectory(projectFile.getParentFile(), file);
	}

	private String pathRelativeToFile(File baseFile, File file) {
		return pathRelativeToDirectory(baseFile.getAbsoluteFile().getParentFile(), file);
	}

	private String pathRelativeToDirectory(File directory, File file) {
		try {
			return directory.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).toString();
		} catch (Exception ex) {
			return file.getAbsolutePath();
		}
	}

	private String getMachineNameFromFile(File fzmFile) throws IOException {
		java.util.List<String> lines = Files.readAllLines(fzmFile.toPath(), StandardCharsets.UTF_8);
		boolean inGlobals = false;
		boolean inMachine = false;
		boolean inName = false;
		for(int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();
			if(line.equals("<globals>"))
				inGlobals = true;
			else if(line.equals("</globals>"))
				inGlobals = false;
			else if(inGlobals && line.equals("<machine>"))
				inMachine = true;
			else if(inMachine && line.equals("</machine>"))
				inMachine = false;
			else if(inMachine && line.equals("<name>"))
				inName = true;
			else if(inName && line.equals("</name>"))
				inName = false;
			else if(inName && line.equals("<value>"))
			{
				for(int j = i + 1; j < lines.size(); j++)
				{
					String value = lines.get(j).trim();
					if(value.equals("") || value.startsWith("<status>") || value.startsWith("</status>"))
						continue;
					if(value.startsWith("<"))
						break;
					return value;
				}
			}
		}
		return stripExtension(fzmFile.getName());
	}

	private void storeRecentFiles(LinkedList<File> recentFiles) {
		for(int i = 0; i < RECENT_FILE_LIMIT; i++)
		{
			if(i < recentFiles.size())
				USER_PREFS.put(RECENT_FILE_PREFIX + i, recentFiles.get(i).getAbsolutePath());
			else
				USER_PREFS.remove(RECENT_FILE_PREFIX + i);
		}
		try {
			USER_PREFS.flush();
		} catch (Exception e) { }
	}

	private void storeRecentProjects(LinkedList<File> recentProjects) {
		for(int i = 0; i < RECENT_FILE_LIMIT; i++)
		{
			if(i < recentProjects.size())
				USER_PREFS.put(RECENT_PROJECT_PREFIX + i, recentProjects.get(i).getAbsolutePath());
			else
				USER_PREFS.remove(RECENT_PROJECT_PREFIX + i);
		}
		try {
			USER_PREFS.flush();
		} catch (Exception e) { }
	}

	private void openRecentFile(File selectedFile) {
		if(!isFizzimFile(selectedFile) || !selectedFile.exists())
		{
			JOptionPane.showMessageDialog(this, "Recent file could not be opened:\n" + selectedFile.getAbsolutePath(),
					"error", JOptionPane.ERROR_MESSAGE);
			forgetRecentFile(selectedFile);
			clearLastOpenedIfMatches(selectedFile);
			return;
		}

		if(canReuseThisWindowForOpen())
			openFile(selectedFile);
		else
			openFileInNewWindow(selectedFile);
	}

	private void openRecentProject(File selectedFile) {
		if(!isProjectFile(selectedFile) || !selectedFile.exists())
		{
			String path = selectedFile == null ? "(unknown)" : selectedFile.getAbsolutePath();
			JOptionPane.showMessageDialog(this, "Recent project could not be opened:\n" + path,
					"Project", JOptionPane.ERROR_MESSAGE);
			forgetRecentProject(selectedFile);
			clearLastOpenedIfMatches(selectedFile);
			return;
		}

		openProject(selectedFile);
	}

	private boolean isProjectFile(File file) {
		return file != null && !file.isDirectory() && file.getName().toLowerCase().endsWith(".fzp");
	}

	private void openFileInNewWindow(File selectedFile) {
		openFileInNewWindow(selectedFile, true);
	}

	private void openFileInNewWindow(File selectedFile, boolean rememberFile) {
		FizzimGui newWindow = new FizzimGui();
		newWindow.setSize(getSize());
		newWindow.setLocation(getX() + 30, getY() + 30);
		newWindow.setVisible(true);
		newWindow.openFile(selectedFile, rememberFile);
	}

	private void openFile(File selectedFile) {
		openFile(selectedFile, true);
	}

	private void openFile(File selectedFile, boolean rememberFile) {
		loading = true;
		currFile = selectedFile;
		FileParser fileParser = new FileParser(currFile, this, drawArea1);
		jTabbedPane1.setComponentAt(1,jScrollPane1);
		jTabbedPane1.setSelectedIndex(1);
		drawArea1.setCurrPage(1);
		updateWindowTitle();
		if(rememberFile)
		{
			rememberRecentFile(currFile);
			rememberLastOpened("diagram", currFile);
		}
		loading = false;
		restorePersistedHdlStatus();
		updateLintStatus();
		enterZoomFitMode();
		scheduleFitDiagramToViewport();

	}
	
	//set indentation (also exists in ObjAttribute.java and GeneralObj.java
	public String i(int indent)
	{
		String ind = "";
		for(int i=0; i<indent; i++)
		{
			ind = ind + "   ";
		}
		return ind;
	}
	
	
	private boolean saveFile(File selectedFile) {
		currFile = selectedFile;
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(currFile));

			Date currDate = new Date();
			long currTime = currDate.getTime();
			DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
			DateFormat dt = DateFormat.getTimeInstance(DateFormat.MEDIUM);
			writer.write("## File last modified by Fizzim: " + dt.format(currTime)
					+ " on " + df.format(currDate) + "\n");
			writer.write("<version>\n" + i(1) + currVer + "\n" + "</version>\n");

			// save global lists
			LinkedList<ObjAttribute> tempList;
			writer.write("<globals>\n");

			writer.write(i(1) + "<machine>\n");
			tempList = (LinkedList<ObjAttribute>) globalList.get(0);
			for (int i = 0; i < tempList.size(); i++) {
				ObjAttribute obj = tempList.get(i);
				obj.save(writer,1);
			}
			writer.write(i(1) + "</machine>\n");
			
			writer.write(i(1) + "<inputs>\n");
			tempList = (LinkedList<ObjAttribute>) globalList.get(1);
			for (int i = 0; i < tempList.size(); i++) {
				ObjAttribute obj = tempList.get(i);
				obj.save(writer,1);
			}
			writer.write(i(1) + "</inputs>\n");

			writer.write(i(1) + "<outputs>\n");
			tempList = (LinkedList<ObjAttribute>) globalList.get(2);
			for (int i = 0; i < tempList.size(); i++) {
				ObjAttribute obj = tempList.get(i);
				obj.save(writer,1);
			}
			writer.write(i(1) + "</outputs>\n");

			writer.write(i(1) + "<state>\n");
			tempList = (LinkedList<ObjAttribute>) globalList.get(3);
			for (int i = 0; i < tempList.size(); i++) {
				ObjAttribute obj = tempList.get(i);
				obj.save(writer,1);
			}
			writer.write(i(1) + "</state>\n");

			writer.write(i(1) + "<trans>\n");
			tempList = (LinkedList<ObjAttribute>) globalList.get(4);
			for (int i = 0; i < tempList.size(); i++) {
				ObjAttribute obj = tempList.get(i);
				obj.save(writer,1);
			}
			writer.write(i(1) + "</trans>\n");

			

			writer.write("</globals>\n");
			
			writer.write("<tabs>\n");
			for(int i = 1; i < jTabbedPane1.getTabCount(); i++)
			{
				writer.write(i(1) + jTabbedPane1.getTitleAt(i) + "\n");
			}
			writer.write("</tabs>\n");

			// can save function on draw area, which will loop through objects
			drawArea1.save(writer);

			writer.close();
			drawArea1.setFileModifed(false);
			updateLintStatus();
			
			return true;
			
		} catch (IOException ex) {
			//ex.printStackTrace();
			JOptionPane.showMessageDialog(this,
                    "Error saving file",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
	}
	
	public int getMaxH()
	{
		return maxH;
	}
	public int getMaxW()
	{
		return maxW;
	}
	
	
	
	
	public int getPages()
	{
		return jTabbedPane1.getTabCount();
	}
	

	
	private void removePage(int i)
	{
		//TODO
	}

	/**
	 * @param args the command line arguments
	 */
	static String clfilename = ""; // command-line filename
	static boolean clexit = false; // command-line -exit switch
	static boolean clbatch_rewrite = false; // command-line -batch_rewrite switch

	public static void main(String args[]) {
		// If one of the args ends with .fzm or .fzp, assume it is the file
		// to open.
		for (String s: args) {
			String lower = s.toLowerCase();
			if (lower.endsWith(".fzm") || lower.endsWith(".fzp")) {
				clfilename = s;
			}
			if (s.equals("-exit")) {
				clexit = true;
			}
			if (s.equals("-batch_rewrite")) {
				clbatch_rewrite = true;
			}
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				
				// sets error file to write to
				final File file = new File("fizzim_errors.log");
				
				//make sure output file doesnt get too large
				FileOutputStream fout = null;
				try {
					fout = new FileOutputStream ( file , true ){
						
						
						public void write(byte[] b, int off, int len) throws IOException
						{
							if(file.length() < 20000)
								super.write(b,off,len);
							System.out.write(b, off, len);
						}
						public void write(byte[] b) throws IOException
						{
							if(file.length() < 20000)
								super.write(b);
							System.out.write(b);
						}
						public void write(int b) throws IOException
						{
							if(file.length() < 20000)
								super.write(b);
							System.out.write(b);
						}
					};
				} catch (FileNotFoundException e) {	}
				
				//sets std err to be written to file
				System.setErr ( new PrintStream (fout));

				FizzimGui fzim = new FizzimGui();
				fzim.setVisible(true);
				fzim.setSize(new java.awt.Dimension(1000, 685));
				fzim.new SplashWindow("splash.png",fzim,3500);
				// If command line filename is not null, open
				// this file.
				if (clfilename != "") {
					System.err.println("Opening file " + clfilename);
					File clfile = new File(clfilename);
					String lowerClfilename = clfilename.toLowerCase();
					if(lowerClfilename.endsWith(".fzp"))
						fzim.openProject(clfile);
					else
						fzim.openFile(clfile);
					if (clbatch_rewrite && lowerClfilename.endsWith(".fzm")) {
						System.err.println("Saving file " + clfilename);
						if (fzim.tryToSave(new File(clfilename), "fzm", false)) {
							System.err.println("Exiting");
							fzim.formWindowClosing();
						}
					}
				}
				else if(!clexit) {
					fzim.reopenLastOpenedItem();
				}
				if (clexit) {
					fzim.formWindowClosing();
				}

			}
		});
	}
	public String getclfilename () {
	  return clfilename;
	}


	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JMenuItem EditItemDelete;
	private javax.swing.JMenuItem EditItemRedo;
	private javax.swing.JMenuItem EditItemUndo;
	private javax.swing.JMenu EditMenu;
	private javax.swing.JMenu settingsMenu;
	private javax.swing.JMenuItem defaultsItem;
	private javax.swing.JMenuItem hdlSettingsItem;
	private javax.swing.JMenu languageMenu;
	private javax.swing.JRadioButtonMenuItem languageEnglishItem;
	private javax.swing.JRadioButtonMenuItem languageJapaneseItem;
	private javax.swing.JRadioButtonMenuItem languageChineseSimplifiedItem;
	private javax.swing.JRadioButtonMenuItem languageChineseTraditionalItem;
	private javax.swing.JRadioButtonMenuItem languageKoreanItem;
	private javax.swing.JRadioButtonMenuItem languageGermanItem;
	private javax.swing.JRadioButtonMenuItem languageFrenchItem;
	private javax.swing.JRadioButtonMenuItem languageSpanishItem;
	private javax.swing.JRadioButtonMenuItem languagePortugueseItem;
	private javax.swing.JRadioButtonMenuItem languageHindiItem;
	private javax.swing.JRadioButtonMenuItem languageRussianItem;
	private javax.swing.JMenu toolsMenu;
	private javax.swing.JMenuItem lintItem;
	private javax.swing.JMenuItem generateHdlItem;
	private javax.swing.JMenu cleanupMenu;
	private javax.swing.JMenuItem resetLabelsItem;
	private javax.swing.JMenuItem cleanRoutesItem;
	private javax.swing.JMenuItem cleanSelectedRoutesItem;
	private javax.swing.JMenuItem alignHorizontalItem;
	private javax.swing.JMenuItem alignVerticalItem;
	private javax.swing.JMenuItem distributeHorizontalItem;
	private javax.swing.JMenuItem distributeVerticalItem;
	private javax.swing.JMenuItem FileItemExit;
	private javax.swing.JMenuItem FileItemNew;
	private javax.swing.JMenuItem FileItemOpen;
	private javax.swing.JMenu FileOpenRecent;
	private javax.swing.JMenu FileProjectMenu;
	private javax.swing.JMenuItem FileProjectNew;
	private javax.swing.JMenuItem FileProjectOpen;
	private javax.swing.JMenu FileProjectOpenRecent;
	private javax.swing.JMenuItem FileProjectSave;
	private javax.swing.JMenuItem FileProjectSaveAs;
	private javax.swing.JMenuItem FileProjectAddCurrent;
	private javax.swing.JMenuItem FileProjectAddDiagrams;
	private javax.swing.JMenuItem FileProjectBuildAll;
	private javax.swing.JMenuItem FileProjectLintAll;
	private javax.swing.JMenuItem FilePref;
	private javax.swing.JMenuItem FileItemSave;
	private javax.swing.JMenuItem FileItemSaveAs;
	private javax.swing.JMenu FileExport;
	private javax.swing.JMenuItem FileExportClipboard;
	private javax.swing.JMenuItem FileExportPNG;
	private javax.swing.JMenuItem FileExportJPEG;
	private javax.swing.JMenu FileMenu;
	private MyJFileChooser FileOpenAction;
	private MyJFileChooser ProjectOpenAction;
	private MyJFileChooser ProjectSaveAction;
	private MyJFileChooser FileSaveAction;
	private MyJFileChooser ExportChooser;
	private javax.swing.JTabbedPane sideTabbedPane;
	private javax.swing.JMenuItem GlobalItemInputs;
	private javax.swing.JMenuItem GlobalItemInternals;
	private javax.swing.JMenuItem GlobalItemMachine;
	private javax.swing.JMenuItem GlobalItemOutputs;
	private javax.swing.JMenuItem GlobalItemParameters;
	private javax.swing.JMenuItem GlobalItemStates;
	private javax.swing.JMenuItem GlobalItemTransitions;
	private javax.swing.JMenu GlobalMenu;
	private javax.swing.JMenuItem HelpItemAbout;
	private javax.swing.JMenuItem HelpItemHelp;
	private javax.swing.JMenu HelpMenu;
	private javax.swing.JMenuBar MenuBar;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel zoomPanel;
	private javax.swing.JButton zoomOutButton;
	private javax.swing.JButton zoomInButton;
	private javax.swing.JButton zoomFitButton;
	private javax.swing.JButton lintButton;
	private javax.swing.JButton generateHdlButton;
	private javax.swing.JLabel hdlStatusLabel;
	private javax.swing.JLabel lintStatusLabel;
	private javax.swing.JLabel zoomPercentLabel;
	private javax.swing.JLabel selectionStatusLabel;
	private javax.swing.JPanel propertyInspectorPanel;
	private javax.swing.JLabel propertyInspectorTitle;
	private javax.swing.JTable propertyInspectorTable;
	private javax.swing.JScrollPane propertyInspectorScroll;
	private javax.swing.JButton propertyInspectorEditButton;
	private javax.swing.JPanel projectPanel;
	private javax.swing.JLabel projectTitleLabel;
	private DefaultMutableTreeNode projectTreeRoot;
	private javax.swing.JTree projectTree;
	private javax.swing.JScrollPane projectScroll;
	private javax.swing.JPanel projectButtonPanel;
	private javax.swing.JButton projectOpenButton;
	private javax.swing.JButton projectAddButton;
	private javax.swing.JButton projectBuildButton;
	private javax.swing.JButton projectLintButton;
	private LinkedList<GeneralObj> inspectorSelectedObjects = new LinkedList<GeneralObj>();
	private javax.swing.JPanel lintPanel;
	private javax.swing.DefaultListModel lintIssueModel;
	private javax.swing.JList lintIssueList;
	private javax.swing.JTextArea lintReportText;
	private javax.swing.JTabbedPane lintTabs;
	private javax.swing.JButton lintRerunButton;
	private javax.swing.JButton lintCloseButton;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JSeparator jSeparator1;
	private javax.swing.JSeparator jSeparator2;
	private javax.swing.JSeparator jSeparator3;
	private javax.swing.JSeparator jSeparator4;
	private MyJTabbedPane jTabbedPane1;
	// End of variables declaration//GEN-END:variables

	File currFile = null;
	File currProjectFile = null;
	LinkedList<File> projectDiagramFiles = new LinkedList<File>();

	private static class ProjectTreeFile {
		String label;
		File file;

		ProjectTreeFile(String labelText, File diagramFile) {
			label = labelText;
			file = diagramFile;
		}

		public String toString() {
			return label;
		}
	}

	private static class ProjectLintResult {
		String report;
		int errorCount;
		int warnCount;
		LinkedList<DrawArea.LintIssue> issues = new LinkedList<DrawArea.LintIssue>();

		int issueCount() {
			return errorCount + warnCount;
		}

		String statusLine(String displayPath) {
			if(errorCount > 0)
				return "ERROR " + displayPath + " (" + errorCount + " error, " + warnCount + " warning)";
			if(warnCount > 0)
				return "WARN  " + displayPath + " (" + warnCount + " warning)";
			return "PASS  " + displayPath;
		}
	}
	private DrawArea drawArea1;
	private boolean closed = false;

	public void updateGlobal(LinkedList<LinkedList<ObjAttribute>> globalList2) {
		globalList = globalList2;

	}

	public static String getDefaultClockName() {
		return USER_PREFS.get(PREF_DEFAULT_CLOCK, "clk");
	}

	public static String getDefaultClockEdge() {
		return USER_PREFS.get(PREF_DEFAULT_CLOCK_EDGE, "posedge");
	}

	public static String getDefaultResetName() {
		return USER_PREFS.get(PREF_DEFAULT_RESET, "rst_l");
	}

	public static String getDefaultResetEdge() {
		return USER_PREFS.get(PREF_DEFAULT_RESET_EDGE, "negedge");
	}

	public static boolean getDefaultImpliedLoopback() {
		return USER_PREFS.getBoolean(PREF_DEFAULT_IMPLIED_LOOPBACK, true);
	}

	class InspectorTableModel extends MyTableModel {
		private DrawArea drawArea;
		private GeneralObj currentObj;
		private boolean editing = false;
		private int[] rowToAttribute;
		private int[] rowToColumn;
		private String[] inspectorColumns = {"Field", "Value"};

		InspectorTableModel(DrawArea da, GeneralObj obj, LinkedList<LinkedList<ObjAttribute>> globals) {
			super(obj, null, globals, (obj.getType() == 1 || obj.getType() == 2) ? 4 : 3);
			drawArea = da;
			currentObj = obj;
			buildRows();
		}

		private void buildRows() {
			LinkedList<Integer> attrs = new LinkedList<Integer>();
			LinkedList<Integer> cols = new LinkedList<Integer>();
			addInspectorRow(attrs, cols, "name", 1);
			if(currentObj.getType() == 1 || currentObj.getType() == 2)
			{
				addInspectorRow(attrs, cols, "equation", 1);
				addInspectorRow(attrs, cols, "priority", 1);
				addOutputRows(attrs, cols);
			}
			else if(currentObj.getType() == 0 || currentObj.getType() == 5)
			{
				addVisibleAttributeRows(attrs, cols);
				addOutputRows(attrs, cols);
			}
			rowToAttribute = new int[attrs.size()];
			rowToColumn = new int[cols.size()];
			for(int i = 0; i < attrs.size(); i++)
			{
				rowToAttribute[i] = attrs.get(i).intValue();
				rowToColumn[i] = cols.get(i).intValue();
			}
		}

		private void addInspectorRow(LinkedList<Integer> attrs, LinkedList<Integer> cols, String attrName, int col) {
			for(int i = 0; i < attrib.size(); i++)
			{
				if(attrib.get(i).getName().equals(attrName))
				{
					attrs.add(new Integer(i));
					cols.add(new Integer(col));
					return;
				}
			}
		}

		private void addVisibleAttributeRows(LinkedList<Integer> attrs, LinkedList<Integer> cols) {
			for(int i = 0; i < attrib.size(); i++)
			{
				ObjAttribute attr = attrib.get(i);
				if(attr.getVisible() && !attr.getName().equals("name") && !attr.getType().equals("output"))
				{
					attrs.add(new Integer(i));
					cols.add(new Integer(1));
				}
			}
		}

		private void addOutputRows(LinkedList<Integer> attrs, LinkedList<Integer> cols) {
			for(int i = 0; i < attrib.size(); i++)
			{
				ObjAttribute attr = attrib.get(i);
				if(attr.getType().equals("output"))
				{
					attrs.add(new Integer(i));
					cols.add(new Integer(1));
				}
			}
		}

		public int getColumnCount() {
			return inspectorColumns.length;
		}

		public int getRowCount() {
			return rowToAttribute.length;
		}

		public String getColumnName(int col) {
			return inspectorColumns[col];
		}

		public Object getValueAt(int row, int col) {
			if(col == 0)
				return inspectorDisplayName(currentObj, attrib.get(rowToAttribute[row]));
			return super.getValueAt(rowToAttribute[row], rowToColumn[row]);
		}

		public boolean isCellEditable(int row, int col) {
			return col == 1 && super.isCellEditable(rowToAttribute[row], rowToColumn[row]);
		}

		public void setValueAt(Object value, int row, int col) {
			if(col != 1)
				return;
			if(editing)
			{
				super.setValueAt(value, rowToAttribute[row], rowToColumn[row]);
				return;
			}
			editing = true;
			GeneralObj replacement = drawArea.prepareInspectorEdit(currentObj);
			if(replacement != null)
			{
				currentObj = replacement;
				obj = replacement;
				attrib = replacement.getAttributeList();
				buildRows();
			}
			super.setValueAt(value, rowToAttribute[row], rowToColumn[row]);
			drawArea.finishInspectorEdit();
			editing = false;
		}
	}

	class BatchInspectorTableModel extends javax.swing.table.AbstractTableModel {
		private DrawArea drawArea;
		private LinkedList<GeneralObj> objects;
		private LinkedList<InspectorRow> rows = new LinkedList<InspectorRow>();
		private String[] inspectorColumns = {"Field", "Value"};
		private boolean editing = false;
		private static final String MIXED_VALUE = "<mixed>";

		BatchInspectorTableModel(DrawArea da, LinkedList<GeneralObj> selected, LinkedList<LinkedList<ObjAttribute>> globals) {
			drawArea = da;
			objects = new LinkedList<GeneralObj>(selected);
			buildRows();
		}

		private void buildRows() {
			rows.clear();
			boolean allTransitions = true;
			boolean allStateLike = true;
			for(int i = 0; i < objects.size(); i++)
			{
				GeneralObj obj = objects.get(i);
				if(obj.getType() != 1 && obj.getType() != 2)
					allTransitions = false;
				if(obj.getType() != 0 && obj.getType() != 5)
					allStateLike = false;
			}
			if(allTransitions)
			{
				addCommonRow("equation", 1);
				addCommonRow("priority", 1);
			}
			if(allStateLike)
				addCommonVisibleRows();
			addCommonOutputRows();
		}

		private void addCommonVisibleRows() {
			if(objects.size() == 0)
				return;
			LinkedList<ObjAttribute> attrs = objects.getFirst().getAttributeList();
			for(int i = 0; i < attrs.size(); i++)
			{
				ObjAttribute attr = attrs.get(i);
				if(attr.getVisible() && !attr.getName().equals("name") && !attr.getType().equals("output"))
					addCommonRow(attr.getName(), 1);
			}
		}

		private void addCommonOutputRows() {
			if(objects.size() == 0)
				return;
			LinkedList<ObjAttribute> attrs = objects.getFirst().getAttributeList();
			for(int i = 0; i < attrs.size(); i++)
			{
				ObjAttribute attr = attrs.get(i);
				if(attr.getType().equals("output"))
					addCommonRow(attr.getName(), 1);
			}
		}

		private void addCommonRow(String attrName, int col) {
			for(int i = 0; i < rows.size(); i++)
			{
				if(rows.get(i).name.equals(attrName) && rows.get(i).col == col)
					return;
			}
			for(int i = 0; i < objects.size(); i++)
			{
				if(findAttribute(objects.get(i), attrName) == null)
					return;
			}
			rows.add(new InspectorRow(attrName, inspectorDisplayName(objects.getFirst(), findAttribute(objects.getFirst(), attrName)), col));
		}

		public int getColumnCount() {
			return inspectorColumns.length;
		}

		public int getRowCount() {
			return rows.size();
		}

		public String getColumnName(int col) {
			return inspectorColumns[col];
		}

		public Object getValueAt(int row, int col) {
			InspectorRow inspectorRow = rows.get(row);
			if(col == 0)
				return inspectorRow.displayName;
			String value = null;
			for(int i = 0; i < objects.size(); i++)
			{
				ObjAttribute attr = findAttribute(objects.get(i), inspectorRow.name);
				String attrValue = attr == null ? "" : String.valueOf(attr.get(inspectorRow.col));
				if(value == null)
					value = attrValue;
				else if(!value.equals(attrValue))
					return MIXED_VALUE;
			}
			return value == null ? "" : value;
		}

		public boolean isCellEditable(int row, int col) {
			if(col != 1)
				return false;
			InspectorRow inspectorRow = rows.get(row);
			for(int i = 0; i < objects.size(); i++)
			{
				ObjAttribute attr = findAttribute(objects.get(i), inspectorRow.name);
				if(attr == null || !isInspectorCellEditable(attr, inspectorRow.col))
					return false;
			}
			return true;
		}

		public void setValueAt(Object value, int row, int col) {
			if(col != 1 || editing)
				return;
			editing = true;
			InspectorRow inspectorRow = rows.get(row);
			LinkedList<GeneralObj> replacements = drawArea.prepareInspectorBatchEdit(objects);
			if(replacements.size() > 0)
				objects = replacements;
			for(int i = 0; i < objects.size(); i++)
			{
				ObjAttribute attr = findAttribute(objects.get(i), inspectorRow.name);
				if(attr != null)
					setInspectorAttribute(objects.get(i), attr, inspectorRow.col, value);
			}
			drawArea.finishInspectorEdit();
			buildRows();
			fireTableDataChanged();
			editing = false;
		}

		private ObjAttribute findAttribute(GeneralObj obj, String name) {
			LinkedList<ObjAttribute> attrs = obj.getAttributeList();
			for(int i = 0; i < attrs.size(); i++)
			{
				ObjAttribute attr = attrs.get(i);
				if(attr.getName().equals(name))
					return attr;
			}
			return null;
		}

		private boolean isInspectorCellEditable(ObjAttribute attr, int col) {
			return attr.getEditable(col) != ObjAttribute.ABS && attr.getEditable(col) != ObjAttribute.GLOBAL_FIXED;
		}

		private void setInspectorAttribute(GeneralObj obj, ObjAttribute attr, int col, Object value) {
			attr.set(col, value);
			if(col == 1)
				attr.setEditable(col, matchesGlobalDefault(obj, attr, col, value) ? ObjAttribute.GLOBAL_VAR : ObjAttribute.LOCAL);
		}

		private boolean matchesGlobalDefault(GeneralObj obj, ObjAttribute attr, int col, Object value) {
			int tab = (obj.getType() == 1 || obj.getType() == 2) ? 4 : 3;
			LinkedList<ObjAttribute> globalAttrs = globalList.get(tab);
			for(int i = 0; i < globalAttrs.size(); i++)
			{
				ObjAttribute globalAttr = globalAttrs.get(i);
				if(globalAttr.getName().equals(attr.getName()))
					return String.valueOf(globalAttr.get(col)).equals(String.valueOf(value));
			}
			return false;
		}
	}

	class InspectorRow {
		String name;
		String displayName;
		int col;

		InspectorRow(String attrName, String attrDisplayName, int attrCol) {
			name = attrName;
			displayName = attrDisplayName == null ? attrName : attrDisplayName;
			col = attrCol;
		}
	}

	private String inspectorDisplayName(GeneralObj obj, ObjAttribute attr) {
		if(attr == null)
			return "";
		if(obj != null && (obj.getType() == 1 || obj.getType() == 2))
		{
			if(attr.getName().equals("equation"))
				return "Condition: equation";
			if(attr.getType().equals("output"))
				return "Action: " + attr.getName();
		}
		return attr.getName();
	}

	class ReadOnlyInspectorTableModel extends javax.swing.table.DefaultTableModel {
		ReadOnlyInspectorTableModel(Object[][] data, Object[] columns) {
			super(data, columns);
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}
	}
	
	public String getPageName(int i)
	{
		return jTabbedPane1.getTitleAt(i);
	}

	public void showPage(int page)
	{
		if(page <= 0 || page >= jTabbedPane1.getTabCount())
			return;
		jTabbedPane1.setSelectedIndex(page);
		drawArea1.setCurrPage(page);
		jTabbedPane1.setComponentAt(page, jScrollPane1);
	}

	public int getPageIndex(String name) {
		for(int i = 1; i < jTabbedPane1.getTabCount(); i++)
		{
			if(jTabbedPane1.getTitleAt(i).equals(name))
				return i;
		}
		return -1;
	}

	
	public void setDASize(int w, int h)
	{
		maxW = w;
		maxH = h;

		// Page setup is kept for legacy page connectors, but the editor canvas
		// now sizes itself from the diagram extents.
		drawArea1.updatePageConn();
		drawArea1.updateCanvasExtents();

		jTabbedPane1.setMinimumSize(new Dimension(100, 100));
		jTabbedPane1.doLayout();
		jPanel3.doLayout();
		repaint();
		
		
	}
	
	class MyJFileChooser extends JFileChooser	{
		
		boolean selected;
		
		MyJFileChooser(String type)
		{
			if(type.equals("fzm"))
				setFileFilter(new FzmFilter());
		}

		public void approveSelection()
		{
			selected = true;
			super.approveSelection();
		}
		public void cancelSelection()
		{
			selected = false;	
			super.cancelSelection();
		}
		public boolean getSelected()
		{
			return selected;
		}
	};
	
	// following 3 classes use code from: http://forum.java.sun.com/thread.jspa?threadID=337070&messageID=1429062

	
	class MyJTabbedPane extends JTabbedPane implements MouseListener, MouseMotionListener {

		private int dragPageTab = -1;
		private boolean dragStartedOnCloseIcon = false;

		public MyJTabbedPane() {
			super();
			this.setUI(new MyBasicTabbedPaneUI());
			addMouseListener(this);
			addMouseMotionListener(this);
		}

		public void addTab(String title, Component component) {
			this.addTab(title, null, component);
		}

		public void addBlankTab(String title, Component component) {
			super.addTab(title, null, component);
		}

		public void addTab(String title, Icon extraIcon, Component component) {
			super.addTab(title, new CloseTabIcon(extraIcon), component);
		}

		public void mouseClicked(MouseEvent arg0) {
			int tab = getUI().tabForCoordinate(this, arg0.getX(), arg0.getY());
			if(tab < 2)
				return;
			Rectangle rect = ((CloseTabIcon)getIconAt(tab)).getBounds();
			if(rect.contains(arg0.getX(), arg0.getY())) {
				if(JOptionPane.showConfirmDialog(this,
						"Everything on this page will be permanently deleted. The undo/redo list will be reset. Delete Tab?", "Close Tab Option",
						JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
				{
					this.setSelectedIndex(tab - 1);
					this.removeTabAt(tab);
					drawArea1.removePage(tab);
					drawArea1.resetUndo();
				}
			}
		}

		public void mouseEntered(MouseEvent arg0) {}
		public void mouseExited(MouseEvent arg0) {}
		public void mousePressed(MouseEvent arg0) {
			dragPageTab = -1;
			dragStartedOnCloseIcon = false;
			if(arg0.getButton() != MouseEvent.BUTTON1)
				return;
			int tab = getUI().tabForCoordinate(this, arg0.getX(), arg0.getY());
			if(tab < 1)
				return;
			if(tab >= 2 && getIconAt(tab) instanceof CloseTabIcon)
			{
				Rectangle rect = ((CloseTabIcon)getIconAt(tab)).getBounds();
				dragStartedOnCloseIcon = rect.contains(arg0.getX(), arg0.getY());
			}
			if(!dragStartedOnCloseIcon)
				dragPageTab = tab;
		}
		public void mouseReleased(MouseEvent arg0) {
			if(dragPageTab >= 1 && !dragStartedOnCloseIcon)
			{
				int targetTab = getUI().tabForCoordinate(this, arg0.getX(), arg0.getY());
				if(targetTab >= 1 && targetTab != dragPageTab)
					movePageTab(dragPageTab, targetTab);
			}
			dragPageTab = -1;
			dragStartedOnCloseIcon = false;
		}
		public void mouseDragged(MouseEvent arg0) {
			if(dragPageTab >= 1 && !dragStartedOnCloseIcon)
				setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}
		public void mouseMoved(MouseEvent arg0) {
			setCursor(Cursor.getDefaultCursor());
		}

	}

	public class MyBasicTabbedPaneUI extends BasicTabbedPaneUI {


		public MyBasicTabbedPaneUI() {
		}
		protected void layoutLabel(int tabPlacement, FontMetrics metrics,
		int tabIndex, String title, Icon icon, Rectangle tabRect,
		Rectangle iconRect, Rectangle textRect, boolean isSelected) {

		textRect.x = 0; textRect.y = 0;
		iconRect.x = 0; iconRect.y = 0;
		SwingUtilities.layoutCompoundLabel((JComponent) tabPane, metrics,
		title, icon, SwingUtilities.CENTER, SwingUtilities.CENTER,
		SwingUtilities.CENTER, SwingUtilities.LEFT, tabRect, iconRect,
		textRect, textIconGap + 2);

		}
	}

	
	class CloseTabIcon implements Icon {
		  private int x_pos;
		  private int y_pos;
		  private int width;
		  private int height;
		  private Icon fileIcon;
		 
		  public CloseTabIcon(Icon fileIcon) {
		    this.fileIcon=fileIcon;
		    width=16;
		    height=16;
		  }
		 
		  public void paintIcon(Component c, Graphics g, int x, int y) {
		    this.x_pos=x;
		    this.y_pos=y;
		 
		    Color col=g.getColor();
		 

		    int y_p=y+2;
		    g.setColor(Color.gray);
		   // g.drawLine(x+1, y_p, x+12, y_p);
		    //g.drawLine(x+1, y_p+13, x+12, y_p+13);
		    //g.drawLine(x, y_p+1, x, y_p+12);
		    //g.drawLine(x+13, y_p+1, x+13, y_p+12);
		    g.setColor(Color.black);
		    g.drawLine(x+3, y_p+3, x+10, y_p+10);
		    g.drawLine(x+3, y_p+4, x+9, y_p+10);
		    g.drawLine(x+4, y_p+3, x+10, y_p+9);
		    g.drawLine(x+10, y_p+3, x+3, y_p+10);
		    g.drawLine(x+10, y_p+4, x+4, y_p+10);
		    g.drawLine(x+9, y_p+3, x+3, y_p+9);
		    g.setColor(col);
		    if (fileIcon != null) {
		      fileIcon.paintIcon(c, g, x+width, y_p);
		    }
		  }
		 
		  public int getIconWidth() {
		    return width + (fileIcon != null? fileIcon.getIconWidth() : 0);
		  }
		 
		  public int getIconHeight() {
		    return height;
		  }
		 
		  public Rectangle getBounds() {
		    return new Rectangle(x_pos, y_pos, width, height);
		  }

	}
	
	class PageSetup extends javax.swing.JDialog {

		FizzimGui fizzim;
		/** Creates new form PageSetup */
		public PageSetup(FizzimGui _fizzim, boolean modal) {
			super(_fizzim, modal);
			fizzim = _fizzim;
			initComponents();
		}

		/** This method is called from within the constructor to
		 * initialize the form.
		 * WARNING: Do NOT modify this code. The content of this method is
		 * always regenerated by the Form Editor.
		 */
		//GEN-BEGIN:initComponents
		// <editor-fold defaultstate="collapsed" desc=" Generated Code ">
		private void initComponents() {
			jLabel1 = new javax.swing.JLabel();
			jLabel2 = new javax.swing.JLabel();
			jTextField1 = new javax.swing.JTextField();
			jTextField2 = new javax.swing.JTextField();
			jLabel3 = new javax.swing.JLabel();
			jLabel4 = new javax.swing.JLabel();
			jButton1 = new javax.swing.JButton();
			jButton2 = new javax.swing.JButton();
			jLabel5 = new javax.swing.JLabel();

			setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
			jLabel1.setText("Width:");
			
			this.setTitle("Page Setup");

			jLabel2.setText("Height:");

			jTextField1.setText(String.valueOf(fizzim.getMaxW()));
			jTextField1.setColumns(4);
			jTextField2.setText(String.valueOf(fizzim.getMaxH()));
			jTextField2.setColumns(4);
			
			jLabel3.setText("pixels");

			jLabel4.setText("pixels");

			jButton1.setText("Cancel");
			jButton1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					jButton1ActionPerformed(evt);
				}
			});

			jButton2.setText("OK");
			jButton2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					jButton2ActionPerformed(evt);
				}
			});

			jLabel5.setText("Enter new dimensions:");

			org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
					getContentPane());
			getContentPane().setLayout(layout);
			layout
					.setHorizontalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									org.jdesktop.layout.GroupLayout.TRAILING,
									layout
											.createSequentialGroup()
											.addContainerGap(23, Short.MAX_VALUE)
											.add(jButton2)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(jButton1).addContainerGap())
							.add(
									layout.createSequentialGroup()
											.addContainerGap().add(jLabel5)
											.addContainerGap(33, Short.MAX_VALUE))
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING)
															.add(jLabel1).add(
																	jLabel2))
											.add(1, 1, 1)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING)
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					jTextField2,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					jLabel4))
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					jTextField1,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																					org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																					org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					jLabel3)))
											.addContainerGap(
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													Short.MAX_VALUE)));
			layout
					.setVerticalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									org.jdesktop.layout.GroupLayout.TRAILING,
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(jLabel5)
											.add(17, 17, 17)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.BASELINE)
															.add(jLabel1)
															.add(
																	jTextField1,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
															.add(jLabel3))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.BASELINE)
															.add(jLabel2)
															.add(
																	jTextField2,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
															.add(jLabel4))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED,
													16, Short.MAX_VALUE)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.BASELINE)
															.add(jButton1).add(
																	jButton2))
											.addContainerGap()));
			pack();
		}// </editor-fold>//GEN-END:initComponents

		//GEN-FIRST:event_jButton1ActionPerformed
		private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {
			dispose();
		}//GEN-LAST:event_jButton1ActionPerformed

		//GEN-FIRST:event_jButton2ActionPerformed
		private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {
			if (JOptionPane.showConfirmDialog(this,
		    			"You cannot undo the page resize action.  You can however set the size back to the original, but you may need to move objects back to their original location. Continue?", "Resize Page",
		    			JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
			{
				try {
					int w = Integer.parseInt(jTextField1.getText());
					int h = Integer.parseInt(jTextField2.getText());
					fizzim.setDASize(w, h);
				}
				catch (NumberFormatException nfe) {
					JOptionPane.showMessageDialog(this,
		                    "Integers only.",
		                    "error",
		                    JOptionPane.ERROR_MESSAGE);
				}
			}
			dispose();
		}//GEN-LAST:event_jButton2ActionPerformed
		
		
		//GEN-BEGIN:variables
		// Variables declaration - do not modify
		private javax.swing.JButton jButton1;
		private javax.swing.JButton jButton2;
		private javax.swing.JLabel jLabel1;
		private javax.swing.JLabel jLabel2;
		private javax.swing.JLabel jLabel3;
		private javax.swing.JLabel jLabel4;
		private javax.swing.JLabel jLabel5;
		private javax.swing.JTextField jTextField1;
		private javax.swing.JTextField jTextField2;
		// End of variables declaration//GEN-END:variables
	}
	
	class ImageToClip implements ClipboardOwner
	{
		private Clipboard clip;
		
		public ImageToClip(BufferedImage bi)
		{
			clip = Toolkit.getDefaultToolkit().getSystemClipboard();

				BiToClip clipIm = new BiToClip(bi);
				clip.setContents(clipIm, this);

		}
		
		public void lostOwnership(Clipboard arg0, Transferable arg1) {
			
		}
		
	}
	
	class BiToClip implements Transferable 
	{
		private DataFlavor[] myFlavors = new DataFlavor[]{DataFlavor.imageFlavor};
		private BufferedImage image;
		
		public BiToClip(BufferedImage bi)
		{
			image = bi;
		}
		
		public Object getTransferData(DataFlavor arg0)
				throws UnsupportedFlavorException, IOException {
			if (arg0 != DataFlavor.imageFlavor) {
				throw new UnsupportedFlavorException(arg0);
			}
			return image;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return myFlavors;
		}

		public boolean isDataFlavorSupported(DataFlavor arg0) {
			return (arg0 == DataFlavor.imageFlavor);
		}
	}
	

	//http://www.javaworld.com/javaworld/javatips/jw-javatip104.html?page=2
	class SplashWindow extends JWindow
	{
	    public SplashWindow(String filename, FizzimGui fzim, int waitTime)
	    {
	    	//splash icon
	    	JLabel label = new JLabel(new ImageIcon(getClass().getResource("splash.png")));	         
	    	Dimension labelSize = label.getPreferredSize();
	    	label.setBounds(0, 0,(int) labelSize.getWidth(), (int)labelSize.getHeight());
	    	
	    	JLabel vers = new JLabel("v" + currVer, SwingConstants.CENTER);
	    	vers.setFont(new Font("Segoe UI",Font.BOLD,14));
	    	vers.setForeground(new Color(66, 100, 116));
	    	Dimension versSize = vers.getPreferredSize();
	    	vers.setBounds(0, (int)labelSize.getHeight() - (int)versSize.getHeight() - 16,
	    			(int)labelSize.getWidth(), (int)versSize.getHeight());
	    	
	        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	        
	        JLayeredPane layer = new JLayeredPane();
	        layer.setPreferredSize(labelSize);
	        layer.setOpaque(false);
	        layer.add(label, new Integer(0));
	        layer.add(vers, new Integer(1));
	        getContentPane().add(layer);

	        pack();
	        layer.setLocation(screenSize.width/2 - (labelSize.width/2),
                    screenSize.height/2 - (labelSize.height/2));

	        setLocation(screenSize.width/2 - (labelSize.width/2),
	                    screenSize.height/2 - (labelSize.height/2));
	        addMouseListener(new MouseAdapter()
	            {
	                public void mousePressed(MouseEvent e)
	                {
	                    setVisible(false);
	                    dispose();
	                }
	            });
	        final int pause = waitTime;
	        final Runnable closerRunner = new Runnable()
	            {
	                public void run()
	                {
	                    setVisible(false);
	                    dispose();
	                }
	            };
	        Runnable waitRunner = new Runnable()
	            {
	                public void run()
	                {
	                    try
	                        {
	                            Thread.sleep(pause);
	                            SwingUtilities.invokeAndWait(closerRunner);
	                        }
	                    catch(Exception e)
	                        {
	                            e.printStackTrace();
	                            // can catch InvocationTargetException
	                            // can catch InterruptedException
	                        }
	                }
	            };
	            
	        setVisible(true);
	        layer.setVisible(true);
	        
	        Thread splashThread = new Thread(waitRunner, "SplashThread");
	        splashThread.start();

	    }
	}
	
	
	/*
	public String getIndent(int i)
	{
		char[] temp = new char[(i*2)];
		Arrays.fill(temp, ' ');
		return temp.toString();
	}
	*/
		

}

class MyHyperlinkListener implements HyperlinkListener
{
    JEditorPane htmlPage;
    public MyHyperlinkListener(JEditorPane pane)
    {
    	htmlPage = pane;
    }
	
	public void hyperlinkUpdate(HyperlinkEvent e) {
      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
    	  try {
    	        htmlPage.setPage(e.getURL());
    	      } catch(IOException ioe) {
    	        // Some warning to user
    	      }
      }
    }
}



