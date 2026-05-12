import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

//Written by: Michael Zimmer - mike@zimmerdesignservices.com

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

public class TextObj extends GeneralObj {
	private static final String HDL_STATE_ATTR = "fizzim2_hdl_generated";
	private static final String HDL_OUTPUT_ATTR = "fizzim2_hdl_output";

	private int selectStatus = 0;
	private int tX,tY,tW,tH,xTemp,yTemp;
        private int selectboxLeft,selectboxRight,selectboxBottom,selectboxTop;
	private String text = null;
	private GeneralObj connectedObj = null;
	private boolean parentSelected = false;
	LinkedList<LinkedList<ObjAttribute>> globalList = null;
	
	private boolean globalLoad = true;
	private boolean globalTable = false;
	private boolean tableVis = true;
	private int space = 20;
	private Color tableColor;
	
	private Color color = Color.black;

	Font tableFont;
	
	FontMetrics fm;
	
	LinkedList<String> col1 = new LinkedList<String>();
	LinkedList<String> col2 = new LinkedList<String>();
	LinkedList<String> col3 = new LinkedList<String>();
	LinkedList<String> col4 = new LinkedList<String>();
	LinkedList<Integer> rowInterfaceTabs = new LinkedList<Integer>();
	int col1W =0, col2W = 0, col3W = 0, col4W = 0;

	
	

	
	public TextObj(int x, int y, LinkedList<LinkedList<ObjAttribute>> global, Font font)
	{
		selectStatus = 0;
		tX = x;
		tY = y;
		myPage = 1;
		globalTable = true;
		tableFont = font;
		globalList = global;

		
	}
	
	public TextObj(int x, int y, LinkedList<LinkedList<ObjAttribute>> global, int p)
	{
		selectStatus = 0;
		tX = x;
		tY = y;
		myPage = p;
		globalList = global;
		globalTable = true;
		
		
	}



	public TextObj(String s, int x, int y, int page)
	{

		selectStatus = 0;
		text = s;
		tX = x;
		tY = y;
		myPage = page;

		
	}
	
	
	public void loadGlobalTable(Font font)
	{
		tableFont = font;
	}
	
	public void updateTableFont(Font font)
	{
		tableFont = font;
	}
	

	public void updateGlobalText(LinkedList<LinkedList<ObjAttribute>> global, Font font, boolean b, int s, Color c) {

		tableFont = font;
		tableVis = b;
		space = s;
		tableColor = c;
		
		globalList = global;
		col1.clear();
		col2.clear();
		col3.clear();
		col4.clear();
		rowInterfaceTabs.clear();
		
		
		//add titles
		col1W = 0;
		col2W = 0;
		col3W = 0;
		col4W = 0;
		
		for(int i = 0; i < globalList.size(); i++)
		{
			if(i == 3 || i == 4)
				continue;
			if(i >= 3 && globalList.get(i).size() < 2)
				continue;
			else if(i < 3 && globalList.get(i).size() < 1)
				continue;
			switch(i) {
				case 0: addGlobalSummaryRow("STATE MACHINE", " ", " ", " ", 0); break;
				case 1: addGlobalSummaryRow("INPUTS", " ", " ", " ", 1); break;
				case 2: addGlobalSummaryRow("OUTPUTS", " ", " ", " ", 2); break;
				case 3: addGlobalSummaryRow("STATES", " ", " ", " ", 3); break;
				case 4: addGlobalSummaryRow("TRANSITIONS", " ", " ", " ", 4); break;
			}
			if(i == 2)
			{
				addOutputSummaryRows(false);
				addOutputSummaryRows(true);
				continue;
			}
			for(int j = 0; j < globalList.get(i).size(); j++)
			{
				//skip "name" for state and transition
				if((i == 3 || i == 4) && j == 0)
					continue;
				ObjAttribute obj = globalList.get(i).get(j);
				if(i == 0 && obj.getType().equals("parameter"))
					continue;
				if(isHiddenSummaryAttribute(obj))
					continue;
				String name = "   " + obj.getName();

				if(col1W < fm.stringWidth(name))
					col1W = fm.stringWidth(name);
				col1.add(name);
				String value = obj.getValue();
				if(col2W < fm.stringWidth(value))
					col2W = fm.stringWidth(value);
				col2.add(value);
				String type = obj.getType();
                                // rename "reg" to "statebit" in the attributes table
                                if (type.equals("reg")) type = "statebit";
				if(col3W < fm.stringWidth(type))
					col3W = fm.stringWidth(type);
				col3.add(type);
				String comm = obj.getComment();
				if(col4W < fm.stringWidth(comm))
					col4W = fm.stringWidth(comm);
				col4.add(comm);
			}
			if(i == 0)
			{
				addGlobalSummaryRow("   HDL output", resolveHdlOutputSummary(), "", "");
				addParameterSummaryRows();
			}
		}

		col1W += space;
		col2W += space;
		col3W += space;
		col4W += space;
	}

	private void addOutputSummaryRows(boolean internals)
	{
		boolean wroteHeader = !internals;
		if(internals)
		{
			for(int j = 0; j < globalList.get(2).size(); j++)
			{
				if(isInternalOutput(globalList.get(2).get(j)))
				{
					addGlobalSummaryRow("INTERNALS", " ", " ", " ", 5);
					wroteHeader = true;
					break;
				}
			}
		}
		if(!wroteHeader)
			return;
		for(int j = 0; j < globalList.get(2).size(); j++)
		{
			ObjAttribute obj = globalList.get(2).get(j);
			if(isInternalOutput(obj) != internals)
				continue;
			String name = "   " + obj.getName();
			String type = obj.getType();
			if(type.equals("reg"))
				type = "statebit";
			addGlobalSummaryRow(name, obj.getValue(), type, obj.getComment());
		}
	}

	private boolean isInternalOutput(ObjAttribute obj)
	{
		String userAtts = obj.getUserAtts();
		if(userAtts == null)
			return false;
		String[] tokens = userAtts.split("[,;\\s]+");
		for(int i = 0; i < tokens.length; i++)
			if(tokens[i].equals("suppress_portlist"))
				return true;
		return false;
	}

	private boolean isHiddenSummaryAttribute(ObjAttribute obj)
	{
		return obj.getName().equals(HDL_STATE_ATTR) || obj.getName().equals(HDL_OUTPUT_ATTR);
	}

	private void addParameterSummaryRows()
	{
		if(globalList == null || globalList.size() == 0)
			return;
		boolean wroteHeader = false;
		LinkedList<ObjAttribute> machine = globalList.get(0);
		for(int i = 0; i < machine.size(); i++)
		{
			ObjAttribute obj = machine.get(i);
			if(!obj.getType().equals("parameter"))
				continue;
			if(!wroteHeader)
			{
				if(col1W < fm.stringWidth("PARAMETERS"))
					col1W = fm.stringWidth("PARAMETERS");
				addGlobalSummaryRow("PARAMETERS", " ", " ", " ", 6);
				wroteHeader = true;
			}
			addGlobalSummaryRow("   " + obj.getName(), obj.getValue(), obj.getType(), obj.getComment());
		}
	}

	private void addGlobalSummaryRow(String name, String value, String type, String comment)
	{
		int tab = rowInterfaceTabs.size() > 0 ? rowInterfaceTabs.getLast().intValue() : 0;
		addGlobalSummaryRow(name, value, type, comment, tab);
	}

	private void addGlobalSummaryRow(String name, String value, String type, String comment, int tab)
	{
		if(col1W < fm.stringWidth(name))
			col1W = fm.stringWidth(name);
		col1.add(name);
		if(col2W < fm.stringWidth(value))
			col2W = fm.stringWidth(value);
		col2.add(value);
		if(col3W < fm.stringWidth(type))
			col3W = fm.stringWidth(type);
		col3.add(type);
		if(col4W < fm.stringWidth(comment))
			col4W = fm.stringWidth(comment);
		col4.add(comment);
		rowInterfaceTabs.add(new Integer(tab));
	}

	private String resolveHdlOutputSummary()
	{
		String generatedPath = getMachineValue(HDL_OUTPUT_ATTR);
		if(generatedPath != null && !generatedPath.trim().equals(""))
			return generatedPath.trim();
		String moduleName = getMachineValue("name");
		if(moduleName == null || moduleName.trim().equals(""))
			moduleName = "fsm";
		return sanitizeHdlFilename(moduleName) + ".v";
	}

	private String getMachineValue(String name)
	{
		if(globalList == null || globalList.size() == 0)
			return "";
		LinkedList<ObjAttribute> machine = globalList.get(0);
		for(int i = 0; i < machine.size(); i++)
		{
			ObjAttribute attr = machine.get(i);
			if(attr.getName().equals(name))
				return attr.getValue();
		}
		return "";
	}

	private String sanitizeHdlFilename(String name)
	{
		if(name == null || name.trim().equals(""))
			return "fsm";
		return name.trim().replaceAll("[^A-Za-z0-9_.$-]", "_");
	}

	public void prepareGlobalBounds(FontMetrics metrics, LinkedList<LinkedList<ObjAttribute>> global, Font font, boolean b, int s, Color c) {
		fm = metrics;
		updateGlobalText(global, font, b, s, c);
		tH = col1.size() * fm.getHeight();
		tW = col1W + col2W + col3W + col4W;
	}
	public boolean getGlobalTable()
	{
		return globalTable;
	}
	


	public String getText()
	{
		return text;
	}
	
	public void setText(String str)
	{
		text = str;
	}

	public void moveBy(int dx, int dy)
	{
		tX += dx;
		tY += dy;
		modified = true;
	}
	
	@Override
	public void adjustShapeOrPosition(int x, int y) {
		if(myPage == currPage)
		{
			if(selectStatus == 1)
			{
				tX += x - xTemp;
				tY += y - yTemp;
	
				xTemp = x;
				yTemp = y;
			}
			
			modified = true;
		}

	}


	
	public int getSelectStatus() {
		
		return selectStatus;
	}

	
	public int getType() {
		return 3;
	}

	
	public boolean isModified() {
		return modified;
	}

	
	public boolean isParentModified() {
		if(modifiedParent)
			return true;
		else
			return false;
	}


	public void paintComponent(Graphics g) {
		
		// need to set font metrics, regardless of whether the text list is on current page
		if(globalTable && tableVis)
		{
			Font tempFont = g.getFont();			
			g.setFont(tableFont);
			fm = g.getFontMetrics();
			
			
			if(globalLoad)
			{
				updateGlobalText(globalList,tableFont,tableVis,space,tableColor);
				globalLoad = false;
				
			}
			if(myPage == currPage)
			{
				g.setColor(tableColor);
				for(int i = 0; i < col1.size(); i++)
				{
					g.drawString(col1.get(i),tX,tY+i*fm.getHeight());
					g.drawString(col2.get(i),tX+col1W,tY+i*fm.getHeight());
					g.drawString(col3.get(i),tX+col1W+col2W,tY+i*fm.getHeight());
					g.drawString(col4.get(i),tX+col1W+col2W+col3W,tY+i*fm.getHeight());
				}
				tH = col1.size()*fm.getHeight();
				tW = col1W+col2W+col3W+col4W;
			}
			g.setFont(tempFont);
		}
		
		if(myPage == currPage)
		{
                        int x2Obj = 0; // to make it match ObjAttribute code
                        int y2Obj = 0; // to make it match ObjAttribute code
                        int txbase = 0;
                        int tybase = 0;
                        int yoffset = 0;

			g.setColor(Color.black);
			FontMetrics fm1 = g.getFontMetrics();

			
			if (!globalTable) {
                          tH = fm1.getHeight();
                          tW = fm1.stringWidth(text);
                          txbase = tX+x2Obj;
                          tybase = tY+y2Obj;
                          
                          if(text.indexOf("\\n")==-1) {
                            g.drawString(text,txbase,tybase);
                          } else {
                            /* split lines on \n */
                            tW = 0;
                            String tempText = text;
                          
                            while(tempText.indexOf("\\n")>-1) {
                              String line = tempText.substring(0,tempText.indexOf("\\n"));
                              g.drawString(line,txbase,tybase+yoffset);
                              if(fm1.stringWidth(line)>tW)
                              tW = fm1.stringWidth(line);
                              tempText = tempText.substring(tempText.indexOf("\\n")+2);
                              yoffset += tH;
                            }
                            g.drawString(tempText,txbase,tybase+yoffset);
                            if(fm1.stringWidth(tempText)>tW)
                              tW = fm1.stringWidth(tempText);
                          }
                          
			}

                    // Since box corners are needed by setSelectStatus, set
                    // them here.
                    selectboxLeft = txbase-4;
                    selectboxRight = txbase+tW+3;
                    selectboxBottom = tybase+yoffset+4;
                    selectboxTop = tybase-tH+2;
  
                  //if object is selected, draw red selection box around it
                  if(selectStatus != 0 || parentSelected) {
                    g.setColor(Color.red);

	            if(!globalTable) {
                      g.drawLine(selectboxLeft,selectboxTop,selectboxRight,selectboxTop);  // Top
                      g.drawLine(selectboxLeft,selectboxBottom,selectboxRight,selectboxBottom);  // Bottom
                      g.drawLine(selectboxLeft,selectboxTop,selectboxLeft,selectboxBottom);  // Left
                      g.drawLine(selectboxRight,selectboxTop,selectboxRight,selectboxBottom);  // Right

                    } else if (tableVis) {           
                      g.drawLine(tX-4,tY+4+tH-(tH/col1.size()),tX+tW+3,tY+4+tH-(tH/col1.size()));
                      g.drawLine(tX+tW+3,tY+4+tH-(tH/col1.size()),tX+tW+3,tY-(tH/col1.size())+2);
                      g.drawLine(tX+tW+3,tY-(tH/col1.size())+2,tX-4,tY-(tH/col1.size())+2);
                      g.drawLine(tX-4,tY+4+tH-(tH/col1.size()),tX-4,tY-(tH/col1.size())+2); 
                   }

		}
              }
	}
	
	public boolean setSelectStatus(int x, int y) {
		if(myPage == currPage)
		{
			xTemp = x;
			yTemp = y;
			selectStatus = 0;

			// check if inside square
			if(globalTable)
			{
				Rectangle bounds = getBounds();
				if(tableVis && bounds.contains(x, y))
					selectStatus = 1;
			}
			else if(x >= selectboxLeft && x <= selectboxRight && y >= selectboxTop && y <= selectboxBottom) {
				selectStatus = 1;
			}

			if(selectStatus == 0)
				return false;
			else
				return true;
		}
		else
			return false;

	}


	public void unselect() {
		selectStatus = 0;
	}

	
	public Point getCenter(int page)
	{
		return new Point(tX +(tW/2), tY + (tH/2));
	}

	public Rectangle getBounds()
	{
		if(globalTable && col1.size() > 0)
		{
			int lineHeight = Math.max(1, tH / col1.size());
			return new Rectangle(tX - 8, tY - lineHeight, Math.max(16, tW + 16), Math.max(24, tH + 12));
		}
		return new Rectangle(tX - 8, tY - Math.max(20, tH), Math.max(16, tW + 16), Math.max(24, tH + 12));
	}

	public int getGlobalInterfaceTabAt(int y)
	{
		if(!globalTable || !tableVis || col1.size() == 0)
			return -1;
		int lineHeight = Math.max(1, tH / col1.size());
		int top = tY - lineHeight + 2;
		int row = (y - top) / lineHeight;
		if(row < 0 || row >= rowInterfaceTabs.size())
			return -1;
		return rowInterfaceTabs.get(row).intValue();
	}



	@Override
	public Point getStart() {
		// TODO better
		Point pt = new Point(tX, tY+20);
		return pt;
	}
	

	//tX,tY,x2Obj,y2Obj,text
	@Override
	public void save(BufferedWriter writer) throws IOException {
		writer.write("<textObj>\n");
		if(text != null)
			writer.write(text + "\n");
		else
			writer.write("fzm_globalTable\n");
		writer.write(i(1) + "<x>\n" + i(1) + tX + "\n" + i(1) + "</x>\n");
		writer.write(i(1) + "<y>\n" + i(1) + tY + "\n" + i(1) + "</y>\n");
		writer.write(i(1) + "<page>\n" + i(1) + myPage + "\n" + i(1) + "</page>\n");
		writer.write("</textObj>\n");
	}


	public void paintComponent(Graphics g, int i) {
		currPage = i;
		paintComponent(g);
	}

	@Override
	public boolean containsParent(GeneralObj oldObj) {
		return false;
	}

	@Override
	public void notifyChange(GeneralObj oldObj, GeneralObj clonedObj) {
		
	}

	@Override
	public void updateObj() {
		
	}

	public void moveIfNeeded(int maxW, int maxH) {
		if(tX > maxW)
			tX = maxW-tW;
		if(tY > maxH)
			tY = maxH-tH;
		
	}

	@Override
	public boolean setBoxSelectStatus(int x0, int y0, int x1, int y1) {
		selectStatus = 0;
		if(myPage == currPage && x0 <= tX-4 && x1 >= tX+tW+3)
		{

			if(!globalTable && y0 <= tY-tH+2 && y1 >= tY+4)
			{
				selectStatus = 1;
				return true;	
			}
			else if(globalTable && tableVis && y0 <= tY-(tH/col1.size())+2 && y1 >= tY+4+tH-(tH/col1.size()))
			{
				selectStatus = 1;
				return true;	
			}
			return false;
		}			
		else
			return false;
	}
	
	public boolean setBoxSelectStatus(int x, int y)
	{
		xTemp = x;
		yTemp = y;
		if(myPage == currPage && x >= tX-4 && x <= tX+tW+3)
		{
			
			if(!globalTable && y >= tY-tH+2 && y <= tY+4)
				return true;			
			else if(globalTable && tableVis && y >= tY-(tH/col1.size())+2 && y <= tY+4+tH-(tH/col1.size()))
				return true;
			return false;
		}			
		else
			return false;
	}
	
	public void setSelectStatus(boolean b) {
		if(b)
			selectStatus = 1;
		else
			selectStatus = 0;
		
	}

	public Color getColor() {
		return color;
	}
	


}
