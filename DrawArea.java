import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import javax.swing.*;

import java.awt.FontMetrics;

// Written by: Michael Zimmer - mike@zimmerdesignservices.com

/*
Copyright 2007 Zimmer Design Services

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


public class DrawArea extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener, ActionListener,Printable {
	private static final int PRIORITY_MAX = 10000;

	public static class LintIssue {
		public String severity;
		public String message;
		public String targetName;
		public int targetType;

		public LintIssue(String severity, String message, GeneralObj target) {
			this.severity = severity;
			this.message = message;
			this.targetName = target == null ? null : target.getName();
			this.targetType = target == null ? -1 : target.getType();
		}

		public String toString() {
			String prefix = "[" + severity + "] ";
			if(targetName != null)
				return prefix + targetName + ": " + message;
			return prefix + message;
		}
	}

	//holds all objects to be currently drawn
	private Vector<Object> objList;
	//holds previous lists of objects
	private Vector<Vector<Object>> undoList;
	private Vector<GeneralObj> diagramClipboard = new Vector<GeneralObj>();
	private int pasteCount = 0;
	
	//temp lists
	private Vector<Object> tempList;
	
	
	//keeps track of current position in undo array
	private int currUndoIndex;
	
	//triggered when undo needs to be committed
	private boolean toCommit = false;
	
	// hold right click position
	private int rXTemp, rYTemp;
	
	//hold clicked state
	private GeneralObj tempObj;
	private GeneralObj tempOld;
	private GeneralObj tempClone;
	
	//for multiple object select
	private boolean multipleSelect = false;
	private boolean objsSelected = false;
	private int mXTemp = 0;
	private int mYTemp = 0;
	private int mX0,mY0,mX1,mY1;
	private LinkedList<Integer> selectedIndices = new LinkedList<Integer>();
	private LinkedList<Integer> boxBaseSelectedIndices = new LinkedList<Integer>();
	private static final int SELECT_REPLACE = 0;
	private static final int SELECT_ADD = 1;
	private static final int SELECT_TOGGLE = 2;
	private int selectionBoxMode = SELECT_REPLACE;
	private boolean ctrlDown = false;
	private static final int PAN_DRAG_THRESHOLD = 4;
	private boolean rightButtonDown = false;
	private boolean rightButtonDragged = false;
	private boolean blankCanvasPan = false;
	private Point panScreenStart = null;
	private Point panViewStart = null;
	private MouseEvent pendingPopupEvent = null;
	private GeneralObj pendingPopupObj = null;
	private int pendingPopupType = 0;
	private static final int CLICK_CYCLE_DISTANCE = 8;
	private static final long CLICK_CYCLE_TIME_MS = 700;
	private int lastCycleX = -1000000;
	private int lastCycleY = -1000000;
	private long lastCycleTime = 0;
	private int lastCycleIndex = -1;
	private LinkedList<GeneralObj> lastCycleObjects = new LinkedList<GeneralObj>();
	private static final long QUICK_CLICK_CYCLE_MS = 250;
	private boolean pendingClickCycle = false;
	private LinkedList<HitCandidate> pendingClickCycleHits = null;
	private int pressModelX = 0;
	private int pressModelY = 0;
	private long pressTime = 0;

	//font
	private Font currFont = FizzimFonts.canvasFont();
	private Font tableFont = FizzimFonts.canvasFont();
	
	//global color chooser
	private JColorChooser colorChooser = new JColorChooser();
	
	private boolean loading = false;
	private LinkedList<LintIssue> lastLintIssues = new LinkedList<LintIssue>();
	private GeneralObj hoverObj = null;
	
	private boolean Redraw = false;

	//default settings for global table
	private boolean tableVis = true;
	private Color tableColor = Color.black;
	private Color defSC = Color.black;
	private Color defSTC = Color.black;
	private Color defLTC = Color.black;
	
	//state size
	private int StateW = 130;
	private int StateH = 130;

        // line widths
        private int LineWidth = 1;
	
	//list of global lists
	private LinkedList<LinkedList<ObjAttribute>> globalList;
	
	//parent frame
	private JFrame frame;
	
	//keeps track if file is modified since last opening/saving
	private boolean fileModified = false;
	
	// used for auto generation of state and transition names
	private int createSCounter = 0, createTCounter = 0;
	
	// pages
	private int currPage = 1;
	
	//double click settings
	private int dClickTime = 200;    // double click speed in ms
	private long lastClick = 0;
	
	//lock to grid settings
	private boolean grid = false;
	private int gridS = 25;
	
	// global table, default tab settings
	private int space = 20;
	private static final double MIN_ZOOM = 0.25;
	private static final double MAX_ZOOM = 4.0;
	private double zoom = 1.0;
	private int logicalWidth = 936;
	private int logicalHeight = 1296;
	private int originX = 0;
	private int originY = 0;
	private static final int DIAGRAM_PADDING = 140;
	private static final int FIT_PADDING = 40;
	private static final int PAN_MARGIN_PIXELS = 800;
	private static final int SMART_ALIGN_THRESHOLD = 3;
	private static final int SMART_SPACING_ROW_THRESHOLD = 16;
	private Integer smartGuideX = null;
	private Integer smartGuideY = null;
	private Integer smartSpacingGuideX = null;
	private Integer smartSpacingGuideY = null;

	private class HitCandidate {
		int index;
		int type;
		GeneralObj obj;

		HitCandidate(int index, GeneralObj obj) {
			this.index = index;
			this.obj = obj;
			this.type = obj.getType();
		}
	}
	

	
	
	
	public DrawArea(LinkedList<LinkedList<ObjAttribute>> globals)
	{
		globalList = globals;

		//create arrays to store created objects
		objList = new Vector<Object>();
		undoList = new Vector<Vector<Object>>();
		tempList = new Vector<Object>();
		this.setFocusable(true); 
		this.requestFocus();
		currUndoIndex = -1;
		
		//global attributes stored at index 0 of object array 
		objList.add(globalList);
		

		TextObj globalTable = new TextObj(10,10,globalList,tableFont);
		
		objList.add(globalTable);
		undoList.add(objList);
		currUndoIndex++;

		setBackground(Color.blue);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK), "selectAllDiagramObjects");
		getActionMap().put("selectAllDiagramObjects", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				selectAllDiagramObjects();
			}
		});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), "copyDiagramSelection");
		getActionMap().put("copyDiagramSelection", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				copyDiagramSelection();
			}
		});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "pasteDiagramSelection");
		getActionMap().put("pasteDiagramSelection", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				pasteDiagramSelection();
			}
		});
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "fitDiagramToViewport");
		getActionMap().put("fitDiagramToViewport", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if(frame instanceof FizzimGui)
					((FizzimGui)frame).fitDiagramShortcut();
			}
		});
		installNudgeKeyBindings();

	}

	private void installNudgeKeyBindings()
	{
		bindNudge("nudgeLeft", KeyEvent.VK_LEFT, 0, -1, 0);
		bindNudge("nudgeRight", KeyEvent.VK_RIGHT, 0, 1, 0);
		bindNudge("nudgeUp", KeyEvent.VK_UP, 0, 0, -1);
		bindNudge("nudgeDown", KeyEvent.VK_DOWN, 0, 0, 1);
		bindNudge("nudgeLeftFast", KeyEvent.VK_LEFT, InputEvent.SHIFT_DOWN_MASK, -10, 0);
		bindNudge("nudgeRightFast", KeyEvent.VK_RIGHT, InputEvent.SHIFT_DOWN_MASK, 10, 0);
		bindNudge("nudgeUpFast", KeyEvent.VK_UP, InputEvent.SHIFT_DOWN_MASK, 0, -10);
		bindNudge("nudgeDownFast", KeyEvent.VK_DOWN, InputEvent.SHIFT_DOWN_MASK, 0, 10);
	}

	private void bindNudge(String actionName, int keyCode, int modifiers, final int dx, final int dy)
	{
		getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, modifiers), actionName);
		getActionMap().put(actionName, new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				nudgeSelectedEndpoints(dx, dy);
			}
		});
	}
	    
	public void paintComponent(Graphics g)
	{
		Graphics2D original = (Graphics2D) g;
		super.paintComponent(original);
		Graphics2D g2D = (Graphics2D) original.create();
		g2D.scale(zoom, zoom);
		g2D.translate(-originX, -originY);
		paintDiagram(g2D);
		g2D.dispose();
	}

	public void paintUnscaled(Graphics g)
	{
		Graphics2D g2D = (Graphics2D) g;
		g2D.setColor(getBackground());
		g2D.fillRect(0, 0, logicalWidth, logicalHeight);
		g2D.translate(-originX, -originY);
		paintDiagram(g2D);
	}

	private void paintDiagram(Graphics2D g2D)
	{
		g2D.setFont(currFont);
		g2D.setStroke(new BasicStroke(getLineWidth()));
		g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		
		//paint all objects
		if(objList != null)
		{
			refreshTransitionPriorityHighlights();
			updateStateGroupDefaultEntryMarkers();
			updateStateGroupHighlights();
			for (int i = 1; i < objList.size(); i++)
			{
				GeneralObj s = (GeneralObj) objList.elementAt(i);
				if(s.getType() == 5)
					s.paintComponent(g2D,currPage);
			}
			for (int i = 1; i < objList.size(); i++)
			{
				GeneralObj s = (GeneralObj) objList.elementAt(i);
				if(s.getType() != 5)
					s.paintComponent(g2D,currPage);
			}
		}
		if(multipleSelect)
		{
			g2D.setColor(Color.RED);
			g2D.drawRect(mX0, mY0, mX1-mX0, mY1-mY0);
		}
		paintSmartGuides(g2D);
		if(loading)
		{
			updateGlobalTable();
			loading = false;
			repaint();
		}
	}

	private void paintSmartGuides(Graphics2D g2D)
	{
		if(smartGuideX == null && smartGuideY == null && smartSpacingGuideX == null && smartSpacingGuideY == null)
			return;

		Stroke oldStroke = g2D.getStroke();
		Color oldColor = g2D.getColor();
		float[] dash = {6.0f, 6.0f};
		g2D.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
		g2D.setColor(new Color(70, 130, 180));
		if(smartGuideX != null)
			g2D.drawLine(smartGuideX.intValue(), originY, smartGuideX.intValue(), originY + logicalHeight);
		if(smartGuideY != null)
			g2D.drawLine(originX, smartGuideY.intValue(), originX + logicalWidth, smartGuideY.intValue());
		g2D.setColor(new Color(120, 90, 180));
		if(smartSpacingGuideX != null)
			g2D.drawLine(smartSpacingGuideX.intValue(), originY, smartSpacingGuideX.intValue(), originY + logicalHeight);
		if(smartSpacingGuideY != null)
			g2D.drawLine(originX, smartSpacingGuideY.intValue(), originX + logicalWidth, smartSpacingGuideY.intValue());
		g2D.setStroke(oldStroke);
		g2D.setColor(oldColor);
	}

	public double getZoom()
	{
		return zoom;
	}

	public void setZoom(double newZoom)
	{
		zoom = clampZoom(newZoom);
		updateZoomedSize();
		repaint();
		notifyZoomChanged();
	}

	public void setLogicalSize(int width, int height)
	{
		updateCanvasExtents();
	}

	public int getLogicalWidth()
	{
		return logicalWidth;
	}

	public int getLogicalHeight()
	{
		return logicalHeight;
	}

	private double clampZoom(double value)
	{
		return Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, value));
	}

	private void updateZoomedSize()
	{
		Dimension size = new Dimension((int)Math.ceil(logicalWidth * zoom), (int)Math.ceil(logicalHeight * zoom));
		setPreferredSize(size);
		setMinimumSize(size);
		setMaximumSize(size);
		setSize(size);
		revalidate();
	}

	public void updateCanvasExtents()
	{
		updateCanvasExtents(DIAGRAM_PADDING);
	}

	private void updateCanvasExtents(int padding)
	{
		Rectangle bounds = getDiagramBounds(currPage);
		originX = bounds.x - padding;
		originY = bounds.y - padding;
		logicalWidth = Math.max(1, bounds.width + padding * 2);
		logicalHeight = Math.max(1, bounds.height + padding * 2);
		updateZoomedSize();
	}

	private void expandCanvasExtentsForDrag()
	{
		Rectangle bounds = getDiagramBounds(currPage);
		int right = bounds.x + bounds.width + DIAGRAM_PADDING;
		int bottom = bounds.y + bounds.height + DIAGRAM_PADDING;
		boolean changed = false;
		if(right > originX + logicalWidth)
		{
			logicalWidth = Math.max(logicalWidth, right - originX);
			changed = true;
		}
		if(bottom > originY + logicalHeight)
		{
			logicalHeight = Math.max(logicalHeight, bottom - originY);
			changed = true;
		}
		if(changed)
			updateZoomedSize();
	}

	public Rectangle getDiagramBounds(int page)
	{
		Rectangle bounds = null;
		if(objList != null)
		{
			for(int i = 1; i < objList.size(); i++)
			{
				GeneralObj obj = (GeneralObj)objList.elementAt(i);
				Rectangle objBounds = getObjectBounds(obj, page);
				if(objBounds == null)
					continue;
				if(bounds == null)
					bounds = new Rectangle(objBounds);
				else
					bounds.add(objBounds);
			}
		}
		if(bounds == null)
			bounds = new Rectangle(0, 0, 1, 1);
		return bounds;
	}

	private Rectangle getObjectBounds(GeneralObj obj, int page)
	{
		if(obj.getPage() != page && obj.getType() != 1)
			return null;
		Rectangle bounds = null;
		if(obj instanceof StateObj)
		{
			int[] coords = ((StateObj)obj).getCoords();
			bounds = normalizedBounds(coords[0], coords[1], coords[2], coords[3], 20);
		}
		else if(obj instanceof TextObj)
		{
			TextObj textObj = (TextObj)obj;
			if(textObj.getGlobalTable())
				textObj.prepareGlobalBounds(getFontMetrics(tableFont), globalList, tableFont, tableVis, space, tableColor);
			bounds = textObj.getBounds();
		}
		else if(obj instanceof StateTransitionObj)
			bounds = getStateTransitionBounds((StateTransitionObj)obj, page);
		else if(obj instanceof LoopbackTransitionObj)
			bounds = getLoopbackTransitionBounds((LoopbackTransitionObj)obj, page);

		return addAttributeBounds(bounds, obj, page);
	}

	private Rectangle addAttributeBounds(Rectangle bounds, GeneralObj obj, int page)
	{
		LinkedList<ObjAttribute> attrs = obj.getAttributeList();
		if(attrs == null)
			return bounds;

		FontMetrics metrics = getFontMetrics(currFont);
		if((obj.getType() == 0 || obj.getType() == 5) && obj instanceof StateObj)
			return addStateLikeAttributeBounds(bounds, (StateObj)obj, attrs, metrics, page);

		Point center = obj.getCenter(page);
		int step = -1;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			if(attr.isCanvasVisible())
				step++;
			Rectangle attrBounds = attr.getDrawBounds(metrics, center, page, step);
			if(attrBounds == null)
				continue;
			if(bounds == null)
				bounds = new Rectangle(attrBounds);
			else
				bounds.add(attrBounds);
		}
		return bounds;
	}

	private Rectangle addStateLikeAttributeBounds(Rectangle bounds, StateObj state, LinkedList<ObjAttribute> attrs, FontMetrics metrics, int page)
	{
		int[] coords = state.getCoords();
		Rectangle shape = normalizedBounds(coords[0], coords[1], coords[2], coords[3], 0);
		ObjAttribute nameAttr = null;
		for(int i = 0; i < attrs.size(); i++)
		{
			if(attrs.get(i).getName().equals("name"))
			{
				nameAttr = attrs.get(i);
				break;
			}
		}
		if(nameAttr != null)
			bounds = addRectangleToBounds(bounds, nameAttr.getStateDrawBounds(metrics, currFont, shape, page, 0));

		int row = 1;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			if(attr == nameAttr || !attr.isCanvasVisible())
				continue;
			bounds = addRectangleToBounds(bounds, attr.getStateDrawBounds(metrics, currFont, shape, page, row));
			row++;
		}
		return bounds;
	}

	private Rectangle addRectangleToBounds(Rectangle bounds, Rectangle addition)
	{
		if(addition == null)
			return bounds;
		if(bounds == null)
			return new Rectangle(addition);
		bounds.add(addition);
		return bounds;
	}

	private Rectangle getStateTransitionBounds(StateTransitionObj trans, int page)
	{
		Rectangle bounds = null;
		if(trans.getSPage() == page)
		{
			bounds = addPointToBounds(bounds, trans.startPt);
			bounds = addPointToBounds(bounds, trans.startCtrlPt);
			if(trans.getSPage() == trans.getEPage())
			{
				bounds = addPointToBounds(bounds, trans.endCtrlPt);
				bounds = addPointToBounds(bounds, trans.endPt);
			}
			else
			{
				bounds = addPointToBounds(bounds, trans.pageSC);
				bounds = addPointToBounds(bounds, trans.pageS);
			}
		}
		if(trans.getEPage() == page && trans.getSPage() != trans.getEPage())
		{
			bounds = addPointToBounds(bounds, trans.pageE);
			bounds = addPointToBounds(bounds, trans.pageEC);
			bounds = addPointToBounds(bounds, trans.endCtrlPt);
			bounds = addPointToBounds(bounds, trans.endPt);
		}
		if(bounds != null)
			bounds.grow(24, 24);
		return bounds;
	}

	private Rectangle getLoopbackTransitionBounds(LoopbackTransitionObj trans, int page)
	{
		if(trans.getPage() != page)
			return null;
		Rectangle bounds = null;
		bounds = addPointToBounds(bounds, trans.startPt);
		bounds = addPointToBounds(bounds, trans.startCtrlPt);
		bounds = addPointToBounds(bounds, trans.endCtrlPt);
		bounds = addPointToBounds(bounds, trans.endPt);
		if(bounds != null)
			bounds.grow(24, 24);
		return bounds;
	}

	private Rectangle normalizedBounds(int x0, int y0, int x1, int y1, int grow)
	{
		int x = Math.min(x0, x1);
		int y = Math.min(y0, y1);
		int w = Math.abs(x1 - x0);
		int h = Math.abs(y1 - y0);
		Rectangle bounds = new Rectangle(x, y, w, h);
		bounds.grow(grow, grow);
		return bounds;
	}

	private Rectangle addPointToBounds(Rectangle bounds, Point point)
	{
		if(point == null)
			return bounds;
		if(bounds == null)
			return new Rectangle(point.x, point.y, 1, 1);
		bounds.add(point);
		return bounds;
	}

	public void fitDiagramToViewport(Dimension viewport)
	{
		if(viewport == null || viewport.width <= 0 || viewport.height <= 0)
			return;
		updateCanvasExtents(FIT_PADDING);
		double xZoom = viewport.getWidth() / logicalWidth;
		double yZoom = viewport.getHeight() / logicalHeight;
		setZoom(Math.min(1.0, Math.min(xZoom, yZoom)));
		JViewport scrollViewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if(scrollViewport != null)
			scrollViewport.setViewPosition(new Point(0, 0));
	}

	private int modelX(MouseEvent e)
	{
		return (int)Math.round(e.getX() / zoom) + originX;
	}

	private int modelY(MouseEvent e)
	{
		return (int)Math.round(e.getY() / zoom) + originY;
	}

	private void notifyZoomChanged()
	{
		if(frame instanceof FizzimGui)
			((FizzimGui)frame).updateZoomControls();
	}

	private void notifyViewManuallyChanged()
	{
		if(frame instanceof FizzimGui)
			((FizzimGui)frame).viewManuallyChanged();
	}

	private boolean isPopupMouse(MouseEvent e)
	{
		return e.getButton() == MouseEvent.BUTTON3 || e.getModifiers() == 20 || e.isPopupTrigger();
	}

	private boolean isRightButtonDrag(MouseEvent e)
	{
		return (e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0;
	}

	private void startPanCandidate(MouseEvent e)
	{
		rightButtonDown = true;
		rightButtonDragged = false;
		panScreenStart = new Point(e.getXOnScreen(), e.getYOnScreen());
		JViewport viewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, this);
		panViewStart = viewport == null ? null : viewport.getViewPosition();
		pendingPopupEvent = null;
		pendingPopupObj = null;
		pendingPopupType = 0;
	}

	private void queuePopup(GeneralObj obj, MouseEvent e, int type)
	{
		pendingPopupObj = obj;
		pendingPopupEvent = e;
		pendingPopupType = type;
	}

	private void clearPendingPopup()
	{
		pendingPopupEvent = null;
		pendingPopupObj = null;
		pendingPopupType = 0;
	}

	private boolean panCanvas(MouseEvent e)
	{
		if(!rightButtonDown || !isRightButtonDrag(e) || panScreenStart == null || panViewStart == null)
			return false;

		int dx = e.getXOnScreen() - panScreenStart.x;
		int dy = e.getYOnScreen() - panScreenStart.y;
		if(!rightButtonDragged && Math.abs(dx) < PAN_DRAG_THRESHOLD && Math.abs(dy) < PAN_DRAG_THRESHOLD)
			return true;

		rightButtonDragged = true;
		notifyViewManuallyChanged();
		clearPendingPopup();
		JViewport viewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if(viewport == null)
			return true;

		ensurePanCanvasMargin(viewport);
		int newX = panViewStart.x - dx;
		int newY = panViewStart.y - dy;
		newX = Math.max(0, Math.min(newX, Math.max(0, getWidth() - viewport.getWidth())));
		newY = Math.max(0, Math.min(newY, Math.max(0, getHeight() - viewport.getHeight())));
		viewport.setViewPosition(new Point(newX, newY));
		return true;
	}

	private void startBlankCanvasPan(MouseEvent e)
	{
	}

	private boolean panBlankCanvas(MouseEvent e)
	{
		return false;
	}

	private void panViewportByWheel(MouseWheelEvent e)
	{
		JViewport viewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, this);
		if(viewport == null)
			return;
		e.consume();
		notifyViewManuallyChanged();
		ensurePanCanvasMargin(viewport);
		Point viewPosition = viewport.getViewPosition();
		int delta = (int)Math.round(e.getPreciseWheelRotation() * 80.0);
		if(delta == 0)
			delta = e.getWheelRotation() == 0 ? 0 : e.getWheelRotation() * 40;
		int dx = e.isShiftDown() ? delta : 0;
		int dy = e.isShiftDown() ? 0 : delta;
		setViewportPosition(viewport, viewPosition.x + dx, viewPosition.y + dy);
	}

	private void setViewportPosition(JViewport viewport, int x, int y)
	{
		int newX = Math.max(0, Math.min(x, Math.max(0, getWidth() - viewport.getWidth())));
		int newY = Math.max(0, Math.min(y, Math.max(0, getHeight() - viewport.getHeight())));
		viewport.setViewPosition(new Point(newX, newY));
	}

	private void ensurePanCanvasMargin(JViewport viewport)
	{
		int margin = PAN_MARGIN_PIXELS;
		Point view = viewport.getViewPosition();
		int shiftX = 0;
		int shiftY = 0;
		int growLeft = Math.max(0, margin - view.x);
		int growTop = Math.max(0, margin - view.y);
		int growRight = Math.max(0, view.x + viewport.getWidth() + margin - getWidth());
		int growBottom = Math.max(0, view.y + viewport.getHeight() + margin - getHeight());

		if(growLeft > 0)
		{
			int modelGrow = (int)Math.ceil(growLeft / zoom);
			originX -= modelGrow;
			logicalWidth += modelGrow;
			shiftX = (int)Math.round(modelGrow * zoom);
		}
		if(growTop > 0)
		{
			int modelGrow = (int)Math.ceil(growTop / zoom);
			originY -= modelGrow;
			logicalHeight += modelGrow;
			shiftY = (int)Math.round(modelGrow * zoom);
		}
		if(growRight > 0)
			logicalWidth += (int)Math.ceil(growRight / zoom);
		if(growBottom > 0)
			logicalHeight += (int)Math.ceil(growBottom / zoom);

		if(growLeft > 0 || growTop > 0 || growRight > 0 || growBottom > 0)
		{
			updateZoomedSize();
			if(shiftX != 0 || shiftY != 0)
			{
				Point shiftedView = new Point(view.x + shiftX, view.y + shiftY);
				viewport.setViewPosition(shiftedView);
				panViewStart = new Point(panViewStart.x + shiftX, panViewStart.y + shiftY);
			}
		}
	}

	private void selectAllDiagramObjects()
	{
		selectedIndices.clear();
		int count = 0;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(isTransitionEndpoint(obj) || isMovableTextObject(obj))
			{
				obj.setSelectStatus(true);
				selectedIndices.add(new Integer(i));
				count++;
			}
			else
			{
				obj.unselect();
			}
		}
		normalizeStateGroupSelection();
		count = selectedIndices.size();
		objsSelected = count > 1;
		multipleSelect = false;
		notifySelectionChanged();
		repaint();
	}

	private boolean isMovableTextObject(GeneralObj obj)
	{
		return obj.getType() == 3 && !((TextObj)obj).getGlobalTable();
	}

	private boolean isSelectableDiagramObject(GeneralObj obj)
	{
		return isTransitionEndpoint(obj) || isMovableTextObject(obj);
	}

	private boolean isInspectableDiagramObject(GeneralObj obj)
	{
		return isSelectableDiagramObject(obj) || obj.getType() == 1 || obj.getType() == 2;
	}

	private void copyDiagramSelection()
	{
		refreshSelectedIndicesFromObjects();
		LinkedHashSet<GeneralObj> selectedObjects = new LinkedHashSet<GeneralObj>();
		LinkedHashSet<GeneralObj> endpointObjects = new LinkedHashSet<GeneralObj>();

		for(int i = 0; i < selectedIndices.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(selectedIndices.get(i).intValue());
			if(isSelectableDiagramObject(obj))
			{
				selectedObjects.add(obj);
				if(isTransitionEndpoint(obj))
					endpointObjects.add(obj);
				if(obj.getType() == 5)
				{
					LinkedList<StateObj> children = getContainedTransitionEndpoints((StateGroupObj)obj);
					for(int j = 0; j < children.size(); j++)
					{
						selectedObjects.add(children.get(j));
						endpointObjects.add(children.get(j));
					}
				}
			}
		}

		if(selectedObjects.size() == 0)
			return;

		LinkedHashSet<GeneralObj> copiedObjects = new LinkedHashSet<GeneralObj>(selectedObjects);
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if((obj.getType() == 1 || obj.getType() == 2) && isTransitionFullyCovered((TransitionObj)obj, endpointObjects))
				copiedObjects.add(obj);
		}

		LinkedHashMap<GeneralObj, GeneralObj> copyMap = new LinkedHashMap<GeneralObj, GeneralObj>();
		Vector<GeneralObj> clipboard = new Vector<GeneralObj>();
		for(GeneralObj obj : copiedObjects)
		{
			GeneralObj cloned = cloneObj(obj);
			copyMap.put(obj, cloned);
			clipboard.add(cloned);
		}
		for(GeneralObj cloned : clipboard)
		{
			if(cloned.getType() == 1 || cloned.getType() == 2)
			{
				for(Map.Entry<GeneralObj, GeneralObj> entry : copyMap.entrySet())
					cloned.notifyChange(entry.getKey(), entry.getValue());
			}
		}
		diagramClipboard = clipboard;
		pasteCount = 0;
	}

	private boolean isTransitionFullyCovered(TransitionObj trans, LinkedHashSet<GeneralObj> endpoints)
	{
		StateObj start = trans.getStartState();
		StateObj end = trans.getEndState();
		if(start == null)
			return false;
		if(end == null)
			return endpoints.contains(start);
		return endpoints.contains(start) && endpoints.contains(end);
	}

	private void pasteDiagramSelection()
	{
		if(diagramClipboard == null || diagramClipboard.size() == 0)
			return;

		setUndoPointAllObjects();
		unselectObjs();

		int offset = 40 + (pasteCount % 4) * 20;
		pasteCount++;
		HashSet<String> usedNames = collectObjectNames();
		LinkedHashMap<GeneralObj, GeneralObj> pasteMap = new LinkedHashMap<GeneralObj, GeneralObj>();
		LinkedHashMap<String, String> nameMap = new LinkedHashMap<String, String>();
		Vector<GeneralObj> pastedObjects = new Vector<GeneralObj>();

		for(int i = 0; i < diagramClipboard.size(); i++)
		{
			GeneralObj source = diagramClipboard.get(i);
			GeneralObj pasted = cloneObj(source);
			pasted.setPage(currPage);
			String oldName = source.getName();
			if(oldName != null && oldName.length() > 0)
			{
				String newName = uniqueCopyName(oldName, usedNames);
				renameObjectForPaste(pasted, newName);
				nameMap.put(oldName, newName);
				usedNames.add(newName);
			}
			if(isTransitionEndpoint(pasted))
				((StateObj)pasted).moveBy(offset, offset);
			else if(isMovableTextObject(pasted))
				((TextObj)pasted).moveBy(offset, offset);
			pasteMap.put(source, pasted);
			pastedObjects.add(pasted);
		}

		for(GeneralObj pasted : pastedObjects)
		{
			if(pasted.getType() == 5)
				remapStateGroupNames((StateGroupObj)pasted, nameMap);
			if(pasted.getType() == 1 || pasted.getType() == 2)
			{
				for(Map.Entry<GeneralObj, GeneralObj> entry : pasteMap.entrySet())
					pasted.notifyChange(entry.getKey(), entry.getValue());
				pasted.setParentModified(true);
				pasted.updateObj();
			}
			objList.add(pasted);
		}

		for(int i = objList.size() - pastedObjects.size(); i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(isSelectableDiagramObject(obj))
			{
				obj.setSelectStatus(true);
				selectedIndices.add(new Integer(i));
			}
		}
		validateStateGroupMembership(true);
		updateStates();
		updateTrans();
		updateCanvasExtents();
		syncSelectionState();
		commitUndo();
		repaint();
	}

	private HashSet<String> collectObjectNames()
	{
		HashSet<String> names = new HashSet<String>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getName() != null && obj.getName().length() > 0)
				names.add(obj.getName());
		}
		return names;
	}

	private String uniqueCopyName(String baseName, HashSet<String> usedNames)
	{
		String candidate = baseName + "_copy";
		int suffix = 2;
		while(usedNames.contains(candidate))
		{
			candidate = baseName + "_copy" + suffix;
			suffix++;
		}
		return candidate;
	}

	private void renameObjectForPaste(GeneralObj obj, String newName)
	{
		obj.setName(newName);
		LinkedList<ObjAttribute> attrs = obj.getAttributeList();
		if(attrs == null)
			return;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			if(attr.getName().equals("name"))
			{
				attr.setValue(newName);
				attr.setEditable(1, ObjAttribute.LOCAL);
			}
		}
	}

	private void remapStateGroupNames(StateGroupObj group, LinkedHashMap<String, String> nameMap)
	{
		LinkedList<String> remapped = new LinkedList<String>();
		LinkedList<String> children = group.getChildNames();
		for(int i = 0; i < children.size(); i++)
		{
			String child = children.get(i);
			remapped.add(nameMap.containsKey(child) ? nameMap.get(child) : child);
		}
		group.setChildNames(remapped);
		String entry = group.getEntryState();
		if(entry != null && nameMap.containsKey(entry))
			group.setEntryState(nameMap.get(entry));
	}

	private void addSelectedIndex(int index)
	{
		if(!selectedIndices.contains(new Integer(index)))
			selectedIndices.add(new Integer(index));
	}

	private void removeSelectedIndex(int index)
	{
		selectedIndices.remove(new Integer(index));
	}

	private void syncSelectionState()
	{
		normalizeStateGroupSelection();
		objsSelected = selectedIndices.size() > 1;
		notifySelectionChanged();
	}

	private void notifySelectionChanged()
	{
		if(!(frame instanceof FizzimGui) || objList == null)
			return;

		int states = 0;
		int groups = 0;
		int forks = 0;
		int transitions = 0;
		int text = 0;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getSelectStatus() == 0)
				continue;
			if(obj.getType() == 0)
				states++;
			else if(obj.getType() == 5)
				groups++;
			else if(obj.getType() == 4)
				forks++;
			else if(obj.getType() == 1 || obj.getType() == 2)
				transitions++;
			else if(obj.getType() == 3 && !((TextObj)obj).getGlobalTable())
				text++;
		}
		((FizzimGui)frame).updateSelectionStatus(formatSelectionStatus(states, groups, forks, transitions, text));
		((FizzimGui)frame).updatePropertyInspector(getSelectedObjectsForInspector());
	}

	public LinkedList<GeneralObj> getSelectedObjectsForInspector()
	{
		LinkedList<GeneralObj> selected = new LinkedList<GeneralObj>();
		if(objList == null)
			return selected;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getSelectStatus() != 0 && isInspectableDiagramObject(obj))
				selected.add(obj);
		}
		return selected;
	}

	public GeneralObj prepareInspectorEdit(GeneralObj current)
	{
		if(current == null)
			return null;
		String name = current.getName();
		int type = current.getType();
		setUndoPointAllObjects();
		GeneralObj replacement = findObjectByNameAndType(name, type);
		if(replacement != null)
			replacement.setSelectStatus(true);
		return replacement;
	}

	public LinkedList<GeneralObj> prepareInspectorBatchEdit(LinkedList<GeneralObj> currentObjects)
	{
		LinkedList<GeneralObj> replacements = new LinkedList<GeneralObj>();
		if(currentObjects == null || currentObjects.size() == 0)
			return replacements;
		setUndoPointAllObjects();
		for(int i = 0; i < currentObjects.size(); i++)
		{
			GeneralObj current = currentObjects.get(i);
			GeneralObj replacement = findObjectByNameAndType(current.getName(), current.getType());
			if(replacement != null)
			{
				replacement.setSelectStatus(true);
				replacements.add(replacement);
			}
		}
		return replacements;
	}

	public void finishInspectorEdit()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(!isTransitionEndpoint(obj))
			{
				obj.setParentModified(true);
				obj.updateObj();
				obj.setParentModified(false);
			}
		}
		updateStates();
		updateTrans();
		validateStateGroupMembership(true);
		updateCanvasExtents();
		commitUndo();
		syncSelectionState();
	}

	private GeneralObj findObjectByNameAndType(String name, int type)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == type && obj.getName().equals(name))
				return obj;
		}
		return null;
	}

	private String formatSelectionStatus(int states, int groups, int forks, int transitions, int text)
	{
		LinkedList<String> parts = new LinkedList<String>();
		addSelectionPart(parts, states, "state", "states");
		addSelectionPart(parts, groups, "state group", "state groups");
		addSelectionPart(parts, forks, "fork", "forks");
		addSelectionPart(parts, transitions, "transition", "transitions");
		addSelectionPart(parts, text, "text", "text");
		if(parts.size() == 0)
			return "No selection";
		StringBuffer sb = new StringBuffer("Selected: ");
		for(int i = 0; i < parts.size(); i++)
		{
			if(i > 0)
				sb.append(", ");
			sb.append(parts.get(i));
		}
		return sb.toString();
	}

	private void addSelectionPart(LinkedList<String> parts, int count, String singular, String plural)
	{
		if(count == 0)
			return;
		parts.add(count + " " + (count == 1 ? singular : plural));
	}

	private void refreshSelectedIndicesFromObjects()
	{
		selectedIndices.clear();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(isSelectableDiagramObject(obj) && obj.getSelectStatus() != 0)
				selectedIndices.add(new Integer(i));
		}
	}

	private int selectionModeForEvent(MouseEvent e)
	{
		if(e.isControlDown())
			return SELECT_TOGGLE;
		if(e.isShiftDown())
			return SELECT_ADD;
		return SELECT_REPLACE;
	}

	private int findSelectableIndexAt(int x, int y)
	{
		int[] typeOrder = {4, 0, 5, 3};
		for(int t = 0; t < typeOrder.length; t++)
		{
			for(int i = objList.size() - 1; i >= 1; i--)
			{
				GeneralObj obj = (GeneralObj)objList.get(i);
				if(obj.getType() == 3 && ((TextObj)obj).getGlobalTable())
					continue;
				if(obj.getType() == typeOrder[t] && isSelectableDiagramObject(obj) && hitTestObject(obj, x, y))
					return i;
			}
		}
		return -1;
	}

	private void selectIndex(int index, int x, int y)
	{
		GeneralObj obj = (GeneralObj)objList.get(index);
		obj.setSelectStatus(x, y);
		addSelectedIndex(index);
	}

	private void applyModifiedClickSelection(int index, int mode, int x, int y)
	{
		if(index < 0)
			return;

		refreshSelectedIndicesFromObjects();
		GeneralObj obj = (GeneralObj)objList.get(index);
		boolean wasSelected = selectedIndices.contains(new Integer(index)) || obj.getSelectStatus() != 0;
		if(mode == SELECT_TOGGLE && wasSelected)
		{
			obj.unselect();
			removeSelectedIndex(index);
		}
		else
		{
			selectIndex(index, x, y);
		}
		syncSelectionState();
		repaint();
	}

	private void normalizeStateGroupSelection()
	{
		LinkedList<StateGroupObj> selectedGroups = getSelectedStateGroups();
		if(selectedGroups.size() == 0)
			return;
		for(int i = selectedIndices.size() - 1; i >= 0; i--)
		{
			int index = selectedIndices.get(i).intValue();
			GeneralObj obj = (GeneralObj)objList.get(index);
			if((obj.getType() == 0 || obj.getType() == 4)
					&& isContainedInSelectedStateGroup((StateObj)obj, selectedGroups))
			{
				obj.unselect();
				selectedIndices.remove(i);
			}
		}
	}

	private void applyBoxSelection()
	{
		LinkedList<Integer> boxHits = new LinkedList<Integer>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(isSelectableDiagramObject(obj) && obj.setBoxSelectStatus(mX0, mY0, mX1, mY1))
				boxHits.add(new Integer(i));
		}

		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(isSelectableDiagramObject(obj))
				obj.unselect();
		}

		selectedIndices.clear();
		if(selectionBoxMode == SELECT_REPLACE)
		{
			selectedIndices.addAll(boxHits);
		}
		else
		{
			selectedIndices.addAll(boxBaseSelectedIndices);
			for(int i = 0; i < boxHits.size(); i++)
			{
				Integer index = boxHits.get(i);
				if(selectionBoxMode == SELECT_TOGGLE && selectedIndices.contains(index))
					selectedIndices.remove(index);
				else if(!selectedIndices.contains(index))
					selectedIndices.add(index);
			}
		}

		for(int i = 0; i < selectedIndices.size(); i++)
			((GeneralObj)objList.get(selectedIndices.get(i).intValue())).setSelectStatus(true);
		syncSelectionState();
	}

	private void addHitCandidate(LinkedList<HitCandidate> hits, int index, GeneralObj obj)
	{
		for(int i = 0; i < hits.size(); i++)
		{
			if(hits.get(i).obj == obj)
				return;
		}
		hits.add(new HitCandidate(index, obj));
	}

	private LinkedList<HitCandidate> getHitCandidates(int x, int y)
	{
		LinkedList<HitCandidate> hits = new LinkedList<HitCandidate>();

		addEndpointHits(hits, x, y, 4);
		addEndpointHits(hits, x, y, 0);
		addEndpointHits(hits, x, y, 5);
		addObjectHits(hits, x, y, 3);
		addObjectHits(hits, x, y, 1);
		addObjectHits(hits, x, y, 2);
		return hits;
	}

	private boolean hitTestObject(GeneralObj obj, int x, int y)
	{
		try {
			GeneralObj clone = (GeneralObj)obj.clone();
			return clone.setSelectStatus(x, y);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return false;
		}
	}

	private void addEndpointHits(LinkedList<HitCandidate> hits, int x, int y, int type)
	{
		for(int i = objList.size() - 1; i >= 1; i--)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(obj.getType() == type && isTransitionEndpoint(obj) && hitTestObject(obj, x, y))
				addHitCandidate(hits, i, obj);
		}
	}

	private void addObjectHits(LinkedList<HitCandidate> hits, int x, int y, int type)
	{
		for(int i = objList.size() - 1; i >= 1; i--)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(obj.getType() == 3 && ((TextObj)obj).getGlobalTable())
				continue;
			if(obj.getType() == type && hitTestObject(obj, x, y))
				addHitCandidate(hits, i, obj);
		}
	}

	private GeneralObj findHoverObject(int x, int y)
	{
		LinkedList<HitCandidate> hits = getHitCandidates(x, y);
		if(hits.size() > 0)
			return hits.get(0).obj;
		return null;
	}

	private void updateHoverObject(GeneralObj newHover)
	{
		if(hoverObj == newHover)
			return;
		if(hoverObj != null)
			hoverObj.setHoverHighlighted(false);
		hoverObj = newHover;
		if(hoverObj != null)
			hoverObj.setHoverHighlighted(true);
		repaint();
	}

	private boolean sameCycleObjects(LinkedList<HitCandidate> hits)
	{
		if(hits.size() != lastCycleObjects.size())
			return false;
		for(int i = 0; i < hits.size(); i++)
		{
			GeneralObj current = hits.get(i).obj;
			GeneralObj previous = lastCycleObjects.get(i);
			if(current.getType() != previous.getType() || !current.getName().equals(previous.getName()))
				return false;
		}
		return true;
	}

	private void rememberCycleObjects(LinkedList<HitCandidate> hits)
	{
		lastCycleObjects.clear();
		for(int i = 0; i < hits.size(); i++)
			lastCycleObjects.add(hits.get(i).obj);
	}

	private GeneralObj selectFromClickCycle(MouseEvent e, int x, int y, LinkedList<HitCandidate> hits)
	{
		if(hits == null || hits.size() < 2)
			return null;

		if(!canContinueClickCycle(e, x, y, hits))
			return null;

		lastCycleIndex = (lastCycleIndex + 1) % hits.size();

		HitCandidate hit = hits.get(lastCycleIndex);
		setUndoPoint(hit.index, hit.type);
		GeneralObj selectedObj = (GeneralObj)objList.get(hit.index);
		unselectObjs();
		selectedObj.setSelectStatus(x, y);
		refreshSelectedIndicesFromObjects();
		syncSelectionState();
		lastCycleX = x;
		lastCycleY = y;
		lastCycleTime = e.getWhen();
		rememberCycleObjects(hits);

		return selectedObj;
	}

	private boolean canContinueClickCycle(MouseEvent e, int x, int y, LinkedList<HitCandidate> hits)
	{
		return hits != null && hits.size() > 1
				&& Math.abs(x - lastCycleX) <= CLICK_CYCLE_DISTANCE
				&& Math.abs(y - lastCycleY) <= CLICK_CYCLE_DISTANCE
				&& e.getWhen() - lastCycleTime <= CLICK_CYCLE_TIME_MS
				&& sameCycleObjects(hits);
	}

	private void rememberClickCycle(MouseEvent e, int x, int y, LinkedList<HitCandidate> hits, GeneralObj selectedObj)
	{
		if(hits == null || hits.size() < 2 || selectedObj == null)
		{
			resetClickCycle();
			return;
		}

		lastCycleIndex = 0;
		for(int i = 0; i < hits.size(); i++)
		{
			if(hits.get(i).obj.getType() == selectedObj.getType()
					&& hits.get(i).obj.getName().equals(selectedObj.getName()))
			{
				lastCycleIndex = i;
				break;
			}
		}
		lastCycleX = x;
		lastCycleY = y;
		lastCycleTime = e.getWhen();
		rememberCycleObjects(hits);
	}

	private void resetClickCycle()
	{
		lastCycleIndex = -1;
		lastCycleObjects.clear();
		pendingClickCycle = false;
		pendingClickCycleHits = null;
	}

	private void editSelectedObject(GeneralObj obj)
	{
		if(obj.getType() == 0 || obj.getType() == 5)
		{
			new StateProperties(this,frame, true, (StateObj) obj).setVisible(true);
		}
		else if(obj.getType() == 1)
		{
			Vector<StateObj> stateObjs = new Vector<StateObj>();
			for(int j = 1; j < objList.size(); j++)
			{
				GeneralObj stateObj = (GeneralObj)objList.get(j);
				if(isTransitionEndpoint(stateObj))
					stateObjs.add((StateObj)stateObj);
			}
			new TransProperties(this,frame, true, (StateTransitionObj) obj,stateObjs,false,null).setVisible(true);
		}
		else if(obj.getType() == 2)
		{
			Vector<StateObj> stateObjs = new Vector<StateObj>();
			for(int j = 1; j < objList.size(); j++)
			{
				GeneralObj stateObj = (GeneralObj)objList.get(j);
				if(stateObj.getType() == 0)
					stateObjs.add((StateObj)stateObj);
			}
			new TransProperties(this,frame, true, (LoopbackTransitionObj) obj,stateObjs,true,null).setVisible(true);
		}
		else if(obj.getType() == 3)
		{
			editText((TextObj) obj);
		}
	}

	private void unselectObjsExcept(int keepIndex)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			if(i != keepIndex)
				((GeneralObj)objList.get(i)).unselect();
		}
		refreshSelectedIndicesFromObjects();
		syncSelectionState();
	}

	private void clearSmartGuides()
	{
		smartGuideX = null;
		smartGuideY = null;
		smartSpacingGuideX = null;
		smartSpacingGuideY = null;
	}

	private void applySmartAlignment(StateObj moving, HashSet<GeneralObj> movedBySelectedGroup, boolean snapDisabled)
	{
		clearSmartGuides();
		if(snapDisabled || moving.getSelectStatus() != StateObj.CENTER)
			return;

		Point center = moving.getRealCenter(currPage);
		int bestDx = 0;
		int bestDy = 0;
		int bestXDistance = SMART_ALIGN_THRESHOLD + 1;
		int bestYDistance = SMART_ALIGN_THRESHOLD + 1;

		LinkedList<StateObj> candidates = getSmartAlignCandidates(moving, movedBySelectedGroup);
		for(int i = 0; i < candidates.size(); i++)
		{
			Point candidateCenter = candidates.get(i).getRealCenter(currPage);
			int dx = candidateCenter.x - center.x;
			int dy = candidateCenter.y - center.y;
			if(Math.abs(dx) < Math.abs(bestXDistance))
			{
				bestDx = dx;
				bestXDistance = dx;
				smartGuideX = new Integer(candidateCenter.x);
			}
			if(Math.abs(dy) < Math.abs(bestYDistance))
			{
				bestDy = dy;
				bestYDistance = dy;
				smartGuideY = new Integer(candidateCenter.y);
			}
		}

		int spacingDx = findSpacingSnapDelta(center, candidates, true);
		if(Math.abs(spacingDx) <= SMART_ALIGN_THRESHOLD
				&& (Math.abs(bestXDistance) > SMART_ALIGN_THRESHOLD || Math.abs(spacingDx) < Math.abs(bestDx)))
		{
			bestDx = spacingDx;
			smartGuideX = null;
			smartSpacingGuideX = new Integer(center.x + spacingDx);
		}

		int spacingDy = findSpacingSnapDelta(center, candidates, false);
		if(Math.abs(spacingDy) <= SMART_ALIGN_THRESHOLD
				&& (Math.abs(bestYDistance) > SMART_ALIGN_THRESHOLD || Math.abs(spacingDy) < Math.abs(bestDy)))
		{
			bestDy = spacingDy;
			smartGuideY = null;
			smartSpacingGuideY = new Integer(center.y + spacingDy);
		}

		if(Math.abs(bestDx) > SMART_ALIGN_THRESHOLD)
		{
			bestDx = 0;
			smartGuideX = null;
			smartSpacingGuideX = null;
		}
		if(Math.abs(bestDy) > SMART_ALIGN_THRESHOLD)
		{
			bestDy = 0;
			smartGuideY = null;
			smartSpacingGuideY = null;
		}
		if(bestDx != 0 || bestDy != 0)
			moving.moveBy(bestDx, bestDy);
	}

	private LinkedList<StateObj> getSmartAlignCandidates(StateObj moving, HashSet<GeneralObj> movedBySelectedGroup)
	{
		LinkedList<StateObj> candidates = new LinkedList<StateObj>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(!isTransitionEndpoint(obj) || obj == moving || movedBySelectedGroup.contains(obj))
				continue;
			if(obj.getPage() != currPage)
				continue;
			if(moving.getType() == 5 && (obj.getType() == 0 || obj.getType() == 4)
					&& ((StateGroupObj)moving).containsState((StateObj)obj))
				continue;
			candidates.add((StateObj)obj);
		}
		return candidates;
	}

	private int findSpacingSnapDelta(Point center, LinkedList<StateObj> candidates, boolean horizontal)
	{
		int bestDelta = SMART_ALIGN_THRESHOLD + 1;
		for(int i = 0; i < candidates.size(); i++)
		{
			Point a = candidates.get(i).getRealCenter(currPage);
			for(int j = i + 1; j < candidates.size(); j++)
			{
				Point b = candidates.get(j).getRealCenter(currPage);
				if(horizontal)
				{
					if(Math.abs(a.y - center.y) > SMART_SPACING_ROW_THRESHOLD
							|| Math.abs(b.y - center.y) > SMART_SPACING_ROW_THRESHOLD)
						continue;
					bestDelta = closerDelta(bestDelta, (2 * a.x) - b.x - center.x);
					bestDelta = closerDelta(bestDelta, (2 * b.x) - a.x - center.x);
					bestDelta = closerDelta(bestDelta, ((a.x + b.x) / 2) - center.x);
				}
				else
				{
					if(Math.abs(a.x - center.x) > SMART_SPACING_ROW_THRESHOLD
							|| Math.abs(b.x - center.x) > SMART_SPACING_ROW_THRESHOLD)
						continue;
					bestDelta = closerDelta(bestDelta, (2 * a.y) - b.y - center.y);
					bestDelta = closerDelta(bestDelta, (2 * b.y) - a.y - center.y);
					bestDelta = closerDelta(bestDelta, ((a.y + b.y) / 2) - center.y);
				}
			}
		}
		return bestDelta;
	}

	private int closerDelta(int current, int candidate)
	{
		if(Math.abs(candidate) <= SMART_ALIGN_THRESHOLD && Math.abs(candidate) < Math.abs(current))
			return candidate;
		return current;
	}
	
	


	
	public boolean canUndo()
	{
		if (currUndoIndex >= 0)
			return true;
		else
			return false;
	}
	
	@SuppressWarnings("unchecked")
	public void undo()
	{
		//store current array on undo list if it isn't already there
		if(currUndoIndex + 1 == undoList.size())
			undoList.add(objList);
		//replace objlist with a previous list
		if(currUndoIndex > 0)
		{
			objList = (Vector<Object>) undoList.elementAt(currUndoIndex);
			currUndoIndex--;
		}
		//unselect all states
		for (int i = 1; i < objList.size(); i++)
		{
			GeneralObj s = (GeneralObj) objList.elementAt(i);
			s.unselect();				
		}
		objsSelected = false;
		
		//update
		globalList = (LinkedList<LinkedList<ObjAttribute>>) objList.get(0);
		updateStates();
		updateTrans();
		updateGlobalTable();
		FizzimGui fgui = (FizzimGui) frame;
		fgui.updateGlobal(globalList);
		notifyHdlOutOfSync();
		repaint();

	}
	
	public boolean canRedo()
	{
		if(currUndoIndex < undoList.size() - 2)
			return true;
		else
			return false;
	}
	
	@SuppressWarnings("unchecked")
	public void redo()
	{
		//if redo is possible, replace objlist with wanted list
		if(currUndoIndex < undoList.size() - 2)
		{
			objList = (Vector<Object>) undoList.elementAt(currUndoIndex + 2);
			currUndoIndex++;
		}
		globalList = (LinkedList<LinkedList<ObjAttribute>>) objList.get(0);
		updateStates();
		updateTrans();
		updateGlobalTable();
		FizzimGui fgui = (FizzimGui) frame;
		fgui.updateGlobal(globalList);
		notifyHdlOutOfSync();
		repaint();
	}
	
	/* The following two methods create an undo point.  It is not committed to undo list yet
	 * because an undo point is not needed if the user is just clicking to select an 
	 * object (this method is triggered on mouse down)
	 * In this method, a temp list is created to hold all the pointers to objects
	 * before any modification occurs.  The objects to be modified, and all objects connected
	 * to them, are then cloned, and their clones are pointed to by objlist.
	 */
	
	@SuppressWarnings("unchecked")
	//this method is called whenever a global attribute is about to be modified
	public LinkedList<LinkedList<ObjAttribute>> setUndoPoint()
	{
		tempList = null;
		tempList = (Vector<Object>) objList.clone();
		LinkedList<LinkedList<ObjAttribute>> oldGlobal = (LinkedList<LinkedList<ObjAttribute>>) objList.get(0);
		LinkedList<LinkedList<ObjAttribute>> newGlobal = (LinkedList<LinkedList<ObjAttribute>>) oldGlobal.clone();
		objList.set(0,newGlobal);
		globalList = newGlobal;
		
		for(int i = 0; i < oldGlobal.size(); i++)
		{
			LinkedList<ObjAttribute> oldList = (LinkedList<ObjAttribute>)oldGlobal.get(i);
			LinkedList<ObjAttribute> newList = (LinkedList<ObjAttribute>)oldList.clone();
			for(int j = 0; j < oldList.size(); j++)
			{
				try {
					newList.set(j,(ObjAttribute)oldList.get(j).clone());
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
			newGlobal.set(i,newList);
			
		}
		return (LinkedList<LinkedList<ObjAttribute>>) objList.get(0);
		
	}
	
	//for multiple select, clone all selected objects
	private void setUndoPointMultiple() 
	{
		tempList = null;
		tempList = (Vector<Object>) objList.clone();

		LinkedHashMap<GeneralObj, GeneralObj> endpointClones = new LinkedHashMap<GeneralObj, GeneralObj>();

		for(int i = 0; i < selectedIndices.size(); i++)
		{
			GeneralObj oldObj = (GeneralObj) tempList.get(selectedIndices.get(i).intValue());
			GeneralObj clonedObj = cloneObj(oldObj);
			objList.set(selectedIndices.get(i).intValue(), clonedObj);
			if(isTransitionEndpoint(oldObj))
				endpointClones.put(oldObj, clonedObj);
			if(oldObj.getType() == 5)
				cloneContainedEndpointsForBatchUndo((StateGroupObj)oldObj, endpointClones);
		}

		cloneAffectedTransitionsForBatchUndo(endpointClones);
	}

	private GeneralObj cloneObj(GeneralObj oldObj)
	{
		try {
			return (GeneralObj) oldObj.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
			return oldObj;
		}
	}

	private void cloneContainedEndpointsForBatchUndo(StateGroupObj oldGroup, LinkedHashMap<GeneralObj, GeneralObj> endpointClones)
	{
		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldChild = (GeneralObj) tempList.get(i);
			if((oldChild.getType() == 0 || oldChild.getType() == 4)
					&& oldGroup.containsState((StateObj)oldChild)
					&& !endpointClones.containsKey(oldChild))
			{
				GeneralObj clonedChild = cloneObj(oldChild);
				objList.set(i, clonedChild);
				endpointClones.put(oldChild, clonedChild);
			}
		}
	}

	private void cloneAffectedTransitionsForBatchUndo(LinkedHashMap<GeneralObj, GeneralObj> endpointClones)
	{
		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldTrans = (GeneralObj) tempList.get(i);
			if(isTransitionEndpoint(oldTrans))
				continue;

			boolean affected = false;
			for(GeneralObj oldEndpoint : endpointClones.keySet())
			{
				if(oldTrans.containsParent(oldEndpoint))
				{
					affected = true;
					break;
				}
			}
			if(!affected)
				continue;

			GeneralObj clonedTrans = cloneObj(oldTrans);
			for(Map.Entry<GeneralObj, GeneralObj> entry : endpointClones.entrySet())
				clonedTrans.notifyChange(entry.getKey(), entry.getValue());
			clonedTrans.setParentModified(true);
			objList.set(i, clonedTrans);
		}
	}

	private void setUndoPointForRouteEdits()
	{
		tempList = null;
		tempList = (Vector<Object>) objList.clone();
		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldObj = (GeneralObj) tempList.get(i);
			if(oldObj.getType() == 1 || oldObj.getType() == 2)
				objList.set(i, cloneObj(oldObj));
		}
	}

	private void setUndoPointAllObjects()
	{
		tempList = null;
		tempList = (Vector<Object>) objList.clone();
		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldObj = (GeneralObj) tempList.get(i);
			objList.set(i, cloneObj(oldObj));
		}
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1 || obj.getType() == 2)
				((TransitionObj)obj).makeConnections(objList);
		}
	}

	@SuppressWarnings("unchecked")
	private void setUndoPoint(int index, int type)
	{
		tempList = null;
		tempList = (Vector<Object>) objList.clone();

		if(index != -1)
		{

			GeneralObj oldObj = (GeneralObj) tempList.elementAt(index);
			GeneralObj clonedObj = null;
			try {
					clonedObj = (GeneralObj) oldObj.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
	
			tempOld = oldObj;
			tempClone = clonedObj;
				
				
			//if a state, create clone of children transitions

			if(isTransitionEndpointType(type))
			{
				FizzimGui fgui = (FizzimGui) frame;
				fgui.updateGlobal(setUndoPoint());
				boolean parentGeometrySelected = endpointGeometrySelectedForUndo(oldObj);
				for(int i = 1; i < objList.size(); i++)
				{
					GeneralObj s = (GeneralObj) objList.elementAt(i);
					//check all objects that have to be modified state as a parent
					if(!isTransitionEndpoint(s) && s.containsParent(oldObj))
					{
						GeneralObj clonedObj2 = null;
						try {
							clonedObj2 = (GeneralObj) s.clone();
						} catch (CloneNotSupportedException e) {
							e.printStackTrace();
						}

						//replace old obj link in trans obj with new cloned one
						clonedObj2.notifyChange(oldObj, clonedObj);
						clonedObj2.setParentModified(parentGeometrySelected);



						//add cloned child to object list
						int objListIndex = objList.indexOf(s);
						objList.set(objListIndex,clonedObj2);
					}
				}
			}
			if(type == 5)
			{
				cloneContainedEndpointsForUndo((StateGroupObj)oldObj, (StateGroupObj)clonedObj);
			}
			objList.set(index,clonedObj);	
		}
	}

	private void cloneContainedEndpointsForUndo(StateGroupObj oldGroup, StateGroupObj clonedGroup)
	{
		LinkedList<GeneralObj> oldEndpoints = new LinkedList<GeneralObj>();
		LinkedList<GeneralObj> clonedEndpoints = new LinkedList<GeneralObj>();

		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldChild = (GeneralObj) tempList.get(i);
			if((oldChild.getType() == 0 || oldChild.getType() == 4) && oldGroup.containsState((StateObj)oldChild))
			{
				int objListIndex = objList.indexOf(oldChild);
				if(objListIndex < 0)
					continue;

				GeneralObj clonedChild = null;
				try {
					clonedChild = (GeneralObj) oldChild.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				objList.set(objListIndex, clonedChild);
				oldEndpoints.add(oldChild);
				clonedEndpoints.add(clonedChild);
			}
		}

		for(int i = 1; i < tempList.size(); i++)
		{
			GeneralObj oldTrans = (GeneralObj) tempList.get(i);
			if(isTransitionEndpoint(oldTrans))
				continue;

			boolean affected = oldTrans.containsParent(oldGroup);
			for(int j = 0; j < oldEndpoints.size() && !affected; j++)
			{
				if(oldTrans.containsParent(oldEndpoints.get(j)))
					affected = true;
			}
			if(!affected)
				continue;

			GeneralObj transInObjList = (GeneralObj) objList.get(i);
			if(transInObjList == oldTrans)
			{
				try {
					transInObjList = (GeneralObj) oldTrans.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
				objList.set(i, transInObjList);
			}
			transInObjList.notifyChange(oldGroup, clonedGroup);
			for(int j = 0; j < oldEndpoints.size(); j++)
				transInObjList.notifyChange(oldEndpoints.get(j), clonedEndpoints.get(j));
			transInObjList.setParentModified(true);
		}
	}

	private boolean endpointGeometrySelectedForUndo(GeneralObj obj)
	{
		if(!isTransitionEndpoint(obj))
			return false;
		int status = obj.getSelectStatus();
		return status == StateObj.CENTER || status == StateObj.TL || status == StateObj.TR
				|| status == StateObj.BL || status == StateObj.BR;
	}

	//used when properties is cancelled
	public void cancel()
	{
		objList = tempList;
	}
	
	// If a modification actually occurred, 
	// the temp list is stored to the undo list array
	public void commitUndo()
	{
		autoAssignTransitionPriorities();
		refreshTransitionPriorityHighlights();
		int size = undoList.size();
		// clears redo points ahead of current position
		if (currUndoIndex < size - 1)
		{
			
			for (int i = size - 1; i > currUndoIndex; i--)
				undoList.remove(i);
		}
		undoList.add(tempList);
		currUndoIndex++;
		fileModified = true;
		notifyHdlOutOfSync();
		repaint();
	}
	
	public void mouseClicked(MouseEvent e) {	
		//System.out.println("mouseClicked:" + " Button:" + e.getButton() + " Modifiers:" + e.getModifiers() + " Popup Trigger:" + e.isPopupTrigger() + " ControlDown:" + e.isControlDown());
	}

	private GeneralObj selectTransitionEndpoint(MouseEvent e, boolean stateGroupsOnly)
	{
		boolean popupMouse = isPopupMouse(e);
		int x = modelX(e);
		int y = modelY(e);
		for (int i = objList.size() - 1; i >= 1; i--)
		{
			GeneralObj s = (GeneralObj) objList.elementAt(i);
			if(!isTransitionEndpoint(s))
				continue;
			if(stateGroupsOnly && s.getType() != 5)
				continue;
			if(!stateGroupsOnly && s.getType() == 5)
				continue;

			if(s.setSelectStatus(x,y))
			{
				if(popupMouse)
				{
					setUndoPoint(i,s.getType());
					unselectObjsExcept(i);
					queuePopup(s,e,1);
				}
				else
				{
					setUndoPoint(i,s.getType());
					unselectObjsExcept(i);
				}
				return s;
			}
		}
		return null;
	}

	private GeneralObj selectTransitionEndpointType(MouseEvent e, int endpointType)
	{
		boolean popupMouse = isPopupMouse(e);
		int x = modelX(e);
		int y = modelY(e);
		for(int i = objList.size() - 1; i >= 1; i--)
		{
			GeneralObj s = (GeneralObj)objList.elementAt(i);
			if(s.getType() != endpointType)
				continue;

			if(s.setSelectStatus(x, y))
			{
				if(popupMouse)
				{
					setUndoPoint(i, s.getType());
					unselectObjsExcept(i);
					queuePopup(s, e, 1);
				}
				else
				{
					setUndoPoint(i, s.getType());
					unselectObjsExcept(i);
				}
				return s;
			}
		}
		return null;
	}

	public void mouseEntered(MouseEvent e) {
		//System.out.println("mouseEntered:" + " Button:" + e.getButton() + " Modifiers:" + e.getModifiers() + " Popup Trigger:" + e.isPopupTrigger() + " ControlDown:" + e.isControlDown());
	}

	public void mouseExited(MouseEvent e) {
		//System.out.println("mouseeExited:" + " Button:" + e.getButton() + " Modifiers:" + e.getModifiers() + " Popup Trigger:" + e.isPopupTrigger() + " ControlDown:" + e.isControlDown());
		updateHoverObject(null);
	}
	
	public void mouseHandle(MouseEvent e) {
		
		
	}

	public void mousePressed(MouseEvent e) {
		
		//System.out.println("mousePressed:" + " Button:" + e.getButton() + " Modifiers:" + e.getModifiers() + " Popup Trigger:" + e.isPopupTrigger() + " ControlDown:" + e.isControlDown());
		boolean popupMouse = isPopupMouse(e);
		if(popupMouse)
			startPanCandidate(e);
		int x = modelX(e);
		int y = modelY(e);
		pressModelX = x;
		pressModelY = y;
		pressTime = e.getWhen();
		pendingClickCycle = false;
		pendingClickCycleHits = null;
		GeneralObj bestMatch = null;
		boolean doubleClick = false;
		//check for double click
		if(e.getWhen() - lastClick < dClickTime && e.getButton() == MouseEvent.BUTTON1 && e.getModifiers() != 20)
			doubleClick = true;
		
		lastClick = e.getWhen();
		boolean plainLeftClick = !popupMouse && !e.isControlDown() && !e.isShiftDown() && !objsSelected
				&& e.getButton() == MouseEvent.BUTTON1 && e.getModifiers() != 20;
		LinkedList<HitCandidate> clickHits = plainLeftClick ? getHitCandidates(x, y) : null;
		if(plainLeftClick && canContinueClickCycle(e, x, y, clickHits))
		{
			pendingClickCycle = true;
			pendingClickCycleHits = clickHits;
		}
		if(popupMouse || e.isControlDown() || e.isShiftDown())
		{
			resetClickCycle();
		}

		boolean modifiedSelectionClick = !popupMouse && (e.isControlDown() || e.isShiftDown())
				&& e.getButton() == MouseEvent.BUTTON1 && e.getModifiers() != 20;
		if(modifiedSelectionClick)
		{
			int selectionMode = selectionModeForEvent(e);
			int hitIndex = findSelectableIndexAt(x, y);
			if(hitIndex >= 0)
			{
				applyModifiedClickSelection(hitIndex, selectionMode, x, y);
				return;
			}
			mXTemp = x;
			mYTemp = y;
			mX0 = 0;
			mY0 = 0;
			mX1 = 0;
			mY1 = 0;
			multipleSelect = true;
			selectionBoxMode = selectionMode;
			refreshSelectedIndicesFromObjects();
			boxBaseSelectedIndices = (LinkedList<Integer>)selectedIndices.clone();
			return;
		}

		//if multiple object selected
		if(objsSelected)
		{
			setUndoPointMultiple();
			if(e.getButton() == MouseEvent.BUTTON1 && e.getModifiers() != 20)
			{
				boolean move = false;
			
				for(int i = 0; i < selectedIndices.size(); i++)
				{
					GeneralObj obj = (GeneralObj) objList.get(selectedIndices.get(i).intValue());
					if((isTransitionEndpoint(obj) || obj.getType() == 3) && obj.setBoxSelectStatus(x,y))
					{
						move = true;
					}
				}
				//if click outside, unselect all
				if(!move)
				{
					objsSelected = false;
					unselectObjs();
				}
			}
			else if(popupMouse)
			{
				queuePopup(null,e,1);
			}
		}
		else
		{
			bestMatch = selectTransitionEndpointType(e, 4);
			if(bestMatch != null && doubleClick)
			{
				new StateProperties(this,frame, true, (StateObj) bestMatch).setVisible(true);
				return;
			}

			//if object already selected
			for (int i = 1; bestMatch == null && i < objList.size(); i++)
			{
				GeneralObj s = (GeneralObj) objList.elementAt(i);
				if(s.getSelectStatus() != 0 && s.setSelectStatus(x,y))
				{
					bestMatch = s;
						
					if(!doubleClick)
					{
						//if right click, create popup menu
						if(popupMouse)
						{
							setUndoPoint(i,s.getType());
							unselectObjsExcept(i);
							queuePopup(s,e,1);
						}
						else
						{
							setUndoPoint(i,s.getType());
							unselectObjsExcept(i);
						}
						break;
					}
					else
					{
						if(s.getType() == 0 || s.getType() == 4 || s.getType() == 5)
						{
							new StateProperties(this,frame, true, (StateObj) s)
							.setVisible(true);
						}
						else if(s.getType() == 1)
						{
							Vector<StateObj> stateObjs = new Vector<StateObj>();
							for(int j = 1; j < objList.size(); j++)
							{
								GeneralObj obj = (GeneralObj)objList.get(j);
								if(isTransitionEndpoint(obj))
									stateObjs.add((StateObj)obj);
							}
				        	new TransProperties(this,frame, true, (StateTransitionObj) s,stateObjs,false,null)
							.setVisible(true);
						}
						else if(s.getType() == 2)
						{
							Vector<StateObj> stateObjs = new Vector<StateObj>();
				    		for(int j = 1; j < objList.size(); j++)
				    		{
				    			GeneralObj obj = (GeneralObj)objList.get(j);
				    			if(obj.getType() == 0)
				    				stateObjs.add((StateObj)obj);
				    		}
				        	new TransProperties(this,frame, true, (LoopbackTransitionObj) s,stateObjs,true,null)
							.setVisible(true);
						}
						else if(s.getType() == 3)
						{
							editText((TextObj) s);
						}
					}
				}
			}
		
	
			//check for text at mouse location
			if(bestMatch == null)
			{
				for (int i = 1; i < objList.size(); i++)
				{
					GeneralObj s = (GeneralObj) objList.elementAt(i);
					if(s.getType() == 3 && !((TextObj)s).getGlobalTable() && s.setSelectStatus(x,y))
					{
						bestMatch = s;
							
						if(popupMouse)
						{
							setUndoPoint(i,3);
							unselectObjsExcept(i);
							queuePopup(s,e,1);
						}
						else
						{
							setUndoPoint(i,3);
							unselectObjsExcept(i);
						}
						break;
					}
				}
			}
			
			//check for transition at mouse position
			if(bestMatch == null)
			{
				for (int i = 1; i < objList.size(); i++)
				{
					GeneralObj s = (GeneralObj) objList.elementAt(i);
					if((s.getType() == 1 || s.getType() == 2) && s.setSelectStatus(x,y))
					{
						bestMatch = s;
						int type = s.getType();
	
						if(popupMouse)
						{
							setUndoPoint(i,type);
							unselectObjsExcept(i);
							queuePopup(s,e,1);
						}
						else
						{
							setUndoPoint(i,type);
							unselectObjsExcept(i);
						}
						break;
					}
				}
			}
	
			//if no transitions found at that position, look through state objects
			if(bestMatch == null)
			{
				bestMatch = selectTransitionEndpoint(e, false);
			}
			if(bestMatch == null)
			{
				bestMatch = selectTransitionEndpoint(e, true);
			}
			
			//if nothing is clicked on, and right click
			if(bestMatch == null && popupMouse)
			{
				queuePopup(null,e,2);
				setUndoPoint(-1,-1);
			}
			
			//now do multiple select if still nothing found
			if(bestMatch == null && e.getButton() == MouseEvent.BUTTON1 && e.getModifiers() != 20)
			{
				resetClickCycle();
				unselectObjs();
				mXTemp = x;
				mYTemp = y;
				mX0 = 0;
				mY0 = 0;
				mX1 = 0;
				mY1 = 0;
				multipleSelect = true;
				selectionBoxMode = SELECT_REPLACE;
				boxBaseSelectedIndices.clear();
				objsSelected = false;
				selectedIndices.clear();
				notifySelectionChanged();
			}
			else if(plainLeftClick)
			{
				rememberClickCycle(e, x, y, clickHits, bestMatch);
			}
			
			repaint();
		}
	}

	public void mouseReleased(MouseEvent e) {
		//System.out.println("mouseReleased:" + " Button:" + e.getButton() + " Modifiers:" + e.getModifiers() + " Popup Trigger:" + e.isPopupTrigger() + " ControlDown:" + e.isControlDown());	
		boolean showPendingPopup = rightButtonDown && !rightButtonDragged && pendingPopupEvent != null;
	
		multipleSelect = false;
		blankCanvasPan = false;
		if(!objsSelected)
		{
			selectedIndices.clear();
			notifySelectionChanged();
		}
		
		//done modifying all objects, so notify state objects that they are done changing
		//this means transition object won't have to re-calculate connection points
		
		toCommit = false;
			
		for (int i = 1; i < objList.size(); i++)
		{
			GeneralObj t = (GeneralObj) objList.elementAt(i);
			//check for modified state
			if (t.isModified() && isTransitionEndpoint(t))
			{
				toCommit = true;
				for(int j = 1; j < objList.size(); j++)
				{
					GeneralObj obj = (GeneralObj) objList.elementAt(j);
					if(!isTransitionEndpoint(obj) && obj.isParentModified())
						obj.updateObj();
					obj.setParentModified(false);
				}
			}

			//check for modified line or text box
			if((t.getType() == 1 || t.getType() == 2) && t.isModified())
			{
				toCommit = true;
				for(int j = 1; j < objList.size(); j++)
				{
					GeneralObj obj = (GeneralObj) objList.elementAt(j);
					if((obj.getType() == 3) && obj.isParentModified())
						obj.updateObj();
					obj.setParentModified(false);
				}
			}
			
			if(t.getType() == 3 && t.isModified())
			{
				toCommit = true;
			}
			t.setModified(false);
		}
			
		if(toCommit)
		{
			commitUndo();
			validateStateGroupMembership(true);
			updateCanvasExtents();
		}

		if(pendingClickCycle && !toCommit && e.getButton() == MouseEvent.BUTTON1
				&& e.getWhen() - pressTime <= QUICK_CLICK_CYCLE_MS
				&& Math.abs(modelX(e) - pressModelX) <= PAN_DRAG_THRESHOLD
				&& Math.abs(modelY(e) - pressModelY) <= PAN_DRAG_THRESHOLD)
		{
			selectFromClickCycle(e, modelX(e), modelY(e), pendingClickCycleHits);
		}
		pendingClickCycle = false;
		pendingClickCycleHits = null;

		if(showPendingPopup)
		{
			if(pendingPopupType == 1)
				createPopup(pendingPopupObj, pendingPopupEvent);
			else if(pendingPopupType == 2)
				createPopup(pendingPopupEvent);
		}
		rightButtonDown = false;
		rightButtonDragged = false;
		panScreenStart = null;
		panViewStart = null;
		clearPendingPopup();
		clearSmartGuides();
		
		repaint();
	}
	
public void updateTransitions()
{
	for(int j = 1; j < objList.size(); j++)
	{				
		GeneralObj obj = (GeneralObj) objList.elementAt(j);
		if(!isTransitionEndpoint(obj) && obj.isParentModified())
			obj.updateObj();
		
	}
	
}

	public void mouseDragged(MouseEvent arg0) {
		if(panCanvas(arg0))
			return;
		if(panBlankCanvas(arg0))
			return;
		pendingClickCycle = false;
		pendingClickCycleHits = null;
		resetClickCycle();

		//keep movement within page
		int x = modelX(arg0);
		int y = modelY(arg0);

		// move object if multiple select is off
		boolean leftButtonDrag = (arg0.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
		if(!multipleSelect && !arg0.isControlDown() && leftButtonDrag)
		{
			HashSet<GeneralObj> movedBySelectedGroup = new HashSet<GeneralObj>();
			LinkedList<StateGroupObj> selectedGroups = objsSelected ? getSelectedStateGroups() : new LinkedList<StateGroupObj>();
			LinkedList<GeneralObj> movedEndpointsBatch = new LinkedList<GeneralObj>();
			boolean snapDisabled = arg0.isAltDown();
			for (int i = 1; i < objList.size(); i++)
			{
				GeneralObj s = (GeneralObj) objList.elementAt(i);
				if(movedBySelectedGroup.contains(s))
					continue;
				if(s.getSelectStatus() != 0)
				{
					if(objsSelected && (s.getType() == 0 || s.getType() == 4)
							&& isContainedInSelectedStateGroup((StateObj)s, selectedGroups))
						continue;
					int[] oldCoords = null;
					LinkedList<StateObj> childEndpoints = new LinkedList<StateObj>();
					if(s.getType() == 5 && s.getSelectStatus() == StateObj.CENTER)
					{
						oldCoords = ((StateObj)s).getCoords();
						childEndpoints = getContainedTransitionEndpoints((StateGroupObj)s);
					}
					boolean batchMove = objsSelected;
					boolean disableEndpointSnap = (snapDisabled || batchMove) && isTransitionEndpoint(s) && s.getSelectStatus() == StateObj.CENTER;
					if(disableEndpointSnap)
						((StateObj)s).setGrid(false, gridS);
					s.adjustShapeOrPosition(x,y);
					if(disableEndpointSnap)
						((StateObj)s).setGrid(grid, gridS);
					if(!batchMove && isTransitionEndpoint(s) && s.getSelectStatus() == StateObj.CENTER)
						applySmartAlignment((StateObj)s, movedBySelectedGroup, snapDisabled);
					if(oldCoords != null)
					{
						int[] newCoords = ((StateObj)s).getCoords();
						int dx = newCoords[0] - oldCoords[0];
						int dy = newCoords[1] - oldCoords[1];
						if(dx != 0 || dy != 0)
						{
							LinkedList<GeneralObj> movedEndpoints = new LinkedList<GeneralObj>();
							movedEndpoints.add(s);
							for(int k = 0; k < childEndpoints.size(); k++)
							{
								childEndpoints.get(k).moveBy(dx, dy);
								movedBySelectedGroup.add(childEndpoints.get(k));
								movedEndpoints.add(childEndpoints.get(k));
							}
							movedEndpointsBatch.addAll(movedEndpoints);
						}
					}
					else if(endpointGeometrySelectedForUndo(s))
					{
						movedEndpointsBatch.add(s);
					}
					//break;

				}
			}
			updateTransitionsForMovedEndpoints(movedEndpointsBatch);
			for(int j = 1; j < objList.size(); j++)
			{
				GeneralObj obj = (GeneralObj) objList.elementAt(j);
				if(!isTransitionEndpoint(obj) && obj.isParentModified())
					obj.updateObj();
			}
			expandCanvasExtentsForDrag();
			repaint();
		}

		//if multiple select is on, then check if any objects inside yet
		else if(multipleSelect && leftButtonDrag)
		{
			// correct box coordinates
			if(x<mXTemp)
			{
				mX0 = x;
				mX1 = mXTemp;
			}
			else
			{
				mX0 = mXTemp;
				mX1 = x;
			}
			if(y<mYTemp)
			{
				mY0 = y;
				mY1 = mYTemp;
			}
			else
			{
				mY0 = mYTemp;
				mY1 = y;
			}
			
			applyBoxSelection();
			repaint();
		}
	}

	public void mouseMoved(MouseEvent arg0) {
		setToolTipText(null);
		int x = modelX(arg0);
		int y = modelY(arg0);
		updateHoverObject(findHoverObject(x, y));
		boolean overRouteHandle = false;
		for (int i = objList.size() - 1; i >= 1; i--)
		{
			GeneralObj obj = (GeneralObj) objList.elementAt(i);
			if(obj.getType() == 4 && ((ForkObj)obj).containsPoint(x, y))
			{
				setToolTipText(obj.getName());
				break;
			}
			if(obj.getType() == 1 && ((StateTransitionObj)obj).isRouteEditHit(x, y))
			{
				overRouteHandle = true;
				break;
			}
			if(obj.getType() == 2 && ((LoopbackTransitionObj)obj).isRouteEditHit(x, y))
			{
				overRouteHandle = true;
				break;
			}
		}
		setCursor(Cursor.getPredefinedCursor(overRouteHandle ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if(e.isShiftDown() && !e.isControlDown())
		{
			panViewportByWheel(e);
			return;
		}

		e.consume();
		double zoomStep = e.isControlDown() ? 1.1 : 1.06;
		zoomAt(e, Math.pow(zoomStep, -e.getPreciseWheelRotation()));
	}

	private void zoomAt(MouseEvent e, double factor)
	{
		double oldZoom = zoom;
		double newZoom = clampZoom(oldZoom * factor);
		if(newZoom == oldZoom)
			return;
		notifyViewManuallyChanged();

		JViewport viewport = (JViewport)SwingUtilities.getAncestorOfClass(JViewport.class, this);
		Point cursorInViewport = null;
		double anchorModelX = e.getX() / oldZoom;
		double anchorModelY = e.getY() / oldZoom;
		if(viewport != null)
		{
			Point viewPosition = viewport.getViewPosition();
			cursorInViewport = new Point(e.getX() - viewPosition.x, e.getY() - viewPosition.y);
		}

		zoom = newZoom;
		updateZoomedSize();

		if(viewport != null && cursorInViewport != null)
		{
			int newViewX = (int)Math.round(anchorModelX * zoom - cursorInViewport.x);
			int newViewY = (int)Math.round(anchorModelY * zoom - cursorInViewport.y);
			setViewportPosition(viewport, newViewX, newViewY);
		}

		repaint();
		notifyZoomChanged();
	}
	
	
	public void createPopup(GeneralObj obj, MouseEvent e)
	{
		// store location of click and object that is clicked on
		rXTemp = modelX(e);
		rYTemp = modelY(e);
		tempObj = obj;

        JMenuItem menuItem;

        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        
        //create submenu for moving pages
        JMenu pages = new JMenu("Move to Page...");
        FizzimGui fgui = (FizzimGui) frame;
        
        for(int i = 1; i < fgui.getPages(); i++)
        {
        	if(i != currPage)
        	{
	        	menuItem = new JMenuItem(fgui.getPageName(i));
		        menuItem.addActionListener(this);
		        pages.add(menuItem);
        	}
        }
        if(obj == null)
        	popup.add(pages);
        
        if(obj != null && isTransitionEndpoint(obj))
        {
			if(obj.getType() == 0)
			{
				menuItem = new JMenuItem("Add Loopback Transition");
				menuItem.setMnemonic(KeyEvent.VK_L);
				menuItem.addActionListener(this);
				popup.add(menuItem);
			}

			JMenu states = new JMenu("Add State Transition to...");
			states.setMnemonic(KeyEvent.VK_T);
			states.setDisplayedMnemonicIndex(10);
			for(int j = 1; j < objList.size(); j++)
			{
				GeneralObj obj1 = (GeneralObj)objList.get(j);
				if(isTransitionEndpoint(obj1) && !obj.getName().equals(obj1.getName()))
				{
					menuItem = new JMenuItem(obj1.getName());
					menuItem.addActionListener(this);
					states.add(menuItem);
				}
			}
			popup.add(states);

			if(obj.getType() == 0 || obj.getType() == 5)
			{
				if(obj.getType() == 5)
					menuItem = new JMenuItem("Edit State Group Properties");
				else
					menuItem = new JMenuItem("Edit State Properties");
				menuItem.setMnemonic(KeyEvent.VK_E);
				menuItem.addActionListener(this);
				popup.add(menuItem);
			}
			popup.add(pages);
        }
        if(obj != null && obj.getType() == 1)
        {

        	menuItem = new JMenuItem("Edit State Transition Properties");
        	menuItem.setMnemonic(KeyEvent.VK_E);
	        menuItem.addActionListener(this);
	        popup.add(menuItem);
	        menuItem = new JMenuItem("Reset Transition Route");
	        menuItem.addActionListener(this);
	        popup.add(menuItem);
	        if(obj.getSelectStatus() == StateTransitionObj.TXT)
	        {
	        	JMenu pages2 = new JMenu("Move to Page...");
	            StateTransitionObj sobj = (StateTransitionObj) obj;
	            for(int i = 1; i < fgui.getPages(); i++)
	            {
	            	if(i != currPage && (i == sobj.getEPage() || i == sobj.getSPage()))
	            	{
		            	menuItem = new JMenuItem(fgui.getPageName(i));
		    	        menuItem.addActionListener(this);
		    	        pages2.add(menuItem);
	            	}
	            }
	        	popup.add(pages2);
	        }
	                
        }
        if(obj != null && obj.getType() == 2)
        {
	        menuItem = new JMenuItem("Edit Loopback Transition Properties");
	        menuItem.setMnemonic(KeyEvent.VK_E);
	        menuItem.addActionListener(this);
	        popup.add(menuItem);
	        menuItem = new JMenuItem("Reset Transition Route");
	        menuItem.addActionListener(this);
	        popup.add(menuItem);
        }
        if(obj != null && obj.getType() == 3)
        {
	        menuItem = new JMenuItem("Edit Text");
	        menuItem.setMnemonic(KeyEvent.VK_E);
	        menuItem.addActionListener(this);
	        popup.add(menuItem);
	        popup.add(pages);
        }
        
        popup.show(e.getComponent(), e.getX(),e.getY());

	}
	
	public void createPopup(MouseEvent e)
	{
		rXTemp = modelX(e);
		rYTemp = modelY(e);
		tempObj = null;
        JMenuItem menuItem;

        //Create the popup menu.
        JPopupMenu popup = new JPopupMenu();
        menuItem = new JMenuItem("New State");
        menuItem.setMnemonic(KeyEvent.VK_Q);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New State...");
        menuItem.setMnemonic(KeyEvent.VK_S);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New Fork");
        menuItem.setMnemonic(KeyEvent.VK_K);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New State Group");
        menuItem.setMnemonic(KeyEvent.VK_U);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New State Transition");
        menuItem.setMnemonic(KeyEvent.VK_T);
        menuItem.setDisplayedMnemonicIndex(10);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New Loopback Transition");
        menuItem.setMnemonic(KeyEvent.VK_L);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        menuItem = new JMenuItem("New Free Text");
        menuItem.setMnemonic(KeyEvent.VK_F);
        menuItem.addActionListener(this);
        popup.add(menuItem);
        
        popup.show(e.getComponent(), e.getX(),e.getY());

	}
	
  
	// called when item on popup menu is selected
    public void actionPerformed(ActionEvent e) {
        JMenuItem source = (JMenuItem)(e.getSource());
        
        //if cloned for undo
        if(tempObj == tempOld)
        	tempObj = tempClone;
        
        String input = source.getText();

        
        if(input == "Edit Text")
        {
        	editText((TextObj) tempObj);
        }
        else if(input == "Edit State Properties")
        {
			new StateProperties(this,frame, true, (StateObj) tempObj)
			.setVisible(true);
        }
        else if(input == "Edit State Group Properties")
        {
			new StateProperties(this,frame, true, (StateObj) tempObj)
			.setVisible(true);
        }
        else if(input == "Edit Loopback Transition Properties")
        {
        	Vector<StateObj> stateObjs = new Vector<StateObj>();
    		for(int i = 1; i < objList.size(); i++)
    		{
    			GeneralObj obj = (GeneralObj)objList.get(i);
				if(obj.getType() == 0)
					stateObjs.add((StateObj)obj);
    		}
        	new TransProperties(this,frame, true, (LoopbackTransitionObj) tempObj,stateObjs,true,null)
			.setVisible(true);
        }
        else if(input == "Edit State Transition Properties")
        {
        	Vector<StateObj> stateObjs = new Vector<StateObj>();
    		for(int i = 1; i < objList.size(); i++)
    		{
    			GeneralObj obj = (GeneralObj)objList.get(i);
				if(isTransitionEndpoint(obj))
					stateObjs.add((StateObj)obj);
    		}
        	new TransProperties(this,frame, true, (StateTransitionObj) tempObj,stateObjs,false,null)
			.setVisible(true);
        }
        else if(input == "Reset Transition Route")
        {
			if(tempObj != null && tempObj.getType() == 1)
				((StateTransitionObj) tempObj).resetRoute();
			else if(tempObj != null && tempObj.getType() == 2)
				((LoopbackTransitionObj) tempObj).resetRoute();
			commitUndo();
        }
        else if(input == "New State")
        {
			GeneralObj state = new StateObj(rXTemp-StateW/2,rYTemp-StateH/2,rXTemp+StateW/2,rYTemp+StateH/2,createSCounter, currPage, defSC,grid, gridS);
			createSCounter++;
    		objList.add(state);
    		state.updateAttrib(globalList,3);
    		commitUndo();

        }
        else if(input == "New State...")
        {
			GeneralObj state = new StateObj(rXTemp-StateW/2,rYTemp-StateH/2,rXTemp+StateW/2,rYTemp+StateH/2,createSCounter, currPage, defSC,grid,gridS);
			createSCounter++;
			objList.add(state);
			state.updateAttrib(globalList,3);
			new StateProperties(this,frame, true, (StateObj) state)
			.setVisible(true);
        }
        else if(input == "New Fork")
        {
			GeneralObj fork = new ForkObj(rXTemp,rYTemp,createSCounter, currPage, defSTC,grid,gridS);
			createSCounter++;
			objList.add(fork);
			commitUndo();
        }
        else if(input == "New State Group")
        {
			GeneralObj stateGroup = new StateGroupObj(rXTemp-StateW,rYTemp-StateH,rXTemp+StateW,rYTemp+StateH,createSCounter, currPage, defSC,grid,gridS);
			createSCounter++;
			objList.add(stateGroup);
			stateGroup.updateAttrib(globalList,3);
			new StateProperties(this,frame, true, (StateObj) stateGroup)
			.setVisible(true);
        }
        else if(input == "New State Transition")
        {
			Vector<StateObj> stateObjs = new Vector<StateObj>();
			for(int i = 1; i < objList.size(); i++)
			{
				GeneralObj obj = (GeneralObj)objList.get(i);

				if(isTransitionEndpoint(obj))
				{
					stateObjs.add((StateObj)obj);
				}

			}
    		if(stateObjs.size() > 1)
    		{
	        	GeneralObj trans = new StateTransitionObj(createTCounter,currPage,this, defSTC);
	    		createTCounter++;
	    		objList.add(trans);
	    		trans.updateAttrib(globalList,4);
	    		new TransProperties(this,frame, true, (TransitionObj) trans, stateObjs,false,null)
				.setVisible(true);
    		}
    		else
    		{
    			JOptionPane.showMessageDialog(this,
                        "Must be more than 2 states before a transition can be created",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
    		}
        }
        else if(input == "Add Loopback Transition")
        {
        	Vector<StateObj> stateObjs = new Vector<StateObj>();
    		for(int i = 1; i < objList.size(); i++)
    		{
    			GeneralObj obj = (GeneralObj)objList.get(i);
    			
    			if(obj.getType() == 0)
    			{
    				stateObjs.add((StateObj)obj);	
    			}
    			
    		}
    		if(stateObjs.size() > 0)
    		{
	        	GeneralObj trans = new LoopbackTransitionObj(rXTemp,rYTemp,createTCounter,currPage, defLTC);
	    		createTCounter++;
	    		objList.add(trans);
	    		trans.updateAttrib(globalList,4);
	    		new TransProperties(this,frame, true, (TransitionObj) trans, stateObjs,true,(StateObj)tempObj)
				.setVisible(true);
    		}
    		else
    		{
    			JOptionPane.showMessageDialog(this,
                        "Must be more than 1 states before a loopback transition can be created",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
    		}
        }
        else if(input == "New Loopback Transition")
        {
        	Vector<StateObj> stateObjs = new Vector<StateObj>();
    		for(int i = 1; i < objList.size(); i++)
    		{
    			GeneralObj obj = (GeneralObj)objList.get(i);
    			
    			if(obj.getType() == 0)
    			{
    				stateObjs.add((StateObj)obj);	
    			}
    			
    		}
    		if(stateObjs.size() > 0)
    		{
	        	GeneralObj trans = new LoopbackTransitionObj(rXTemp,rYTemp,createTCounter,currPage, defLTC);
	    		createTCounter++;
	    		objList.add(trans);
	    		trans.updateAttrib(globalList,4);
	    		new TransProperties(this,frame, true, (TransitionObj) trans, stateObjs,true,null)
				.setVisible(true);
    		}
    		else
    		{
    			JOptionPane.showMessageDialog(this,
                        "Must be more than 1 states before a loopback transition can be created",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
    		}
        }
        else if(input == "New Free Text")
        {
        	GeneralObj text = new TextObj("",rXTemp,rYTemp,currPage);
    		objList.add(text);
        	editText((TextObj) text);
        }
        else if(checkStateName(input))
        {
        	GeneralObj trans = new StateTransitionObj(createTCounter,currPage,this,(StateObj)tempObj,getStateObj(input), defSTC);
    		createTCounter++;
    		objList.add(trans);
    		StateTransitionObj sTrans = (StateTransitionObj) trans;
    		sTrans.initTrans((StateObj)tempObj,getStateObj(input));
    		
    		trans.updateAttrib(globalList,4);
    		commitUndo();
        }
        else if(getPageIndex(input) > -1)
        {

	        int page = getPageIndex(input);
	        
	        if(page != currPage)
	        {
	        	if(!objsSelected)
	        	{
	        		tempObj.setPage(page);
		       
		        	for(int j = 1; j < objList.size(); j++)
					{
						GeneralObj obj = (GeneralObj) objList.elementAt(j);
						if(!isTransitionEndpoint(obj) && obj.isParentModified())
							obj.updateObj();
						obj.setParentModified(false);
					}
		        	tempObj.setModified(false);
		        	tempObj.updateObj();
		        	commitUndo();
		        	unselectObjs();
	        	}
	        	else
	        	{
	        		//move all states
	        		for(int i = 0; i < selectedIndices.size(); i++)
					{
	        			GeneralObj obj = (GeneralObj) objList.get(selectedIndices.get(i).intValue());
						if(isTransitionEndpoint(obj) || obj.getType() == 3)
						{
							obj.setPage(page);
							if(isTransitionEndpoint(obj))
								obj.setModified(true);
						}
					}
	        		for(int j = 1; j <objList.size(); j++)
	        		{
	        			GeneralObj obj = (GeneralObj) objList.elementAt(j);
						if(!isTransitionEndpoint(obj) && obj.isParentModified())
	        			{
							TransitionObj trans = (TransitionObj) obj;
	        				trans.updateObjPages(page);
	        			}
						obj.setParentModified(false);
					}
	        		for(int k = 0; k < selectedIndices.size(); k++)
					{
	        			GeneralObj obj = (GeneralObj) objList.get(selectedIndices.get(k).intValue());
						if(isTransitionEndpoint(obj))
						{
							obj.setModified(false);
						}
					}
	        		
	        		unselectObjs();
	        		objsSelected = false;
	        		multipleSelect = false;
	        		selectedIndices.clear();
	        		commitUndo();

	        	}
	        	
	        }
        }
        repaint();

    }
    
    private int getPageIndex(String input) {
		FizzimGui fgui = (FizzimGui) frame;
		return fgui.getPageIndex(input);
	}

	private StateObj getStateObj(String name)
    {
    	for(int j = 1; j < objList.size(); j++)
        {
        	GeneralObj obj1 = (GeneralObj)objList.get(j);
			if(isTransitionEndpoint(obj1) && obj1.getName().equals(name))
				return (StateObj) obj1;
        }
        return null;
    }
    
    private boolean checkStateName(String input)
    {
        for(int j = 1; j < objList.size(); j++)
        {
        	GeneralObj obj1 = (GeneralObj)objList.get(j);
			if(isTransitionEndpoint(obj1) && obj1.getName().equals(input))
				return true;
        }
        return false;
    }

	private boolean isTransitionEndpoint(GeneralObj obj)
    {
		return obj.getType() == 0 || obj.getType() == 4 || obj.getType() == 5;
    }

	public Vector<StateObj> getTransitionEndpointObjects()
	{
		Vector<StateObj> endpoints = new Vector<StateObj>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(isTransitionEndpoint(obj))
				endpoints.add((StateObj)obj);
		}
		return endpoints;
	}

	private LinkedList<StateObj> getContainedTransitionEndpoints(StateGroupObj stateGroup)
	{
		LinkedList<StateObj> states = new LinkedList<StateObj>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if((obj.getType() == 0 || obj.getType() == 4) && stateGroup.containsState((StateObj)obj))
				states.add((StateObj)obj);
		}
		return states;
	}

	private LinkedList<StateGroupObj> getSelectedStateGroups()
	{
		LinkedList<StateGroupObj> groups = new LinkedList<StateGroupObj>();
		for(int i = 0; i < selectedIndices.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(selectedIndices.get(i).intValue());
			if(obj.getType() == 5 && obj.getSelectStatus() == StateObj.CENTER)
				groups.add((StateGroupObj)obj);
		}
		return groups;
	}

	private boolean isContainedInSelectedStateGroup(StateObj endpoint, LinkedList<StateGroupObj> selectedGroups)
	{
		for(int i = 0; i < selectedGroups.size(); i++)
		{
			if(selectedGroups.get(i).containsState(endpoint))
				return true;
		}
		return false;
	}

	private LinkedList<StateObj> getContainedStates(StateGroupObj stateGroup)
	{
		LinkedList<StateObj> states = new LinkedList<StateObj>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0 && stateGroup.containsState((StateObj)obj))
				states.add((StateObj)obj);
		}
		return states;
	}

	private void updateTransitionsForMovedEndpoints(LinkedList<GeneralObj> movedEndpoints)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.elementAt(i);
			if(isTransitionEndpoint(obj))
				continue;

			boolean affected = false;
			for(int j = 0; j < movedEndpoints.size(); j++)
			{
				if(obj.containsParent(movedEndpoints.get(j)))
				{
					affected = true;
					break;
				}
			}
			if(affected)
			{
				obj.setParentModified(true);
				obj.updateObj();
				obj.setParentModified(false);
			}
		}
	}

	private void nudgeSelectedEndpoints(int dx, int dy)
	{
		refreshSelectedIndicesFromObjects();
		LinkedList<Integer> movableIndices = new LinkedList<Integer>();
		for(int i = 0; i < selectedIndices.size(); i++)
		{
			int index = selectedIndices.get(i).intValue();
			GeneralObj obj = (GeneralObj)objList.get(index);
			if(isTransitionEndpoint(obj) && obj.getSelectStatus() == StateObj.CENTER)
				movableIndices.add(new Integer(index));
		}
		if(movableIndices.size() == 0)
			return;

		setUndoPointMultiple();
		LinkedList<GeneralObj> movedEndpointsBatch = new LinkedList<GeneralObj>();
		HashSet<GeneralObj> movedBySelectedGroup = new HashSet<GeneralObj>();
		LinkedList<StateGroupObj> selectedGroups = getSelectedStateGroups();

		for(int i = 0; i < movableIndices.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(movableIndices.get(i).intValue());
			if(movedBySelectedGroup.contains(obj))
				continue;
			if((obj.getType() == 0 || obj.getType() == 4)
					&& isContainedInSelectedStateGroup((StateObj)obj, selectedGroups))
				continue;

			((StateObj)obj).moveBy(dx, dy);
			movedEndpointsBatch.add(obj);
			if(obj.getType() == 5)
			{
				LinkedList<StateObj> childEndpoints = getContainedTransitionEndpoints((StateGroupObj)obj);
				for(int j = 0; j < childEndpoints.size(); j++)
				{
					childEndpoints.get(j).moveBy(dx, dy);
					movedBySelectedGroup.add(childEndpoints.get(j));
					movedEndpointsBatch.add(childEndpoints.get(j));
				}
			}
		}

		updateTransitionsForMovedEndpoints(movedEndpointsBatch);
		for(int j = 1; j < objList.size(); j++)
		{
			GeneralObj obj = (GeneralObj) objList.elementAt(j);
			if(!isTransitionEndpoint(obj) && obj.isParentModified())
				obj.updateObj();
		}
		validateStateGroupMembership(true);
		updateCanvasExtents();
		commitUndo();
		refreshSelectedIndicesFromObjects();
		syncSelectionState();
		repaint();
	}

    private boolean isTransitionEndpointType(int type)
    {
		return type == 0 || type == 4 || type == 5;
    }

    // update state attribute lists when global list is updated
	public void updateStates() {
		String resetName = null;
		for(int j = 0; j < globalList.get(0).size(); j++)
		{
			 if(globalList.get(0).get(j).getName().equals("reset_state"))
				 resetName = globalList.get(0).get(j).getValue();
		}
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj o = (GeneralObj) objList.elementAt(i);
			if(o.getType() == 0 || o.getType() == 5)
			{
				StateObj s = (StateObj) o;
				s.updateAttrib(globalList,3);
				if(s.getType() == 0 && s.getName().equals(resetName))
					s.setReset(true);
				else
					s.setReset(false);
			}
		}
		
	}
	
    // update transition attribute lists when global list is updated
	public void updateTrans() {
		syncTransitionOutputDefaultsWithOutputs();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj o = (GeneralObj) objList.elementAt(i);
			if(o.getType() == 1 || o.getType() == 2)
			{
				TransitionObj s = (TransitionObj) o;
				s.updateAttrib(globalList,4);
			}
		}
		
	}

	public void syncTransitionOutputDefaultsWithOutputs()
	{
		for(int i = 0; i < globalList.get(2).size(); i++)
		{
			ObjAttribute output = globalList.get(2).get(i);
			if(!output.getType().equals("regdp"))
				continue;

			ObjAttribute transOutput = findOutputAttribute(globalList.get(4), output.getName());
			if(transOutput != null)
				transOutput.setValue("");
		}
	}

	private void autoAssignTransitionPriorities()
	{
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsBySource = getTransitionsBySource();
		for(Iterator<StateObj> it = transitionsBySource.keySet().iterator(); it.hasNext(); )
		{
			StateObj source = it.next();
			LinkedList<TransitionObj> transitions = transitionsBySource.get(source);
			if(transitions.size() <= 1)
			{
				setTransitionPriorityImplied(transitions.get(0));
				continue;
			}
			sortTransitionsByPriority(transitions);
			for(int i = 0; i < transitions.size(); i++)
				setTransitionPriority(transitions.get(i), Math.min(i, PRIORITY_MAX));
		}
	}

	private void refreshTransitionPriorityHighlights()
	{
		if(objList == null)
			return;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1 || obj.getType() == 2)
				((TransitionObj)obj).setHighestPriority(false);
		}

		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsBySource = getTransitionsBySource();
		for(Iterator<StateObj> it = transitionsBySource.keySet().iterator(); it.hasNext(); )
		{
			StateObj source = it.next();
			LinkedList<TransitionObj> transitions = transitionsBySource.get(source);
			if(transitions.size() <= 1)
				continue;
			sortTransitionsByPriority(transitions);
			transitions.get(0).setHighestPriority(true);
		}
	}

	private LinkedHashMap<StateObj, LinkedList<TransitionObj>> getTransitionsBySource()
	{
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsBySource = new LinkedHashMap<StateObj, LinkedList<TransitionObj>>();
		if(objList == null)
			return transitionsBySource;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 1 && obj.getType() != 2)
				continue;
			TransitionObj trans = (TransitionObj)obj;
			StateObj source = trans.getStartState();
			if(source == null)
				continue;
			LinkedList<TransitionObj> transitions = transitionsBySource.get(source);
			if(transitions == null)
			{
				transitions = new LinkedList<TransitionObj>();
				transitionsBySource.put(source, transitions);
			}
			transitions.add(trans);
		}
		return transitionsBySource;
	}

	private void sortTransitionsByPriority(LinkedList<TransitionObj> transitions)
	{
		Collections.sort(transitions, new Comparator<TransitionObj>() {
			public int compare(TransitionObj a, TransitionObj b)
			{
				int priorityCompare = Double.compare(getTransitionPriority(a), getTransitionPriority(b));
				if(priorityCompare != 0)
					return priorityCompare;
				return objList.indexOf(a) - objList.indexOf(b);
			}
		});
	}

	private double getTransitionPriority(TransitionObj trans)
	{
		ObjAttribute priority = getTransitionPriorityAttribute(trans);
		if(priority == null || priority.getValue() == null)
			return Double.MAX_VALUE;
		try {
			return Double.parseDouble(priority.getValue().trim());
		} catch(NumberFormatException e) {
			return Double.MAX_VALUE;
		}
	}

	private void setTransitionPriority(TransitionObj trans, int priorityValue)
	{
		ObjAttribute priority = getTransitionPriorityAttribute(trans);
		if(priority == null)
			return;
		String value = Integer.toString(priorityValue);
		if(!value.equals(priority.getValue()))
		{
			priority.setValue(value);
			priority.setEditable(1, ObjAttribute.LOCAL);
		}
	}

	private void setTransitionPriorityImplied(TransitionObj trans)
	{
		ObjAttribute priority = getTransitionPriorityAttribute(trans);
		if(priority == null)
			return;
		priority.setValue("1000");
		priority.setEditable(1, ObjAttribute.GLOBAL_VAR);
	}

	private ObjAttribute getTransitionPriorityAttribute(TransitionObj trans)
	{
		LinkedList<ObjAttribute> attrs = trans.getAttributeList();
		if(attrs == null)
			return null;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			if(attr.getName().equals("priority"))
				return attr;
		}
		return null;
	}

	private ObjAttribute findOutputAttribute(LinkedList<ObjAttribute> list, String name)
	{
		for(int i = 0; i < list.size(); i++)
		{
			ObjAttribute attr = list.get(i);
			if(attr.getType().equals("output") && attr.getName().equals(name))
				return attr;
		}
		return null;
	}

	public void renameOutputAttributeEverywhere(String oldName, String newName) {
		if(oldName == null || newName == null || oldName.equals(newName))
			return;

		renameOutputAttributes(globalList.get(3), oldName, newName);
		renameOutputAttributes(globalList.get(4), oldName, newName);

		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.elementAt(i);
			if(obj.getType() == 0 || obj.getType() == 1 || obj.getType() == 2 || obj.getType() == 5)
				renameOutputAttributes(obj.getAttributeList(), oldName, newName);
		}
	}

	private void renameOutputAttributes(LinkedList<ObjAttribute> list, String oldName, String newName) {
		for(int i = 0; i < list.size(); i++)
		{
			ObjAttribute attr = list.get(i);
			if(attr.getType().equals("output") && attr.getName().equals(oldName))
				attr.setName(newName);
		}
	}


	public void editText(TextObj obj)
	{
		//ColorChooserIcon icon = new ColorChooserIcon(obj.getColor(),colorChooser);
		//addMouseListener(icon);
		String s = (String)JOptionPane.showInputDialog(
		        frame,
		        "Edit Text:\n",
		        "Edit Text Properties",
		        JOptionPane.PLAIN_MESSAGE,
		        null,
		        null,
		        obj.getText());
			
		if(s != null)
		{
			obj.setText(s);
			commitUndo();
		}
	}



	public void setJFrame(FizzimGui fizzimGui) {
		frame = fizzimGui;
		
	}
	

	//check for duplicate names
	public boolean checkStateNames()
	{
		TreeSet<String> stateSet = new TreeSet<String>();
		int stateCounter = 0;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0 || obj.getType() == 5)
			{
				stateSet.add(obj.getName());
				stateCounter++;
			}
		}
		if(stateSet.size() == stateCounter)
			return true;
		else
			return false;
	}

	public boolean checkReservedStateNames()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0)
			{
				if(VerilogNameValidator.showReservedWordError(frame, obj.getName(), "state name"))
					return false;
			}
			else if(obj.getType() == 5)
			{
				if(VerilogNameValidator.showReservedWordError(frame, obj.getName(), "state group name"))
					return false;
			}
		}
		return true;
	}

	public boolean validateStateGroupMembership(boolean showMessage)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 0)
				continue;

			StateObj state = (StateObj)obj;
			StateGroupObj containingGroup = null;
			for(int j = 1; j < objList.size(); j++)
			{
				GeneralObj groupObj = (GeneralObj)objList.get(j);
				if(groupObj.getType() != 5)
					continue;

				StateGroupObj stateGroup = (StateGroupObj)groupObj;
				if(stateGroup.containsState(state))
				{
					if(containingGroup != null)
					{
						showStateGroupError(showMessage,
								"State \"" + state.getName() + "\" is fully inside more than one state group.\n"
								+ "Move it so it belongs to only one group.");
						return false;
					}
					containingGroup = stateGroup;
				}
				else if(stateGroup.overlapsState(state))
				{
					showStateGroupError(showMessage,
							"State \"" + state.getName() + "\" partially overlaps state group \""
							+ stateGroup.getName() + "\".\n"
							+ "Move it fully inside the group or fully outside the group.");
					return false;
				}
			}
		}
		return true;
	}

	private void showStateGroupError(boolean showMessage, String message)
	{
		if(showMessage)
		{
			JOptionPane.showMessageDialog(frame,
					message,
					"State Group Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	//check for duplicate names
	public boolean checkTransNames()
	{
		TreeSet<String> transSet = new TreeSet<String>();
		int transCounter = 0;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1 || obj.getType() == 2)
			{
				transSet.add(obj.getName());
				transCounter++;
			}
		}
		if(transSet.size() == transCounter)
			return true;
		else
			return false;
	}

	public boolean checkReservedTransNames()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1 || obj.getType() == 2)
			{
				if(VerilogNameValidator.showReservedWordError(frame, obj.getName(), "transition name"))
					return false;
			}
		}
		return true;
	}
	


	public void save(BufferedWriter writer) throws IOException {
		writer.write("## START PREFERENCES\n");
		writer.write("<SCounter>\n" + createSCounter + "\n</SCounter>\n");
		writer.write("<TCounter>\n" + createTCounter + "\n</TCounter>\n");
		writer.write("<TableVis>\n" + tableVis + "\n</TableVis>\n");
		writer.write("<TableSpace>\n" + space + "\n</TableSpace>\n");
		writer.write("<TableFont>\n" + tableFont.getFamily() + "\n" + tableFont.getSize() + "\n</TableFont>\n");
		writer.write("<TableColor>\n" + tableColor.getRGB() + "\n</TableColor>\n");
		writer.write("<Font>\n" + currFont.getFamily() + "\n" + currFont.getSize() + "\n</Font>\n");
		writer.write("<Grid>\n" + grid + "\n" + gridS + "\n</Grid>\n");
		writer.write("<PageSizeW>\n" + getMaxW() + "\n</PageSizeW>\n");
		writer.write("<PageSizeH>\n" + getMaxH() + "\n</PageSizeH>\n");
		writer.write("<StateW>\n" + getStateW() + "\n</StateW>\n");
		writer.write("<StateH>\n" + getStateH() + "\n</StateH>\n");
		writer.write("<LineWidth>\n" + getLineWidth() + "\n</LineWidth>\n");
		writer.write("## END PREFERENCES\n");
		writer.write("## START OBJECTS\n");

		// tell every object to save itself
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() == 5)
				updateStateGroupChildren((StateGroupObj)obj);
			obj.save(writer);
		}
		writer.write("## END OBJECTS\n");

	}

	private void updateStateGroupChildren(StateGroupObj stateGroup)
	{
		LinkedList<String> children = new LinkedList<String>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() == 0 && stateGroup.containsState((StateObj)obj))
				children.add(obj.getName());
		}
		stateGroup.setChildNames(children);
		if(!children.contains(stateGroup.getEntryState()))
		{
			if(children.size() > 0)
				stateGroup.setEntryState(children.get(0));
			else
				stateGroup.setEntryState("");
		}
	}

	public LinkedList<String> getStateGroupChildNames(StateGroupObj stateGroup)
	{
		updateStateGroupChildren(stateGroup);
		return stateGroup.getChildNames();
	}

	private void updateStateGroupDefaultEntryMarkers()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0)
				((StateObj)obj).setDefaultGroupEntry(false);
		}

		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 5)
				continue;

			StateGroupObj stateGroup = (StateGroupObj)obj;
			updateStateGroupChildren(stateGroup);
			StateObj entryState = getStateObj(stateGroup.getEntryState());
			if(entryState != null && entryState.getType() == 0)
				entryState.setDefaultGroupEntry(true);
		}
	}

	private void updateStateGroupHighlights()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0)
				((StateObj)obj).setStateGroupHighlighted(false);
		}
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 5 || obj.getSelectStatus() == StateObj.NONE)
				continue;
			LinkedList<StateObj> children = getContainedStates((StateGroupObj)obj);
			for(int j = 0; j < children.size(); j++)
				children.get(j).setStateGroupHighlighted(true);
		}
	}

	public void resetTransitionLabelPositions()
	{
		setUndoPointForRouteEdits();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1 || obj.getType() == 2)
			{
				LinkedList<ObjAttribute> attrs = obj.getAttributeList();
				for(int j = 0; j < attrs.size(); j++)
					attrs.get(j).resetTextOffset();
			}
		}
		commitUndo();
		repaint();
	}

	public void cleanTransitionRoutes()
	{
		setUndoPointForRouteEdits();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1)
				((StateTransitionObj)obj).resetRoute();
			else if(obj.getType() == 2)
				((LoopbackTransitionObj)obj).resetRoute();

			if(obj.getType() == 1 || obj.getType() == 2)
			{
				LinkedList<ObjAttribute> attrs = obj.getAttributeList();
				for(int j = 0; j < attrs.size(); j++)
					attrs.get(j).resetTextOffset();
			}
		}
		updateCanvasExtents();
		commitUndo();
		repaint();
	}

	public void cleanSelectedTransitionRoutes()
	{
		LinkedList<Integer> routeIndices = new LinkedList<Integer>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if((obj.getType() == 1 || obj.getType() == 2) && obj.getSelectStatus() != 0)
				routeIndices.add(new Integer(i));
		}
		if(routeIndices.size() == 0)
		{
			cleanTransitionRoutes();
			return;
		}

		setUndoPointForRouteEdits();
		for(int i = 0; i < routeIndices.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(routeIndices.get(i).intValue());
			if(obj.getType() == 1)
				((StateTransitionObj)obj).resetRoute();
			else if(obj.getType() == 2)
				((LoopbackTransitionObj)obj).resetRoute();
			LinkedList<ObjAttribute> attrs = obj.getAttributeList();
			for(int j = 0; j < attrs.size(); j++)
				attrs.get(j).resetTextOffset();
		}
		updateCanvasExtents();
		commitUndo();
		repaint();
	}

	public void alignSelectedCenters(boolean horizontal)
	{
		LinkedList<GeneralObj> endpoints = getSelectedMovableEndpoints();
		if(endpoints.size() < 2)
			return;
		int total = 0;
		for(int i = 0; i < endpoints.size(); i++)
		{
			Point center = endpoints.get(i).getCenter(currPage);
			total += horizontal ? center.y : center.x;
		}
		int target = Math.round((float)total / endpoints.size());
		setUndoPointMultiple();
		LinkedList<GeneralObj> moved = new LinkedList<GeneralObj>();
		for(int i = 0; i < endpoints.size(); i++)
		{
			GeneralObj obj = findObjectByNameAndType(endpoints.get(i).getName(), endpoints.get(i).getType());
			if(obj == null)
				continue;
			Point center = obj.getCenter(currPage);
			moveEndpointAndChildren((StateObj)obj, horizontal ? 0 : target - center.x, horizontal ? target - center.y : 0, moved);
		}
		finishEndpointCleanupMove(moved);
	}

	public void distributeSelectedCenters(boolean horizontal)
	{
		LinkedList<GeneralObj> endpoints = getSelectedMovableEndpoints();
		if(endpoints.size() < 3)
			return;
		Collections.sort(endpoints, new Comparator<GeneralObj>() {
			public int compare(GeneralObj a, GeneralObj b) {
				Point ca = a.getCenter(currPage);
				Point cb = b.getCenter(currPage);
				return horizontal ? ca.x - cb.x : ca.y - cb.y;
			}
		});
		int first = horizontal ? endpoints.getFirst().getCenter(currPage).x : endpoints.getFirst().getCenter(currPage).y;
		int last = horizontal ? endpoints.getLast().getCenter(currPage).x : endpoints.getLast().getCenter(currPage).y;
		if(first == last)
			return;
		setUndoPointMultiple();
		LinkedList<GeneralObj> moved = new LinkedList<GeneralObj>();
		for(int i = 1; i < endpoints.size() - 1; i++)
		{
			int target = first + Math.round((last - first) * ((float)i / (endpoints.size() - 1)));
			GeneralObj obj = findObjectByNameAndType(endpoints.get(i).getName(), endpoints.get(i).getType());
			if(obj == null)
				continue;
			Point center = obj.getCenter(currPage);
			moveEndpointAndChildren((StateObj)obj, horizontal ? target - center.x : 0, horizontal ? 0 : target - center.y, moved);
		}
		finishEndpointCleanupMove(moved);
	}

	private LinkedList<GeneralObj> getSelectedMovableEndpoints()
	{
		refreshSelectedIndicesFromObjects();
		LinkedList<GeneralObj> endpoints = new LinkedList<GeneralObj>();
		LinkedList<StateGroupObj> selectedGroups = getSelectedStateGroups();
		for(int i = 0; i < selectedIndices.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(selectedIndices.get(i).intValue());
			if(!isTransitionEndpoint(obj) || obj.getSelectStatus() != StateObj.CENTER)
				continue;
			if((obj.getType() == 0 || obj.getType() == 4)
					&& isContainedInSelectedStateGroup((StateObj)obj, selectedGroups))
				continue;
			endpoints.add(obj);
		}
		return endpoints;
	}

	private void moveEndpointAndChildren(StateObj obj, int dx, int dy, LinkedList<GeneralObj> moved)
	{
		if(dx == 0 && dy == 0)
			return;
		obj.moveBy(dx, dy);
		moved.add(obj);
		if(obj.getType() == 5)
		{
			LinkedList<StateObj> children = getContainedTransitionEndpoints((StateGroupObj)obj);
			for(int j = 0; j < children.size(); j++)
			{
				children.get(j).moveBy(dx, dy);
				moved.add(children.get(j));
			}
		}
	}

	private void finishEndpointCleanupMove(LinkedList<GeneralObj> moved)
	{
		if(moved.size() == 0)
			return;
		updateTransitionsForMovedEndpoints(moved);
		validateStateGroupMembership(true);
		updateCanvasExtents();
		commitUndo();
		refreshSelectedIndicesFromObjects();
		syncSelectionState();
		repaint();
	}

	public String validateDiagram()
	{
		StringBuffer errors = new StringBuffer();
		appendDuplicateNameErrors(errors);
		appendReservedNameErrors(errors);
		appendResetErrors(errors);
		appendTransitionEndpointErrors(errors);
		appendStateGroupErrors(errors);
		if(errors.length() == 0)
			return "";
		return errors.toString();
	}

	public String lintDiagram()
	{
		lastLintIssues.clear();
		StringBuffer report = new StringBuffer();
		report.append("Fizzim RTL/FSM Lint Report\n");
		report.append("==========================\n\n");
		report.append("This lint pass focuses on FSM intent, generated RTL robustness, and common ASIC signoff risks.\n\n");
		appendStructuralLint(report);
		appendPriorityLint(report);
		appendForkLint(report);
		appendEquationReferenceLint(report);
		appendReachabilityLint(report);
		appendTransitionCoverageLint(report);
		appendOutputAndActionLint(report);
		if(report.indexOf("[ERROR]") < 0 && report.indexOf("[WARN]") < 0)
			report.append("[PASS] No lint issues found. The FSM structure looks ready for backend generation.\n");
		return report.toString();
	}

	public LinkedList<LintIssue> getLastLintIssues()
	{
		return lastLintIssues;
	}

	public void highlightAllLintIssues()
	{
		clearLintHighlights();
		for(int i = 0; i < lastLintIssues.size(); i++)
			highlightLintTarget(lastLintIssues.get(i));
		repaint();
	}

	public void selectLintIssue(LintIssue issue)
	{
		if(issue == null || issue.targetName == null)
			return;
		clearLintHighlights();
		GeneralObj obj = highlightLintTarget(issue);
		if(obj != null)
		{
			unselectObjs();
			if(frame instanceof FizzimGui)
				((FizzimGui)frame).showPage(obj.getPage());
			obj.setSelectStatus(true);
			selectedIndices.clear();
			int index = objList.indexOf(obj);
			if(index >= 0 && isSelectableDiagramObject(obj))
				selectedIndices.add(new Integer(index));
			syncSelectionState();
			Rectangle bounds = getObjectBounds(obj, obj.getPage());
			if(bounds != null)
				scrollRectToVisible(new Rectangle((int)Math.round((bounds.x - originX) * zoom),
						(int)Math.round((bounds.y - originY) * zoom),
						Math.max(40, (int)Math.round(bounds.width * zoom)),
						Math.max(40, (int)Math.round(bounds.height * zoom))));
		}
		repaint();
	}

	public void clearLintHighlights()
	{
		for(int i = 1; i < objList.size(); i++)
			((GeneralObj)objList.get(i)).setLintHighlighted(false);
		repaint();
	}

	private GeneralObj highlightLintTarget(LintIssue issue)
	{
		if(issue == null || issue.targetName == null)
			return null;
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == issue.targetType && obj.getName().equals(issue.targetName))
			{
				obj.setLintHighlighted(true);
				return obj;
			}
		}
		return null;
	}

	private void appendStructuralLint(StringBuffer report)
	{
		String errors = validateDiagram();
		if(!errors.equals(""))
		{
			report.append("Structural Validation\n");
			report.append("---------------------\n");
			String[] lines = errors.split("\\n");
			for(int i = 0; i < lines.length; i++)
			{
				if(lines[i].trim().length() > 0)
					appendLint(report, "ERROR", lines[i].replaceFirst("^-\\s*", ""));
			}
			report.append("\n");
		}
	}

	private void appendPriorityLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsBySource = getTransitionsBySource();
		for(Iterator<StateObj> it = transitionsBySource.keySet().iterator(); it.hasNext(); )
		{
			StateObj source = it.next();
			LinkedList<TransitionObj> transitions = transitionsBySource.get(source);
			if(transitions.size() <= 1)
				continue;
			TreeMap<Integer, String> seen = new TreeMap<Integer, String>();
			for(int i = 0; i < transitions.size(); i++)
			{
				TransitionObj trans = transitions.get(i);
				ObjAttribute priorityAttr = getTransitionPriorityAttribute(trans);
				String priorityText = priorityAttr == null ? "" : priorityAttr.getValue().trim();
				int priority = parsePriority(priorityText);
				if(priority < 0 || priority > PRIORITY_MAX)
				{
					wroteHeader = appendLintHeader(report, wroteHeader, "Priority And Ordering");
					appendLint(report, "ERROR", transitionLabel(trans) + " from " + source.getName()
							+ " has priority \"" + priorityText + "\". Use an integer from 0 to " + PRIORITY_MAX + ".", trans);
				}
				else if(seen.containsKey(new Integer(priority)))
				{
					wroteHeader = appendLintHeader(report, wroteHeader, "Priority And Ordering");
					appendLint(report, "ERROR", transitionLabel(trans) + " and " + seen.get(new Integer(priority))
							+ " both use priority " + priority + " from source " + source.getName() + ".", trans);
				}
				else
				{
					seen.put(new Integer(priority), transitionLabel(trans));
				}
			}
			sortTransitionsByPriority(transitions);
			for(int i = 0; i < transitions.size() - 1; i++)
			{
				if(isDefaultEquation(getTransitionEquation(transitions.get(i))))
				{
					wroteHeader = appendLintHeader(report, wroteHeader, "Priority And Ordering");
					appendLint(report, "ERROR", transitionLabel(transitions.get(i)) + " from " + source.getName()
							+ " is an always-true/default transition above lower-priority transitions. Those lower-priority branches are unreachable.", transitions.get(i));
				}
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private void appendForkLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsBySource = getTransitionsBySource();
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsByDestination = getTransitionsByDestination();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 4)
				continue;
			StateObj fork = (StateObj)obj;
			LinkedList<TransitionObj> incoming = transitionsByDestination.get(fork);
			LinkedList<TransitionObj> outgoing = transitionsBySource.get(fork);
			int incomingCount = incoming == null ? 0 : incoming.size();
			int outgoingCount = outgoing == null ? 0 : outgoing.size();
			if(incomingCount == 0)
			{
				wroteHeader = appendLintHeader(report, wroteHeader, "Fork Coverage");
				appendLint(report, "ERROR", "Fork " + fork.getName() + " has no incoming transition.", fork);
			}
			if(outgoingCount == 0)
			{
				wroteHeader = appendLintHeader(report, wroteHeader, "Fork Coverage");
				appendLint(report, "ERROR", "Fork " + fork.getName() + " has no outgoing transition.", fork);
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private void appendEquationReferenceLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		TreeSet<String> knownSignals = getKnownExpressionNames();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 1 && obj.getType() != 2)
				continue;
			TransitionObj trans = (TransitionObj)obj;
			LinkedList<String> refs = extractExpressionIdentifiers(getTransitionEquation(trans));
			for(int j = 0; j < refs.size(); j++)
			{
				String ref = refs.get(j);
				if(!knownSignals.contains(ref))
				{
					wroteHeader = appendLintHeader(report, wroteHeader, "Equation References");
					appendLint(report, "WARN", transitionLabel(trans) + " equation references \""
							+ ref + "\", which is not declared in the global input/output lists or as a built-in FSM signal.", trans);
				}
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private TreeSet<String> getKnownExpressionNames()
	{
		TreeSet<String> names = new TreeSet<String>();
		for(int i = 0; i < globalList.get(1).size(); i++)
			names.add(baseIdentifier(globalList.get(1).get(i).getName()));
		for(int i = 0; i < globalList.get(2).size(); i++)
			names.add(baseIdentifier(globalList.get(2).get(i).getName()));
		for(int i = 0; i < globalList.get(0).size(); i++)
		{
			ObjAttribute attr = globalList.get(0).get(i);
			String type = attr.getType() == null ? "" : attr.getType().trim().toLowerCase();
			if(type.equals("parameter") || type.equals("define") || type.equals("`define"))
				names.add(baseIdentifier(stripVerilogMacroPrefix(attr.getName())));
		}
		names.add("state");
		names.add("nextstate");
		names.add("statename");
		names.add("default");
		names.add("true");
		names.add("false");
		return names;
	}

	private String baseIdentifier(String name)
	{
		int bracket = name.indexOf("[");
		if(bracket >= 0)
			return name.substring(0, bracket);
		return name;
	}

	private LinkedList<String> extractExpressionIdentifiers(String expression)
	{
		LinkedList<String> ids = new LinkedList<String>();
		if(expression == null)
			return ids;
		String stripped = stripExpressionIgnoredTokens(expression);
		java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[A-Za-z_][A-Za-z0-9_$]*").matcher(stripped);
		while(matcher.find())
		{
			String id = matcher.group();
			String base = baseIdentifier(id);
			if(isExpressionKeyword(base) || ids.contains(base))
				continue;
			ids.add(base);
		}
		return ids;
	}

	private String stripExpressionIgnoredTokens(String expression)
	{
		String stripped = expression;
		stripped = stripped.replaceAll("(?i)(?:\\d+\\s*)?'\\s*[bBoOdDhH]\\s*[0-9a-f_xz?]+", " ");
		stripped = stripped.replaceAll("`[A-Za-z_][A-Za-z0-9_$]*", " ");
		return stripped;
	}

	private String stripVerilogMacroPrefix(String name)
	{
		if(name == null)
			return "";
		String stripped = name.trim();
		if(stripped.startsWith("`"))
			stripped = stripped.substring(1);
		return stripped;
	}

	private boolean isExpressionKeyword(String id)
	{
		return id.equals("if") || id.equals("else") || id.equals("case") || id.equals("endcase")
				|| id.equals("begin") || id.equals("end") || id.equals("and") || id.equals("or")
				|| id.equals("not") || id.equals("posedge") || id.equals("negedge");
	}

	private void appendReachabilityLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		LinkedList<StateObj> states = getRealStates();
		if(states.size() == 0)
			return;

		StateObj reset = getStateObj(getMachineAttributeValue("reset_state"));
		if(reset == null || reset.getType() != 0)
			return;

		HashSet<StateObj> reachable = new HashSet<StateObj>();
		LinkedList<StateObj> queue = new LinkedList<StateObj>();
		reachable.add(reset);
		queue.add(reset);
		while(queue.size() > 0)
		{
			StateObj state = queue.removeFirst();
			LinkedList<StateObj> nextStates = getConcreteDestinationsFromState(state);
			for(int i = 0; i < nextStates.size(); i++)
			{
				StateObj next = nextStates.get(i);
				if(next.getType() == 0 && !reachable.contains(next))
				{
					reachable.add(next);
					queue.add(next);
				}
			}
		}

		for(int i = 0; i < states.size(); i++)
		{
			StateObj state = states.get(i);
			if(!reachable.contains(state))
			{
				wroteHeader = appendLintHeader(report, wroteHeader, "Reachability");
				appendLint(report, "WARN", "State " + state.getName()
						+ " is not reachable from reset_state " + reset.getName() + " through the drawn transitions.", state);
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private void appendTransitionCoverageLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		boolean impliedLoopback = getMachineAttributeValue("implied_loopback").equals("1");
		for(int i = 0; i < getRealStates().size(); i++)
		{
			StateObj state = getRealStates().get(i);
			LinkedList<TransitionObj> outgoing = getEffectiveOutgoingTransitions(state);
			if(outgoing.size() == 0 && !impliedLoopback)
			{
				wroteHeader = appendLintHeader(report, wroteHeader, "State Coverage");
				appendLint(report, "WARN", "State " + state.getName()
						+ " has no explicit outgoing transition and implied_loopback is disabled.", state);
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private void appendOutputAndActionLint(StringBuffer report)
	{
		boolean wroteHeader = false;
		for(int i = 0; i < globalList.get(2).size(); i++)
		{
			ObjAttribute output = globalList.get(2).get(i);
			if((output.getType().equals("reg") || output.getType().equals("regdp"))
					&& output.getresetval().trim().equals(""))
			{
				wroteHeader = appendLintHeader(report, wroteHeader, "Transition Actions");
				appendLint(report, "WARN", "Registered output " + output.getName()
						+ " has no reset value. ASIC FSM outputs should reset deterministically unless intentionally left unreset.");
			}
		}
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 1 && obj.getType() != 2)
				continue;
			LinkedList<ObjAttribute> attrs = obj.getAttributeList();
			for(int j = 0; j < attrs.size(); j++)
			{
				ObjAttribute attr = attrs.get(j);
				if(attr.getType().equals("output") && attr.getEditable(1) == ObjAttribute.LOCAL)
				{
					String value = attr.getValue().trim();
					if(value.equals(""))
						continue;
					if(value.indexOf("<=") >= 0 || value.indexOf("=") >= 0)
					{
						wroteHeader = appendLintHeader(report, wroteHeader, "Transition Actions");
						appendLint(report, "WARN", transitionLabel((TransitionObj)obj) + " transition action for "
								+ attr.getName() + " looks like a full assignment. Enter only the RHS expression.", obj);
					}
				}
			}
		}
		if(wroteHeader)
			report.append("\n");
	}

	private boolean appendLintHeader(StringBuffer report, boolean wroteHeader, String title)
	{
		if(!wroteHeader)
		{
			report.append(title).append("\n");
			for(int i = 0; i < title.length(); i++)
				report.append("-");
			report.append("\n");
		}
		return true;
	}

	private void appendLint(StringBuffer report, String severity, String message)
	{
		appendLint(report, severity, message, null);
	}

	private void appendLint(StringBuffer report, String severity, String message, GeneralObj target)
	{
		lastLintIssues.add(new LintIssue(severity, message, target));
		report.append("[").append(severity).append("] ").append(message).append("\n");
	}

	private String transitionLabel(TransitionObj trans)
	{
		return "Transition " + trans.getName();
	}

	private int parsePriority(String text)
	{
		try {
			return Integer.parseInt(text.trim());
		} catch(Exception e) {
			return -1;
		}
	}

	private String getTransitionEquation(TransitionObj trans)
	{
		ObjAttribute attr = getTransitionAttribute(trans, "equation");
		return attr == null ? "" : attr.getValue().trim();
	}

	private ObjAttribute getTransitionAttribute(TransitionObj trans, String name)
	{
		LinkedList<ObjAttribute> attrs = trans.getAttributeList();
		if(attrs == null)
			return null;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			if(attr.getName().equals(name))
				return attr;
		}
		return null;
	}

	private boolean isDefaultEquation(String equation)
	{
		String normalized = equation == null ? "" : equation.trim().toLowerCase();
		return normalized.equals("1") || normalized.equals("1'b1") || normalized.equals("1'b01")
				|| normalized.equals("true") || normalized.equals("default");
	}

	private LinkedHashMap<StateObj, LinkedList<TransitionObj>> getTransitionsByDestination()
	{
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> transitionsByDestination = new LinkedHashMap<StateObj, LinkedList<TransitionObj>>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 1 && obj.getType() != 2)
				continue;
			TransitionObj trans = (TransitionObj)obj;
			StateObj dest = trans.getEndState();
			if(dest == null)
				dest = trans.getStartState();
			if(dest == null)
				continue;
			LinkedList<TransitionObj> transitions = transitionsByDestination.get(dest);
			if(transitions == null)
			{
				transitions = new LinkedList<TransitionObj>();
				transitionsByDestination.put(dest, transitions);
			}
			transitions.add(trans);
		}
		return transitionsByDestination;
	}

	private LinkedList<StateObj> getRealStates()
	{
		LinkedList<StateObj> states = new LinkedList<StateObj>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0)
				states.add((StateObj)obj);
		}
		return states;
	}

	private LinkedList<TransitionObj> getEffectiveOutgoingTransitions(StateObj state)
	{
		LinkedList<TransitionObj> transitions = new LinkedList<TransitionObj>();
		LinkedHashMap<StateObj, LinkedList<TransitionObj>> bySource = getTransitionsBySource();
		LinkedList<TransitionObj> stateTransitions = bySource.get(state);
		if(stateTransitions != null)
			transitions.addAll(stateTransitions);
		StateGroupObj group = getContainingStateGroup(state);
		if(group != null)
		{
			LinkedList<TransitionObj> groupTransitions = bySource.get(group);
			if(groupTransitions != null)
				transitions.addAll(groupTransitions);
		}
		return transitions;
	}

	private LinkedList<StateObj> getConcreteDestinationsFromState(StateObj state)
	{
		LinkedList<StateObj> destinations = new LinkedList<StateObj>();
		LinkedList<TransitionObj> outgoing = getEffectiveOutgoingTransitions(state);
		for(int i = 0; i < outgoing.size(); i++)
			addConcreteDestinations(destinations, outgoing.get(i).getEndState(), new HashSet<StateObj>());
		return destinations;
	}

	private void addConcreteDestinations(LinkedList<StateObj> destinations, StateObj endpoint, HashSet<StateObj> visitedForks)
	{
		if(endpoint == null)
			return;
		if(endpoint.getType() == 0)
		{
			if(!destinations.contains(endpoint))
				destinations.add(endpoint);
			return;
		}
		if(endpoint.getType() == 5)
		{
			StateGroupObj group = (StateGroupObj)endpoint;
			updateStateGroupChildren(group);
			StateObj entry = getStateObj(group.getEntryState());
			if(entry != null && entry.getType() == 0 && !destinations.contains(entry))
				destinations.add(entry);
			return;
		}
		if(endpoint.getType() == 4)
		{
			if(visitedForks.contains(endpoint))
				return;
			visitedForks.add(endpoint);
			LinkedHashMap<StateObj, LinkedList<TransitionObj>> bySource = getTransitionsBySource();
			LinkedList<TransitionObj> outgoing = bySource.get(endpoint);
			if(outgoing == null)
				return;
			for(int i = 0; i < outgoing.size(); i++)
				addConcreteDestinations(destinations, outgoing.get(i).getEndState(), visitedForks);
		}
	}

	private StateGroupObj getContainingStateGroup(StateObj state)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 5 && ((StateGroupObj)obj).containsState(state))
				return (StateGroupObj)obj;
		}
		return null;
	}

	private void appendDuplicateNameErrors(StringBuffer errors)
	{
		TreeMap<String, Integer> states = new TreeMap<String, Integer>();
		TreeMap<String, Integer> trans = new TreeMap<String, Integer>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0 || obj.getType() == 5)
				incrementNameCount(states, obj.getName());
			else if(obj.getType() == 1 || obj.getType() == 2)
				incrementNameCount(trans, obj.getName());
		}
		appendDuplicateMap(errors, "state/state group", states);
		appendDuplicateMap(errors, "transition", trans);
	}

	private void incrementNameCount(TreeMap<String, Integer> map, String name)
	{
		Integer count = map.get(name);
		map.put(name, count == null ? new Integer(1) : new Integer(count.intValue() + 1));
	}

	private void appendDuplicateMap(StringBuffer errors, String kind, TreeMap<String, Integer> map)
	{
		Iterator<String> it = map.keySet().iterator();
		while(it.hasNext())
		{
			String name = it.next();
			if(map.get(name).intValue() > 1)
				errors.append("- Duplicate ").append(kind).append(" name: ").append(name).append("\n");
		}
	}

	private void appendReservedNameErrors(StringBuffer errors)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0 || obj.getType() == 5 || obj.getType() == 4 || obj.getType() == 1 || obj.getType() == 2)
			{
				String reserved = VerilogNameValidator.reservedWordInIdentifier(obj.getName());
				if(reserved != null)
					errors.append("- Reserved Verilog/SystemVerilog word used by object \"").append(obj.getName()).append("\": ").append(reserved).append("\n");
			}
		}
		String moduleName = getMachineAttributeValue("name");
		String reserved = VerilogNameValidator.reservedWordInIdentifier(moduleName);
		if(reserved != null)
			errors.append("- Reserved Verilog/SystemVerilog word used by module name: ").append(reserved).append("\n");
		for(int i = 0; i < globalList.get(1).size(); i++)
		{
			reserved = VerilogNameValidator.reservedWordInIdentifier(globalList.get(1).get(i).getName());
			if(reserved != null)
				errors.append("- Reserved Verilog/SystemVerilog word used by input: ").append(reserved).append("\n");
		}
		for(int i = 0; i < globalList.get(2).size(); i++)
		{
			reserved = VerilogNameValidator.reservedWordInIdentifier(globalList.get(2).get(i).getName());
			if(reserved != null)
				errors.append("- Reserved Verilog/SystemVerilog word used by output: ").append(reserved).append("\n");
		}
	}

	private void appendResetErrors(StringBuffer errors)
	{
		String resetState = getMachineAttributeValue("reset_state");
		if(resetState.equals(""))
			errors.append("- No reset_state is set.\n");
		else if(getStateObj(resetState) == null)
			errors.append("- reset_state \"").append(resetState).append("\" does not match a state.\n");
		String resetSignal = getMachineAttributeValue("reset_signal");
		if(!resetSignal.equals("") && !globalNameExists(globalList.get(1), resetSignal))
			errors.append("- reset_signal \"").append(resetSignal).append("\" is not listed as an input.\n");
		String clock = getMachineAttributeValue("clock");
		if(clock.equals(""))
			errors.append("- No clock signal is set.\n");
		else if(!globalNameExists(globalList.get(1), clock))
			errors.append("- clock \"").append(clock).append("\" is not listed as an input.\n");
	}

	private String getMachineAttributeValue(String name)
	{
		for(int i = 0; i < globalList.get(0).size(); i++)
		{
			ObjAttribute attr = globalList.get(0).get(i);
			if(attr.getName().equals(name))
				return attr.getValue();
		}
		return "";
	}

	private boolean globalNameExists(LinkedList<ObjAttribute> list, String name)
	{
		for(int i = 0; i < list.size(); i++)
		{
			if(list.get(i).getName().equals(name))
				return true;
		}
		return false;
	}

	private void appendTransitionEndpointErrors(StringBuffer errors)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1)
			{
				StateTransitionObj trans = (StateTransitionObj)obj;
				if(trans.getStartState() == null)
					errors.append("- Transition \"").append(trans.getName()).append("\" is missing a start endpoint.\n");
				if(trans.getEndState() == null)
					errors.append("- Transition \"").append(trans.getName()).append("\" is missing an end endpoint.\n");
			}
			else if(obj.getType() == 2)
			{
				LoopbackTransitionObj trans = (LoopbackTransitionObj)obj;
				if(trans.getStartState() == null)
					errors.append("- Loopback transition \"").append(trans.getName()).append("\" is missing its state endpoint.\n");
			}
		}
	}

	private void appendStateGroupErrors(StringBuffer errors)
	{
		if(!validateStateGroupMembership(false))
			errors.append("- One or more states partially overlap or belong to multiple state groups.\n");
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 5)
			{
				StateGroupObj group = (StateGroupObj)obj;
				updateStateGroupChildren(group);
				if(group.getChildNames().size() == 0)
					errors.append("- State group \"").append(group.getName()).append("\" has no child states.\n");
				else if(group.getEntryState().equals("") || !group.getChildNames().contains(group.getEntryState()))
					errors.append("- State group \"").append(group.getName()).append("\" has no valid default entry state.\n");
			}
		}
	}

	public void delete() {
		
		setUndoPoint(-1,-1);
		//indices of transitions to delete
		LinkedList<Integer> trans = new LinkedList<Integer>();
		// find indices of all transitions to delete
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			// for deleteing multiple selected objects, find transitions
			boolean toDel = false;
			if((obj.getType() == 1 || obj.getType() == 2) && objsSelected)
			{
				for(int j = 0; j < selectedIndices.size(); j++)
				{
					GeneralObj state = (GeneralObj) objList.get(selectedIndices.get(j).intValue());
					if(obj.containsParent(state))
						toDel = true;
				}
			}
			// make sure global table isnt removed
			if(obj.getType() == 3 && objsSelected)
			{
				TextObj txt = (TextObj) obj;
				if(txt.getGlobalTable() && txt.getSelectStatus() != 0)
				{
					String error = "To remove global table, go to 'Settings->View Settings'";
					JOptionPane.showMessageDialog(frame,
	                        error,
	                        "error",
	                        JOptionPane.ERROR_MESSAGE);
				}
				// remove from selected indices
				for(int j = 0; j < selectedIndices.size(); j++)
				{
					Integer tempInt = selectedIndices.get(j);
					if(tempInt.intValue() == i)
					{
						selectedIndices.remove(j);
						break;
					}
				}
			}
			
			
			//for delteing single object, if it is a state remove the transitions
			if(!objsSelected && obj.getSelectStatus() != 0)
			{
				//if transition, just delete
				if((obj.getType() == 1 || obj.getType() == 2))
				{
				objList.remove(i);
					commitUndo();
					break;
				}
				// stop delete of global table, otherwise delete
			
				if(obj.getType() == 3)
				{
					TextObj txt = (TextObj) obj;
					if(txt.getGlobalTable() && txt.getSelectStatus() != 0)
					{
						String error = "To remove global table, go to 'Settings->View Settings'";
						JOptionPane.showMessageDialog(frame,
		                        error,
	                        "error",
		                        JOptionPane.ERROR_MESSAGE);
					}
					else
					{
						objList.remove(i);
						commitUndo();
						break;
					}			
				}
	
			//if state, add transitions to delete
				if(isTransitionEndpoint(obj))
				{
					for(int j = 1; j < objList.size(); j++)
					{
						GeneralObj t = (GeneralObj) objList.elementAt(j);
						if(t.getType() == 1 || t.getType() == 2)
						{
							TransitionObj tran = (TransitionObj) t;
							if(tran.containsParent(obj))
								trans.add(new Integer(j));
						}
					}
					// make sure state gets deleted at correct time
					selectedIndices.add(new Integer(i));
				}
			}
			if(toDel)
				trans.add(new Integer(i));	
			
			
		}
		//delete all selected

	
		while(selectedIndices.size() > 0 || trans.size() > 0)
		{
			int i1 = -1;
			int i2 = -1;
			if(selectedIndices.size() > 0)
				i1 = selectedIndices.get((selectedIndices.size()-1)).intValue();
			if(trans.size() > 0)
				i2 = trans.get((trans.size()-1)).intValue();
			if(i1>i2)
			{
				objList.remove(i1);
				selectedIndices.removeLast();
			}
			else
			{
				objList.remove(i2);
				trans.removeLast();
			}
		}
		
		commitUndo();
		objsSelected = false;
		
		
	}

	

	public static void disableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(false);
	}

	public static void enableDoubleBuffering(Component c) {
		RepaintManager currentManager = RepaintManager.currentManager(c);
		currentManager.setDoubleBufferingEnabled(true);
	}

	public int print(Graphics g, PageFormat pageFormat, int pageIndex) 
	throws PrinterException {
	    if (pageIndex > 0) {
	    	return(NO_SUCH_PAGE);
	    } else {
	      		for (int i = 1; i < objList.size(); i++)
	  		{
	  			GeneralObj s = (GeneralObj) objList.elementAt(i);
	  			s.unselect();				
	  		}
	        Graphics2D g2d = (Graphics2D)g;
	        g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
	        g2d.scale(pageFormat.getImageableWidth()/logicalWidth,pageFormat.getImageableHeight()/logicalHeight);
	       
	        disableDoubleBuffering((Component)this);
	        paintUnscaled(g2d);
	        enableDoubleBuffering((Component)this);
	        return(PAGE_EXISTS);
	    }
	}

	public void open(Vector<Object> objList2) {
		loading = true;
		currPage = 1;
		objList = objList2;
		undoList.clear();
		tempList.clear();
		currUndoIndex = 0;
		undoList.add(objList);
		toCommit = false;
		tempObj = null;
		tempOld = null;
		tempClone = null;
		fileModified = false;
		for (int i = 1; i < objList.size(); i++)
		{
			GeneralObj s = (GeneralObj) objList.elementAt(i);
			if(s.getType() == 1 || s.getType() == 2)
			{
				TransitionObj t = (TransitionObj) s;
				t.makeConnections(objList);
			}
			if(s.getType() == 3)
			{
				TextObj textObj = (TextObj) s;
				if(textObj.getGlobalTable())
				{
					textObj.loadGlobalTable(tableFont);
				}
			}
		}
		updateStates();
		updateTrans(); // Added by pz, but no sure why (initial paint of flags is incorrect without it)
		setGrid(grid,gridS);
		repaint();


	}

	
	public void open(LinkedList<LinkedList<ObjAttribute>> global) {
		loading = true;
		currPage = 1;
		globalList = global;
		objList.clear();
		objList.add(globalList);
		TextObj globalTable = new TextObj(10,10,globalList,tableFont);
		
		objList.add(globalTable);
		undoList.clear();
		undoList.add(objList);
		tempList.clear();
		currUndoIndex = 0;
		toCommit = false;
		tempObj = null;
		tempOld = null;
		tempClone = null;
		fileModified = false;
		createSCounter = 0;
		createTCounter = 0;
		updateStates();
		updateTrans(); // Added by pz, but no sure why (initial paint of flags is incorrect without it)
		repaint();

	}



	public void setSCounter(String readLine) {
		createSCounter = Integer.parseInt(readLine);
		
	}



	public void setTCounter(String readLine) {
		createTCounter = Integer.parseInt(readLine);
		
	}



	public void updateGlobal(LinkedList<LinkedList<ObjAttribute>> globalList2) {
		globalList = globalList2;
		
	}
	
	public LinkedList<LinkedList<ObjAttribute>> getGlobalList()
	{
		syncTransitionOutputDefaultsWithOutputs();
		return globalList;
	}

	public void setFileModifed(boolean b)
	{
		fileModified = b;
		if(b)
			notifyHdlOutOfSync();
		else if(frame instanceof FizzimGui)
			((FizzimGui)frame).updateWindowTitle();
	}

	public void setFileModifiedPreserveHdlStatus(boolean b)
	{
		fileModified = b;
		if(frame instanceof FizzimGui)
			((FizzimGui)frame).updateWindowTitle();
	}
	
	public boolean getFileModifed()
	{
		return fileModified;
	}

	private void notifyHdlOutOfSync()
	{
		if(!loading && frame instanceof FizzimGui)
			((FizzimGui)frame).markHdlOutOfSync();
	}



	public String[] getStateNames() {
		ArrayList<String> names = new ArrayList<String>();
		names.add("null");
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj s = (GeneralObj) objList.get(i);
			if(s.getType() == 0)
				names.add(s.getName());
		}
		String names1[] = names.toArray(new String[names.size()]);
		return names1;
	}
	
	public void setCurrPage(int i)
	{
		currPage = i;
	}
	
	public int getMaxH()
	{
		FizzimGui fgui = (FizzimGui) frame;
		return fgui.getMaxH();
	}
	public int getMaxW()
	{
		FizzimGui fgui = (FizzimGui) frame;
		return fgui.getMaxW();
	}

	public void removePage(int tab) {
		for(int i = objList.size()-1; i > 0; i--)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() != 1)
			{
				if(obj.getPage() == tab)
				{
					objList.remove(i);
				}
				if(obj.getPage() > tab)
					obj.decrementPage();
			}
			else
			{
				StateTransitionObj obj1 = (StateTransitionObj) obj;
				if(obj1.getSPage() == tab || obj1.getEPage() == tab)
					objList.remove(i);
				if(obj1.getSPage() > tab)
					obj1.decrementSPage();
				if(obj1.getEPage() > tab)
					obj1.decrementEPage();
			}
		}
	}

	public void reorderPages(int fromPage, int toPage)
	{
		if(fromPage == toPage || fromPage < 1 || toPage < 1)
			return;

		setUndoPointAllObjects();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 1)
			{
				StateTransitionObj trans = (StateTransitionObj)obj;
				trans.sPage = remapPageIndex(trans.sPage, fromPage, toPage);
				trans.ePage = remapPageIndex(trans.ePage, fromPage, toPage);
				trans.myPage = trans.sPage;
				remapAttributePages(trans, fromPage, toPage);
			}
			else if(obj.getType() == 2)
			{
				obj.myPage = remapPageIndex(obj.myPage, fromPage, toPage);
				remapAttributePages(obj, fromPage, toPage);
			}
			else
			{
				obj.setPage(remapPageIndex(obj.getPage(), fromPage, toPage));
			}
		}
		currPage = remapPageIndex(currPage, fromPage, toPage);
		updateCanvasExtents();
		commitUndo();
	}

	private void remapAttributePages(GeneralObj obj, int fromPage, int toPage)
	{
		LinkedList<ObjAttribute> attrs = obj.getAttributeList();
		if(attrs == null)
			return;
		for(int i = 0; i < attrs.size(); i++)
		{
			ObjAttribute attr = attrs.get(i);
			attr.setPage(remapPageIndex(attr.getPage(), fromPage, toPage));
		}
	}

	private int remapPageIndex(int page, int fromPage, int toPage)
	{
		if(page == fromPage)
			return toPage;
		if(fromPage < toPage && page > fromPage && page <= toPage)
			return page - 1;
		if(toPage < fromPage && page >= toPage && page < fromPage)
			return page + 1;
		return page;
	}

	public void unselectObjs()
	{
		for (int i = 1; i < objList.size(); i++)
		{
			GeneralObj s = (GeneralObj) objList.elementAt(i);
			s.unselect();				
		}
		selectedIndices.clear();
		objsSelected = false;
		multipleSelect = false;
		notifySelectionChanged();
	}

	public void resetUndo() {
		undoList.clear();
		undoList.add(objList);
		tempList.clear();
		currUndoIndex = 0;
		
	}

	public String getPageName(int page) {
		FizzimGui fgui = (FizzimGui) frame;
		return fgui.getPageName(page);
	}

	public void updateGlobalTable() {
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() == 3)
			{
				TextObj textObj = (TextObj) obj;
				if(textObj.getGlobalTable())
					textObj.updateGlobalText(globalList,tableFont,tableVis,space,tableColor);
			}
		}
		repaint();
		
	}
	
	public void setFont(Font font)
	{
		currFont = FizzimFonts.normalizeCodeFont(font, FizzimFonts.CANVAS_FONT_SIZE);
	}
	
	public Font getFont()
	{
		return currFont;
	}
	
	public void setTableFont(Font font)
	{
		tableFont = FizzimFonts.normalizeCodeFont(font, FizzimFonts.CANVAS_FONT_SIZE);

	}
	
	public Font getTableFont()
	{
		return tableFont;
	}
	

	
	public void setSpace(int i)
	{
		space = i;
	}
	
	public int getSpace()
	{
		return space;
	}
	
	public boolean getTableVis()
	{
		return tableVis;
	}
	
	public void setTableVis(boolean b)
	{
		tableVis = b;
	}
	
	public Color getTableColor()
	{
		return tableColor;
	}
	
	public void setTableColor(Color c)
	{
		tableColor = c;
	}
	
	public void setGrid(boolean b, int i)
	{
		grid = b;
		gridS = i;
		for(int j = 1; j < objList.size(); j++)
		{
			GeneralObj obj = (GeneralObj) objList.get(j);
			if(isTransitionEndpoint(obj))
			{
				StateObj obj1 = (StateObj) obj;
				obj1.setGrid(b,i);
			}
		}
	}
	
	public boolean getGrid()
	{
		return grid;
	}
	
	public int getGridSpace()
	{
		return gridS;
	}

	//generate pixel offset for page connectors
	public int getOffset(int page, StateObj startState, StateTransitionObj transObj, String type) {
		int totalNumb = 0;
		int numb = 0;
		for(int i = 1; i < objList.size(); i++)
		{
			
			GeneralObj obj = (GeneralObj) objList.get(i);
			// find number that transition is out of total number
			if(obj.getType() == 1)
			{
				StateTransitionObj trans = (StateTransitionObj) obj;
				if(trans.pageConnectorExists(page,startState,type))
				{
					totalNumb++;
					if(trans.equals(transObj))
						numb = totalNumb;
					
				}
			}
		}
		//find number for cener position
		int avg;
		if(totalNumb % 2 != 0)
			avg = (int)((totalNumb+1)/2);
		else
			avg = (int)(totalNumb/2);
		int finalOffset = (numb-avg)*40;
		return finalOffset;
	}

	public int getDistributedBorderIndex(StateObj state, StateTransitionObj transObj, String type,
			int baseIndex, Vector<Point> borderPts)
	{
		if(state == null || transObj == null || borderPts == null || borderPts.size() == 0
				|| transObj.getStub())
			return baseIndex;

		StateObj otherState = getConnectedTransitionState(transObj, state);
		if(otherState == null || otherState.getPage() != state.getPage())
			return baseIndex;

		int side = sideFromTarget(state, otherState.getRealCenter(state.getPage()));
		LinkedList<TransitionSlot> slots = new LinkedList<TransitionSlot>();
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() != 1)
				continue;

			StateTransitionObj trans = (StateTransitionObj)obj;
			if(trans.getStub())
				continue;

			StateObj candidateOther = getConnectedTransitionState(trans, state);
			if(candidateOther == null || candidateOther.getPage() != state.getPage())
				continue;
			if(sideFromTarget(state, candidateOther.getRealCenter(state.getPage())) != side)
				continue;

			Point candidateCenter = candidateOther.getRealCenter(state.getPage());
			slots.add(new TransitionSlot(trans, i, edgeSortValue(side, candidateCenter)));
		}

		if(slots.size() <= 1)
			return baseIndex;

		Collections.sort(slots, new Comparator<TransitionSlot>() {
			public int compare(TransitionSlot a, TransitionSlot b) {
				if(a.sortValue != b.sortValue)
					return a.sortValue - b.sortValue;
				return a.objectIndex - b.objectIndex;
			}
		});

		int slotIndex = -1;
		for(int i = 0; i < slots.size(); i++)
		{
			if(slots.get(i).transition == transObj)
			{
				slotIndex = i;
				break;
			}
		}
		if(slotIndex < 0)
			return baseIndex;

		return borderIndexForSlot(state, side, borderPts, slotIndex, slots.size(), baseIndex);
	}

	private StateObj getConnectedTransitionState(StateTransitionObj trans, StateObj state)
	{
		if(trans.getStartState() == state && trans.getStartState().getPage() == trans.getEndState().getPage())
			return trans.getEndState();
		if(trans.getEndState() == state && trans.getStartState().getPage() == trans.getEndState().getPage())
			return trans.getStartState();
		return null;
	}

	private int sideFromTarget(StateObj state, Point target)
	{
		Point center = state.getRealCenter(state.getPage());
		int dx = target.x - center.x;
		int dy = target.y - center.y;
		if(Math.abs(dx) >= Math.abs(dy))
			return dx >= 0 ? 1 : 3;
		return dy >= 0 ? 2 : 0;
	}

	private int edgeSide(StateObj state, Point borderPt)
	{
		int[] coords = state.getCoords();
		int left = coords[0];
		int top = coords[1];
		int right = coords[2];
		int bottom = coords[3];
		int dl = Math.abs(borderPt.x - left);
		int dr = Math.abs(borderPt.x - right);
		int dt = Math.abs(borderPt.y - top);
		int db = Math.abs(borderPt.y - bottom);
		int min = Math.min(Math.min(dl, dr), Math.min(dt, db));
		if(min == dt)
			return 0;
		if(min == dr)
			return 1;
		if(min == db)
			return 2;
		return 3;
	}

	private int edgeSortValue(int side, Point point)
	{
		if(side == 1 || side == 3)
			return point.y;
		return point.x;
	}

	private int borderIndexForSlot(StateObj state, int side, Vector<Point> borderPts,
			int slotIndex, int slotCount, int fallbackIndex)
	{
		LinkedList<Integer> sideIndices = new LinkedList<Integer>();
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		for(int i = 0; i < borderPts.size(); i++)
		{
			Point point = borderPts.get(i);
			if(edgeSide(state, point) != side)
				continue;
			int value = edgeSortValue(side, point);
			min = Math.min(min, value);
			max = Math.max(max, value);
			sideIndices.add(new Integer(i));
		}
		if(sideIndices.size() == 0 || max <= min)
			return fallbackIndex;

		double fraction = (double)(slotIndex + 1) / (slotCount + 1);
		int target = (int)Math.round(min + (max - min) * fraction);
		int bestIndex = fallbackIndex;
		int bestDistance = Integer.MAX_VALUE;
		for(int i = 0; i < sideIndices.size(); i++)
		{
			int index = sideIndices.get(i).intValue();
			int distance = Math.abs(edgeSortValue(side, borderPts.get(index)) - target);
			if(distance < bestDistance)
			{
				bestDistance = distance;
				bestIndex = index;
			}
		}
		return bestIndex;
	}

	private static class TransitionSlot
	{
		StateTransitionObj transition;
		int objectIndex;
		int sortValue;

		TransitionSlot(StateTransitionObj transition, int objectIndex, int sortValue)
		{
			this.transition = transition;
			this.objectIndex = objectIndex;
			this.sortValue = sortValue;
		}
	}

	public void pageConnUpdate(StateObj startState, StateObj endState) {
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() == 1)
			{
				StateTransitionObj trans = (StateTransitionObj) obj;
				if(trans.pageConnectorExists(startState.getPage(),startState,"start") ||
						trans.pageConnectorExists(endState.getPage(),endState,"end"))
				{
					trans.setEndPts();
					
				
					
				}
			}
		}
	}
	
	//for redrawing all page connectors when page is resized
	public void updatePageConn()
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			if(obj.getType() == 1)
			{
				StateTransitionObj trans = (StateTransitionObj) obj;
				if(trans.getSPage() != trans.getEPage())
					trans.setEndPts();
			}
		}
	}

	public void moveOnResize(int maxW, int maxH)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj) objList.get(i);
			//move text objects onto page
			if(obj.getType() == 3)
			{
				TextObj text = (TextObj) obj;
				text.moveIfNeeded(maxW,maxH);
			}
			if(isTransitionEndpoint(obj))
			{
				StateObj state = (StateObj) obj;
				state.moveIfNeeded(maxW,maxH);
			}
		}
	}

	public Color getDefSC() {
		return defSC;
	}

	public void setDefSC(Color defSC) {
		this.defSC = defSC;
	}

	public Color getDefSTC() {
		return defSTC;
	}

	public void setDefSTC(Color defSTC) {
		this.defSTC = defSTC;
	}

	public Color getDefLTC() {
		return defLTC;
	}

	public void setDefLTC(Color defLTC) {
		this.defLTC = defLTC;
	}
	
	public JColorChooser getColorChooser()
	{
		return colorChooser;
	}

	public int getStateW()
	{
		return StateW;
	}
	
	public int getStateH()
	{
		return StateH;
	}
	
	public int getLineWidth()
	{
		return LineWidth;
	}
	
	public void setStateW(int w)
	{
		StateW = w;
	}
	
	public void setStateH(int h)
	{
		StateH = h;
	}

	public void setLineWidth(int w)
	{
		LineWidth = w;
	}
	
	
	public boolean getRedraw()
	{
		return Redraw;
	}
	

}

/* class ColorChooserIcon implements Icon, MouseListener {
	  
	  private Color color;
	  private JColorChooser colorChooser;

	  public ColorChooserIcon(Color color, JColorChooser colorChooser) {
		  this.color = color;
		  this.colorChooser = colorChooser;
	}

	public void paintIcon(Component c, Graphics g, int x, int y) {
	    Color old_color = g.getColor();
	    g.setColor(color);
	    g.fillRect(x,y,15,15);
	    g.setColor(old_color);
	  }

	  public int getIconWidth() {
	    return 15;
	  }

	  public int getIconHeight() {
	    return 15;
	  }

	public void mouseClicked(MouseEvent e) {
		System.out.println("test");
		color = JColorChooser.showDialog(null, "Choose Color", color);
	}

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}


	  
	}
*/
