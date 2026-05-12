import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

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

/* all the methods are dependent on the format of the saved file
 * not being changed.  Comments can be added to the saved file with ##
 * This file could be modified to make file opening much more robust.
 */

public class FileParser {
	
	private File file;
	private FizzimGui fizzim;
	ArrayList<String> tempList;
	ArrayList<String> tempList2;
	LinkedList<ObjAttribute> globalMachineAttributes;
	LinkedList<ObjAttribute> globalStateAttributes;
	LinkedList<ObjAttribute> globalTransAttributes;
	LinkedList<ObjAttribute> globalInputsAttributes;
	LinkedList<ObjAttribute> globalOutputsAttributes;
	LinkedList<LinkedList<ObjAttribute>> globalList;
	DrawArea drawArea;
	Vector<Object> objList;
;
	private int ver = 0;

	
	public FileParser(File _file, FizzimGui _fizzim, DrawArea _drawArea)
	{
		file = _file;
		fizzim = _fizzim;
		drawArea = _drawArea;
		tempList = new ArrayList<String>();
		objList = new Vector<Object>();
		parse();
	}
	


	private void parse() {
		FileReader fileReader;
		
		try {
			fileReader = new FileReader(file);
			BufferedReader reader = new BufferedReader(fileReader) {
				public String readLine() throws IOException
				{
					//parse out indents
					String line = super.readLine();
					while(line != null && line.length() > 0 && line.charAt(0) == ' ')
						line = line.substring(1);
					return line;
					
				}};
			
			String line = null;
			while((line = reader.readLine()) != null)
			{
				// ignore comments
				if(line.startsWith("##"))
					continue;
			
				
				
				else if(line.equals("<version>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();

					String temp = line2;
					temp = temp.replaceAll("\\.", "");
					ver = Integer.parseInt(temp);
				}
				
				//templist holds related chunks of lines
				else if(line.equals("<globals>"))
				{
					while((line = reader.readLine()) != null && !line.equals("</globals>"))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openGlobal(tempList);
				}
				else if(line.equals("<SCounter>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					drawArea.setSCounter(line2);
				}
				else if(line.equals("<TCounter>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					drawArea.setTCounter(line2);
				}
				else if(line.equals("<TableVis>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					if(line2.equals("false"))
						drawArea.setTableVis(false);
					else
						drawArea.setTableVis(true);
				}
				else if(line.equals("<TableSpace>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					try {	
						drawArea.setSpace(Integer.parseInt(line2));
					}
					catch (NumberFormatException nfe) {
						drawArea.setSpace(20);
					}
				}
				else if(line.equals("<TableFont>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					
					try {
						Font newFont = new Font(line2,Font.PLAIN,Integer.parseInt(line3));
						drawArea.setTableFont(newFont);
					} catch (NumberFormatException nfe) {}
				}
				else if(line.equals("<TableColor>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					try {	
						drawArea.setTableColor(new Color(Integer.parseInt(line2)));
					}
					catch (NumberFormatException nfe) {
						drawArea.setTableColor(Color.black);
					}
				}
				else if(line.equals("<Font>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					
					try {
						Font newFont = new Font(line2,Font.PLAIN,Integer.parseInt(line3));
						drawArea.setFont(newFont);
					} catch (NumberFormatException nfe) {}
				}
				else if(line.equals("<Grid>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					
					boolean grid = false;
					if(line2.equals("true"))
						grid = true;
					try {	
						drawArea.setGrid(grid,Integer.parseInt(line3));
					}
					catch (NumberFormatException nfe) {
						drawArea.setGrid(grid,25);
					}
				}
				
				else if(line.equals("<PageSizeW>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					

					try {
						fizzim.setDASize(Integer.parseInt(line2), fizzim.getMaxH());
					}
					catch (NumberFormatException nfe) { }
				}
				
				
				else if(line.equals("<PageSizeH>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					

					try {
						fizzim.setDASize(fizzim.getMaxW(),Integer.parseInt(line2));
					}
					catch (NumberFormatException nfe) { }
				}
				
				else if(line.equals("<StateW>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					

					try {
						drawArea.setStateW(Integer.parseInt(line2));
					}
					catch (NumberFormatException nfe) { }
				}
				
				else if(line.equals("<StateH>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					

					try {
						drawArea.setStateH(Integer.parseInt(line2));
					}
					catch (NumberFormatException nfe) { }
				}

				else if(line.equals("<LineWidth>"))
				{
					String line2 = reader.readLine();
					while(line2.startsWith("##"))
						line2 = reader.readLine();
					String line3 = reader.readLine();
					while(line3.startsWith("##"))
						line3 = reader.readLine();
					

					try {
						drawArea.setLineWidth(Integer.parseInt(line2));
					}
					catch (NumberFormatException nfe) { }
				}
				
				
				


				else if(line.equals("<state>"))
				{
					while((line = reader.readLine()) != null && !line.equals("</state>"))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openState(tempList);
				}
				else if(line.equals("<fork>"))
				{
					while((line = reader.readLine()) != null && !line.equals("</fork>"))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openFork(tempList);
				}
				else if(line.equals("<stategroup>") || line.equals("<substate>"))
				{
					String endTag = line.equals("<stategroup>") ? "</stategroup>" : "</substate>";
					while((line = reader.readLine()) != null && !line.equals(endTag))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openStateGroup(tempList);
				}

				else if(line.equals("<transition>"))
				{
					while((line = reader.readLine()) != null && !line.equals("</transition>"))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openTrans(tempList);
				}
				
				else if(line.equals("<textObj>"))
				{
					while((line = reader.readLine()) != null && !line.equals("</textObj>"))
					{
						if(line.startsWith("##"))
							continue;
						tempList.add(line);
					}
					openText(tempList);
				}
				
				else if(line.equals("<tabs>"))
				{
					fizzim.resetTabs();
					while((line = reader.readLine()) != null && !line.equals("</tabs>"))
					{
						if(line.startsWith("##"))
							continue;
						fizzim.addNewTab(line);
					}
				}
			}
			reader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		drawArea.open(objList);
		
		
	}

	private void openText(ArrayList<String> tempList3) {
		

		String text = tempList3.get(0);
		int x = Integer.parseInt(tempList3.get(2));
		int y = Integer.parseInt(tempList3.get(5));
		int page = Integer.parseInt(tempList3.get(8));
		
		if(!text.equals("fzm_globalTable"))
		{
			TextObj obj = new TextObj(text,x,y,page);
			objList.add(obj);
		}
		else
		{
			TextObj obj = new TextObj(x,y,globalList,page);
			objList.add(obj);
		}
		

		tempList.clear();
		
	}

	private void openFork(ArrayList<String> tempList3) {
		String name = tempList3.get(1);
		int x0 = Integer.parseInt(tempList3.get(4));
		int y0 = Integer.parseInt(tempList3.get(7));
		int x1 = Integer.parseInt(tempList3.get(10));
		int y1 = Integer.parseInt(tempList3.get(13));
		int page = Integer.parseInt(tempList3.get(16));
		Color currColor = Color.black;
		if(tempList3.size() > 19)
			currColor = new Color(Integer.parseInt(tempList3.get(19)));
		ForkObj fork = new ForkObj(x0,y0,x1,y1,name,page,currColor);
		objList.add(fork);
		tempList.clear();
	}

	private void openStateGroup(ArrayList<String> tempList3) {
		int startAttributes = tempList3.indexOf("<attributes>");
		int endAttributes = tempList3.indexOf("</attributes>");
		LinkedList<ObjAttribute> newList = new LinkedList<ObjAttribute>();
		openAttributeList(tempList3,startAttributes+1,endAttributes-1,newList);
		Color currColor = Color.black;

		int i = endAttributes;
		int x0 = Integer.parseInt(tempList3.get(i+2));
		int y0 = Integer.parseInt(tempList3.get(i+5));
		int x1 = Integer.parseInt(tempList3.get(i+8));
		int y1 = Integer.parseInt(tempList3.get(i+11));
		int page = Integer.parseInt(tempList3.get(i+14));
		if(tempList3.size() > i+17 && tempList3.get(i+16).equals("<color>"))
			currColor = new Color(Integer.parseInt(tempList3.get(i+17)));

		String name = newList.get(0).getValue();
		StateGroupObj stateGroup = new StateGroupObj(x0,y0,x1,y1,newList,name,page,currColor);
		int startEntry = tempList3.indexOf("<entryState>");
		int endEntry = tempList3.indexOf("</entryState>");
		if(startEntry >= 0 && endEntry > startEntry && startEntry + 1 < endEntry)
			stateGroup.setEntryState(tempList3.get(startEntry + 1));
		int startChildren = tempList3.indexOf("<children>");
		int endChildren = tempList3.indexOf("</children>");
		if(startChildren >= 0 && endChildren > startChildren)
		{
			LinkedList<String> children = new LinkedList<String>();
			for(int j = startChildren; j < endChildren; j++)
			{
				if(tempList3.get(j).equals("<child>") && j+1 < endChildren)
					children.add(tempList3.get(j+1));
			}
			stateGroup.setChildNames(children);
		}
		objList.add(stateGroup);
		tempList.clear();
	}

	private void openTrans(ArrayList<String> tempList3) {
		int startAttributes = tempList3.indexOf("<attributes>");
		int endAttributes = tempList3.indexOf("</attributes>");
		LinkedList<ObjAttribute> newList = new LinkedList<ObjAttribute>();
		openAttributeList(tempList3,startAttributes+1,endAttributes-1,newList);
		Color currColor = Color.black;
		
		int i = endAttributes;
		String startState = tempList3.get(i+2);
		String endState = tempList3.get(i+5);

		int sX = (int) Double.parseDouble(tempList3.get(i+8));
		int sY = (int) Double.parseDouble(tempList3.get(i+11));
		int eX = (int) Double.parseDouble(tempList3.get(i+14));
		int eY = (int) Double.parseDouble(tempList3.get(i+17));
		int sCX = (int) Double.parseDouble(tempList3.get(i+20));
		int sCY = (int) Double.parseDouble(tempList3.get(i+23));
		int eCX = (int) Double.parseDouble(tempList3.get(i+26));
		int eCY = (int) Double.parseDouble(tempList3.get(i+29));
		int sStateIndex = Integer.parseInt(tempList3.get(i+32));
		int eStateIndex = Integer.parseInt(tempList3.get(i+35));
		int page = Integer.parseInt(tempList3.get(i+38));
		
		if(ver >= 80316)
		{
			currColor = new Color(Integer.parseInt(tempList3.get(i+41)));
			i += 3;
		}
		
		Point pS = new Point();
		Point pSC = new Point();
		Point pE = new Point();
		Point pEC = new Point();
		boolean stub1 = false;
		
		if(!startState.equals(endState))
		{				
				pS.setLocation(Double.parseDouble(tempList3.get(i+41)),Double.parseDouble(tempList3.get(i+44)));
				pSC.setLocation(Double.parseDouble(tempList3.get(i+47)),Double.parseDouble(tempList3.get(i+50)));
				pE.setLocation(Double.parseDouble(tempList3.get(i+53)),Double.parseDouble(tempList3.get(i+56)));
				pEC.setLocation(Double.parseDouble(tempList3.get(i+59)),Double.parseDouble(tempList3.get(i+62)));
			
				String stub  = tempList3.get(i+65);				
				if(stub.equals("true"))
					stub1 = true;
			
		}
		
		Point sP = new Point(sX,sY);
		Point eP = new Point(eX,eY);
		Point sCP = new Point(sCX,sCY);
		Point eCP = new Point(eCX,eCY);
		
				
		TransitionObj obj;
		String name = newList.get(0).getValue();
		if(startState.equals(endState))
			obj = new LoopbackTransitionObj(sP,eP,sCP,eCP,newList,name, startState, endState,sStateIndex,eStateIndex, page, currColor);
		else
			obj = new StateTransitionObj(sP,eP,sCP,eCP,newList,name, startState, 
					endState,sStateIndex,eStateIndex, page, pS, pSC, pE,pEC,drawArea,stub1,currColor);
		

		objList.add(obj);
		tempList.clear();
		
	}

	private void openState(ArrayList<String> tempList3) {
		int startAttributes = tempList3.indexOf("<attributes>");
		int endAttributes = tempList3.indexOf("</attributes>");
		LinkedList<ObjAttribute> newList = new LinkedList<ObjAttribute>();
		openAttributeList(tempList3,startAttributes+1,endAttributes-1,newList);
		Color currColor = Color.black;
		
		int i = endAttributes;
		int x0 = Integer.parseInt(tempList3.get(i+2));
		int y0 = Integer.parseInt(tempList3.get(i+5));
		int x1 = Integer.parseInt(tempList3.get(i+8));
		int y1 = Integer.parseInt(tempList3.get(i+11));
		String reset  = tempList3.get(i+14);
		boolean reset1;
		if(reset.equals("true"))
			reset1 = true;
		else
			reset1 = false;
		
		int page = Integer.parseInt(tempList3.get(i+17));
		if(ver >= 80316)
			currColor = new Color(Integer.parseInt(tempList3.get(i+20)));
		String name = newList.get(0).getValue();
		StateObj state = new StateObj(x0,y0,x1,y1,newList,name,reset1, page, currColor);


		objList.add(state);
		tempList.clear();
		
	}


	private void openGlobal(ArrayList<String> list) {
		
		//create global list
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
		
		int mS = list.indexOf("<machine>")+1;
		int mE = list.indexOf("</machine>")-1;
		int iS = list.indexOf("<inputs>")+1;
		int iE = list.indexOf("</inputs>")-1;
		int oS = list.indexOf("<outputs>")+1;
		int oE = list.indexOf("</outputs>")-1;
		int sS = list.indexOf("<state>")+1;
		int sE = list.indexOf("</state>")-1;
		int tS = list.indexOf("<trans>")+1;
		int tE = list.indexOf("</trans>")-1;
		
		openAttributeList(list, mS, mE,globalMachineAttributes);
		openAttributeList(list, iS, iE,globalInputsAttributes);
		openAttributeList(list, oS, oE,globalOutputsAttributes);
		openAttributeList(list, sS, sE,globalStateAttributes);
		openAttributeList(list, tS, tE,globalTransAttributes);
		ensureRegdpTransitionActions();
		
		/*
		int counter = 0;
		int start = counter;
		
		//set up machine globals
		while(!list.get(counter).equals("</machine>"))
			counter++;
		openAttributeList(list, start+1, counter-1,globalMachineAttributes);
		
		//set up state globals
		start = ++counter;
		while(!list.get(counter).equals("</inputs>"))
			counter++;
		openAttributeList(list, start+1, counter-1,globalInputsAttributes);
		
		//set up trans globals
		start = ++counter;
		while(!list.get(counter).equals("</outputs>"))
			counter++;
		openAttributeList(list, start+1, counter-1,globalOutputsAttributes);
		
		//set up inputs globals
		start = ++counter;
		while(!list.get(counter).equals("</state>"))
			counter++;
		openAttributeList(list, start+1, counter-1,globalStateAttributes);
		
		//set up state globals
		start = ++counter;
		while(!list.get(counter).equals("</trans>"))
			counter++;
		openAttributeList(list, start+1, counter-1,globalTransAttributes);
		*/
		
		drawArea.updateGlobal(globalList);
		fizzim.updateGlobal(globalList);
		objList.add(globalList);
		tempList.clear();	
	
	}

	private void ensureRegdpTransitionActions() {
		int[] editable = { ObjAttribute.GLOBAL_FIXED, ObjAttribute.GLOBAL_VAR,
				ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR,
				ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR };

		for(int i = 0; i < globalOutputsAttributes.size(); i++) {
			ObjAttribute output = globalOutputsAttributes.get(i);
			if(!output.getType().equals("regdp"))
				continue;
			if(hasTransitionOutputAttribute(output.getName()))
				continue;

			globalTransAttributes.addLast(new ObjAttribute(output.getName(), "",
					output.getVisibility(), "output", "", Color.black, "", "", editable));
		}
	}

	private boolean hasTransitionOutputAttribute(String name) {
		for(int i = 0; i < globalTransAttributes.size(); i++) {
			ObjAttribute attribute = globalTransAttributes.get(i);
			if(attribute.getName().equals(name) && attribute.getType().equals("output"))
				return true;
		}
		return false;
	}
	


	private void openAttributeList(ArrayList<String> list,
			int start, int end, LinkedList<ObjAttribute> newList) {
		int pointer = start;

		while(pointer <= end)
		{
			String line = list.get(pointer);
			if(!isOpenTag(line))
			{
				pointer++;
				continue;
			}

			String name = tagName(line);
			int attrEnd = findMatchingEndTag(list, name, pointer, end);
			if(attrEnd < 0)
				break;

			String nameStatus = firstStatus(list, pointer + 1, attrEnd, "ABS");
			String value = tagValue(list, pointer, attrEnd, "value", "");
			String valueStatus = tagStatus(list, pointer, attrEnd, "value", "GLOBAL_VAR");
			String vis = tagValue(list, pointer, attrEnd, "vis", "0");
			String visStatus = tagStatus(list, pointer, attrEnd, "vis", "GLOBAL_VAR");
			String type = tagValue(list, pointer, attrEnd, "type", "");
			String typeStatus = tagStatus(list, pointer, attrEnd, "type", "GLOBAL_VAR");
			String comm = tagValue(list, pointer, attrEnd, "comment", "");
			String commStatus = tagStatus(list, pointer, attrEnd, "comment", "GLOBAL_VAR");
			String currColorStatus = tagStatus(list, pointer, attrEnd, "color", "GLOBAL_VAR");
			String useratts = tagValue(list, pointer, attrEnd, "useratts", "");
			String userattsStatus = tagStatus(list, pointer, attrEnd, "useratts", "GLOBAL_VAR");
			String resetval = tagValue(list, pointer, attrEnd, "resetval", "");
			String resetvalStatus = tagStatus(list, pointer, attrEnd, "resetval", "GLOBAL_VAR");

			Color currColor = Color.black;
			try {
				currColor = new Color(Integer.parseInt(tagValue(list, pointer, attrEnd, "color", "-16777216")));
			} catch (NumberFormatException nfe) {
				currColor = Color.black;
			}

			int x2Obj = intTagValue(list, pointer, attrEnd, "x2Obj", 0);
			int y2Obj = intTagValue(list, pointer, attrEnd, "y2Obj", 0);
			int page = intTagValue(list, pointer, attrEnd, "page", -1);

			ObjAttribute obj = new ObjAttribute(name,nameStatus,value,valueStatus,vis,visStatus,type,typeStatus,
					comm,commStatus, currColor, currColorStatus,useratts,userattsStatus,resetval,resetvalStatus,x2Obj, y2Obj, page);

			newList.add(obj);
			pointer = attrEnd + 1;
		}
	}

	private boolean isOpenTag(String line) {
		return line.startsWith("<") && line.endsWith(">") && !line.startsWith("</");
	}

	private String tagName(String line) {
		return line.substring(1,line.length()-1);
	}

	private int findTag(ArrayList<String> list, String tag, int start, int end) {
		for(int i = start; i <= end && i < list.size(); i++)
			if(list.get(i).equals(tag))
				return i;
		return -1;
	}

	private int findMatchingEndTag(ArrayList<String> list, String tag, int start, int end) {
		int depth = 0;
		String openTag = "<" + tag + ">";
		String closeTag = "</" + tag + ">";
		for(int i = start; i <= end && i < list.size(); i++) {
			String line = list.get(i);
			if(line.equals(openTag))
				depth++;
			else if(line.equals(closeTag)) {
				depth--;
				if(depth == 0)
					return i;
			}
		}
		return -1;
	}

	private String firstStatus(ArrayList<String> list, int start, int end, String def) {
		int status = findTag(list, "<status>", start, end);
		if(status >= 0 && status + 1 <= end)
			return list.get(status + 1);
		return def;
	}

	private String tagValue(ArrayList<String> list, int start, int end, String tag, String def) {
		int tagStart = findDirectChildTag(list, tag, start + 1, end - 1);
		if(tagStart >= 0 && tagStart + 1 <= end)
			return list.get(tagStart + 1);
		return def;
	}

	private String tagStatus(ArrayList<String> list, int start, int end, String tag, String def) {
		int tagStart = findDirectChildTag(list, tag, start + 1, end - 1);
		if(tagStart < 0)
			return def;
		int tagEnd = findMatchingEndTag(list, tag, tagStart, end);
		if(tagStart >= 0 && tagEnd >= 0)
			return firstStatus(list, tagStart + 1, tagEnd - 1, def);
		return def;
	}

	private int findDirectChildTag(ArrayList<String> list, String tag, int start, int end) {
		int depth = 0;
		String openTag = "<" + tag + ">";
		for(int i = start; i <= end && i < list.size(); i++) {
			String line = list.get(i);
			if(depth == 0 && line.equals(openTag))
				return i;
			if(isOpenTag(line))
				depth++;
			else if(line.startsWith("</") && line.endsWith(">") && depth > 0)
				depth--;
		}
		return -1;
	}

	private int intTagValue(ArrayList<String> list, int start, int end, String tag, int def) {
		try {
			return Integer.parseInt(tagValue(list, start, end, tag, Integer.toString(def)));
		} catch (NumberFormatException nfe) {
			return def;
		}
	}
}
