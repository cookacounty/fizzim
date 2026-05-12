import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Vector;
import javax.swing.border.LineBorder;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRootPane;
import javax.swing.JTable;
import javax.swing.InputMap;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.text.JTextComponent;

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

class OutputAttributeFilter {
	static boolean isInternal(ObjAttribute attr) {
		return hasUserAttribute(attr, "suppress_portlist");
	}

	static boolean hasUserAttribute(ObjAttribute attr, String attribute) {
		String userAtts = attr.getUserAtts();
		if(userAtts == null)
			return false;
		String[] tokens = userAtts.split("[,;\\s]+");
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals(attribute))
				return true;
		}
		return false;
	}

	static void setInternal(ObjAttribute attr, boolean internal) {
		if(internal) {
			addUserAttribute(attr, "suppress_portlist");
			return;
		}
		removeUserAttribute(attr, "suppress_portlist");
	}

	private static void addUserAttribute(ObjAttribute attr, String attribute) {
		if(hasUserAttribute(attr, attribute))
			return;
		String userAtts = attr.getUserAtts();
		if(userAtts == null || userAtts.trim().equals(""))
			attr.setUserAtts(attribute);
		else
			attr.setUserAtts(userAtts.trim() + " " + attribute);
	}

	private static void removeUserAttribute(ObjAttribute attr, String attribute) {
		String userAtts = attr.getUserAtts();
		if(userAtts == null || userAtts.trim().equals(""))
			return;
		String[] tokens = userAtts.split("[,;\\s]+");
		String newUserAtts = "";
		for(int i = 0; i < tokens.length; i++) {
			if(tokens[i].equals("") || tokens[i].equals(attribute))
				continue;
			if(!newUserAtts.equals(""))
				newUserAtts += " ";
			newUserAtts += tokens[i];
		}
		attr.setUserAtts(newUserAtts);
	}
}

class MyTableModel extends AbstractTableModel {
	
	boolean DEBUG = false;
// pz
//	String[] columnNames = {"Attribute Name", "Value",
//			"Visibility", "Type", "Comment", "Color" };
	String[] columnNames = {"Attribute Name", "Value",
			"Visibility", "Type", "Comment",
                        "Color","UserAtts","ResetValue" }; // for state/trans edit boxes
	boolean global = false;

    GeneralObj obj;
    LinkedList<ObjAttribute> attrib;
    LinkedList<LinkedList<ObjAttribute>> globalList;
    DrawArea drawArea;
    JDialog dialog;
    int tab;

    
	MyTableModel(GeneralObj s,JDialog dia,LinkedList<LinkedList<ObjAttribute>> global, int k)
	{
		obj = s;
		attrib = obj.getAttributeList();
		globalList = global;
		dialog = dia;
		tab = k;
	}
	
	MyTableModel(LinkedList<ObjAttribute> list,LinkedList<LinkedList<ObjAttribute>> globalL)
	{
		this(list, globalL, null);
	}

	MyTableModel(LinkedList<ObjAttribute> list,LinkedList<LinkedList<ObjAttribute>> globalL, DrawArea da)
	{
		global = true;
		globalList = globalL;
		attrib = list;
		drawArea = da;
// pz
//		columnNames = new String[] {"Attribute Name", "Default Value",
//				"Visibility", "Type","Comment", "Color"};
		columnNames = new String[] {"Attribute Name", "Default Value",
				"Visibility", "Type","Comment",
                                "Color","UserAtts","ResetValue"}; // for main att edit boxes
	}
	

	//methods that need to be implemented

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        return attrib.size();
    }

    public Object getValueAt(int row, int col) {
    	Object obj = attrib.get(row).get(col);
    	if(col == 2) //pz - transition?
    	{
    		if(obj.equals(new Integer(0)))
    			obj = "No";
    		if(obj.equals(new Integer(1)))
    			obj = "Yes";
    		if(obj.equals(new Integer(2)))
    			obj = "Only non-default";	
    	}  		
        // Translate internal representation "reg" to "statebit"
    	if(col == 3) 
    	{
    		if(obj.equals(new String("reg")))
    			obj = "statebit";
    	}  		
    	return obj;		
    }


	// get type for column
    public Class getColumnClass(int col) {
    	if(getRowCount() == 0)
    		return String.class;
    	return getValueAt(0, col).getClass();
    }
    
    public String getColumnName(int col) {
        return columnNames[col];
    }

    //GLOBAL_FIXED can only be edited in global tab, ABS can't be edited anywhere
    public boolean isCellEditable(int row, int col) {
        if ((attrib.get(row).getEditable(col) == ObjAttribute.GLOBAL_FIXED && !global) || attrib.get(row).getEditable(col) == ObjAttribute.ABS) 
            return false;
        else if(global && attrib.equals(globalList.get(1)) && (col == 2 || col == 5))
        	return false;
         else 
            return true;
        
    }

    public void setValueAt(Object value, int row, int col) {

                        // 0: Name
                        // 1: (Default) value
                        // 2: Visibility
                        // 3: Type
                        // 4: Comment
                        // 5: Color
                        // 6: UserAtts
                        // 7: Resetval

        //turn string into corresponding number
        if(col == 2)
        {
        	if(value.equals("No"))
        		value = new Integer(0);
        	if(value.equals("Yes"))
        		value = new Integer(1);
        	if(value.equals("Only non-default"))
        		value = new Integer(2);
        }

        // Translate "statebit" to internal representation "reg"
        if (col == 3 && value.equals("statebit")) {
          value = new String("reg");
        }
        
        if(shouldCheckReservedWord(row, col) && VerilogNameValidator.showReservedWordError(dialog, (String)value, reservedWordNameType(row, col)))
        {
        	fireTableCellUpdated(row, col);
        	return;
        }

        // only flag and regdp can have reset values
        if ( false
          || (global && col == 7 && !value.equals("") && !((attrib.get(row).getType().equals("flag") || attrib.get(row).getType().equals("regdp"))) )
          )
        {
		JOptionPane.showMessageDialog(dialog,
                    "Only regdp and flag can have a reset value",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
		value = attrib.get(row).get(col);
        }

        if(global && col == 3 && attrib.equals(globalList.get(2)))
        {
		if(value.equals("regdp") && attrib.get(row).getresetval().trim().equals(""))
			attrib.get(row).setresetval("0");
		else if(!value.equals("regdp") && !value.equals("flag") && !attrib.get(row).getresetval().trim().equals(""))
			attrib.get(row).setresetval("");
        }
        // flag type outputs must have null default value
        if ( false
          || (global && col == 1 && !value.equals("") && attrib.get(row).getType().equals("flag") )
          || (global && col == 3 && value.equals("flag") && !attrib.get(row).getValue().equals("") ) 
          )
        {
        	JOptionPane.showMessageDialog(dialog,
                    "Flags cannot have default values",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
        	value = attrib.get(row).get(col);
        }
        
        // forces user to enter attribute name in outputs tab
        if(!global && col == 3 && value.equals("output"))
        {
        	if(!checkOutputs(attrib.get(row)))
        	{
        		value = "";
        		JOptionPane.showMessageDialog(dialog,
                        "Attribute with that name must exist in global outputs tab",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
        		
        	}
        }
        
        
        //first time type being set in outputs, create corresponding attribute in state tab
        if(global && col == 3 && attrib.equals(globalList.get(2)) && (value.equals("regdp") || value.equals("comb") || value.equals("reg") || value.equals("flag"))
        		&& attrib.get(row).get(col).equals(""))
        {
	        	int[] editable = { ObjAttribute.GLOBAL_FIXED, ObjAttribute.GLOBAL_VAR,
	        	ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR};
	        	ObjAttribute newObj = new ObjAttribute(attrib.get(row).getName(),attrib.get(row).getValue(),
	        			attrib.get(row).getVisibility(),"output","",Color.black,"","",editable);
				globalList.get(3).addLast(newObj);
				if(value.equals("regdp"))
				{
					ObjAttribute newTransObj = new ObjAttribute(attrib.get(row).getName(),"",
							attrib.get(row).getVisibility(),"output","",Color.black,"","",editable);
					globalList.get(4).addLast(newTransObj);
				}
        }

        if(global && col == 3 && attrib.equals(globalList.get(2)) && !attrib.get(row).get(col).equals(value)
        		&& !attrib.get(row).get(col).equals(""))
        {
        	if(value.equals("regdp"))
        	{
	        	int[] editable = { ObjAttribute.GLOBAL_FIXED, ObjAttribute.GLOBAL_VAR,
	        	ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR};
	        	removeAttribute(4,attrib.get(row).getName());
				ObjAttribute newTransObj = new ObjAttribute(attrib.get(row).getName(),"",
						attrib.get(row).getVisibility(),"output","",Color.black,"","",editable);
				globalList.get(4).addLast(newTransObj);
        	}
        	else if(attrib.get(row).getType().equals("regdp"))
        	{
        		removeAttribute(4,attrib.get(row).getName());
        	}
        }
        
        //if rename an output, preserve existing per-state and per-transition assignments
        if(global && col == 0 && globalList.get(2).equals(attrib) && !attrib.get(row).get(col).equals(value))
        {
        	if(drawArea != null)
        		drawArea.renameOutputAttributeEverywhere(attrib.get(row).getName(), (String) value);
        	else
        	{
        		renameAttribute(3,attrib.get(row).getName(),col,value,row);
        		renameAttribute(4,attrib.get(row).getName(),col,value,row);
        	}
        }
        //if changing another property of type output
        else if(global && col != 3 && globalList.get(2).equals(attrib) && !attrib.get(row).get(col).equals(value))
        {
        		renameAttribute(3,attrib.get(row).getName(),col,value,row);
        		renameAttribute(4,attrib.get(row).getName(),col,value,row);
        }
        

        //force user to edit in outputs tab
        // changed
        if(global && 
           // can't edit anything but type in states
           (col != 3 && attrib.equals(globalList.get(3)) && attrib.get(row).getType().equals("output"))
           // can't edit default value in transitions
           // new fizzim.pl (>=4.3 uses default from outputs page)
        || (col == 1 && attrib.equals(globalList.get(4)) && attrib.get(row).getType().equals("output"))
        )
        {
        	JOptionPane.showMessageDialog(dialog,
                    "Must edit in output tab",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
        	value = attrib.get(row).get(col);
        }
        
        //if changing from comb to reg
        /*
        if(attrib.equals(globalList.get(4)) && attrib.get(row).getType().equals("comb") && value.equals("reg"))
        {
        	for(int i = 0; i < globalList.get(2).size(); i++)
        	{
        		if(attrib.get(row).getName().equals(globalList.get(2).get(i).getName()))
        		{
        			globalList.get(2).remove(i);
        		}
        	}
        }
        */
        

        // dont set if nothing changes	
        if(!attrib.get(row).get(col).equals(value))
        	attrib.get(row).set(col,value);
        
        
        // set to local if different from global
        if(!global)
        {
        	if(!checkValue(row,col,value))
        	{
        		attrib.get(row).setEditable(col, ObjAttribute.LOCAL);
        	}
        	else
        		attrib.get(row).setEditable(col, ObjAttribute.GLOBAL_VAR);
        }
        
        // checks for when name being changed
        if(attrib.get(row).getName().equals("name") && col == 1 && !global)
        	obj.setName((String) value);
        

        //restore to default if empty string was entered
        if(col != 2 && value.equals("") && !global)
        {
        	obj.updateAttrib(globalList,tab);
        }
        
        fireTableCellUpdated(row, col);
        
    }

    private boolean checkValue(int row, int col, Object value)
    {
    	LinkedList<ObjAttribute> list = globalList.get(tab);
    	String name = attrib.get(row).getName();
    	Object val = attrib.get(row).get(col);
    	for (int i = 0; i < list.size(); i++)
    	{
    		ObjAttribute obj = list.get(i);
    		if(name.equals(obj.getName()) && val.equals(obj.get(col)))
    			return true;
    	}
    	return false;
    }

    private boolean shouldCheckReservedWord(int row, int col)
    {
    	if(!(attrib.get(row).get(col) instanceof String))
    		return false;
    	if(global)
    	{
    		if(col == 0 && (attrib.equals(globalList.get(1)) || attrib.equals(globalList.get(2))))
    			return true;
    		return col == 1 && attrib.equals(globalList.get(0)) && attrib.get(row).getName().equals("name");
    	}
    	return col == 1 && attrib.get(row).getName().equals("name");
    }

    private String reservedWordNameType(int row, int col)
    {
    	if(global)
    	{
    		if(attrib.equals(globalList.get(1)))
    			return "input name";
    		if(attrib.equals(globalList.get(2)))
    			return "output name";
    		if(attrib.equals(globalList.get(0)) && col == 1)
    			return "module name";
    	}
    	if(obj != null)
    	{
    		if(obj.getType() == 0)
    			return "state name";
    		if(obj.getType() == 1 || obj.getType() == 2)
    			return "transition name";
    		if(obj.getType() == 4)
    			return "fork name";
    		if(obj.getType() == 5)
    			return "state group name";
    	}
    	return "name";
    }

	private void renameAttribute(int t, String name, int col, Object value, int row) {
		// try to find cooresponding field in states tab that is in the same relative position
		// (fixes errors due to multiple fields with same names)
		int num = 0;
		boolean needed = true;
		
		for(int h = 0; h < globalList.get(t).size(); h++)
		{
			ObjAttribute obj = globalList.get(t).get(h);
			// check if field is of type output in state tab
			if(obj.getType().equals("output") && t == 3 && num <= row)
			{
				if(num == row && obj.getName().equals(name))
				{
					obj.set(col, value);
					needed = false;
					break;
				}
				else
					num++;
			}
		}
		
		if(needed)
		{
			for(int i = 0; i < globalList.get(t).size(); i++)
			{
				ObjAttribute obj = globalList.get(t).get(i);
				if(obj.getName().equals(name))
				{
					obj.set(col, value);
					break;
				}
			}
		}

	}

	private void removeAttribute(int tab, String name)
	{
		for(int i = globalList.get(tab).size() - 1; i >= 0; i--)
		{
			ObjAttribute obj = globalList.get(tab).get(i);
			if(obj.getName().equals(name) && obj.getType().equals("output"))
				globalList.get(tab).remove(i);
		}
	}

	private boolean checkOutputs(ObjAttribute objAttribute) {
		LinkedList<ObjAttribute> outputList = globalList.get(2);
		String name = objAttribute.getName();
		for(int i = 0; i < outputList.size(); i++)
		{
			ObjAttribute obj = outputList.get(i);
			if(obj.getName().equals(name))
				return true;
		}
		return false;

	}
	
    private boolean checkName(LinkedList<ObjAttribute> linkedList, String name) {
		for(int i = 0; i < linkedList.size(); i++)
		{
			ObjAttribute obj = linkedList.get(i);
			if(obj.getName().equals(name))
				return true;
		}
		return false;
	}



    }

class FilteredOutputTableModel extends MyTableModel {
	private boolean internal;

	FilteredOutputTableModel(LinkedList<LinkedList<ObjAttribute>> globalL, DrawArea da, boolean showInternal) {
		super((LinkedList<ObjAttribute>)globalL.get(2), globalL, da);
		internal = showInternal;
	}

	private ArrayList<Integer> visibleRows() {
		ArrayList<Integer> rows = new ArrayList<Integer>();
		for(int i = 0; i < attrib.size(); i++) {
			if(OutputAttributeFilter.isInternal(attrib.get(i)) == internal)
				rows.add(new Integer(i));
		}
		return rows;
	}

	int actualRow(int visibleRow) {
		return visibleRows().get(visibleRow).intValue();
	}

	ObjAttribute attributeAt(int visibleRow) {
		ArrayList<Integer> rows = visibleRows();
		if(visibleRow < 0 || visibleRow >= rows.size())
			return null;
		int actualRow = rows.get(visibleRow).intValue();
		if(actualRow < 0 || actualRow >= attrib.size())
			return null;
		return attrib.get(actualRow);
	}

	public int getRowCount() {
		return visibleRows().size();
	}

	public Object getValueAt(int row, int col) {
		return super.getValueAt(actualRow(row), col);
	}

	public Class getColumnClass(int col) {
		if(getRowCount() == 0)
			return String.class;
		return getValueAt(0, col).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		return super.isCellEditable(actualRow(row), col);
	}

	public void setValueAt(Object value, int row, int col) {
		int actualRow = actualRow(row);
		super.setValueAt(value, actualRow, col);
		if(col == 6)
			fireTableDataChanged();
	}
}

class FilteredParameterTableModel extends MyTableModel {
	FilteredParameterTableModel(LinkedList<LinkedList<ObjAttribute>> globalL, DrawArea da) {
		super((LinkedList<ObjAttribute>)globalL.get(0), globalL, da);
	}

	private ArrayList<Integer> visibleRows() {
		ArrayList<Integer> rows = new ArrayList<Integer>();
		for(int i = 0; i < attrib.size(); i++) {
			if(attrib.get(i).getType().equals("parameter"))
				rows.add(new Integer(i));
		}
		return rows;
	}

	int actualRow(int visibleRow) {
		return visibleRows().get(visibleRow).intValue();
	}

	ObjAttribute attributeAt(int visibleRow) {
		ArrayList<Integer> rows = visibleRows();
		if(visibleRow < 0 || visibleRow >= rows.size())
			return null;
		int actualRow = rows.get(visibleRow).intValue();
		if(actualRow < 0 || actualRow >= attrib.size())
			return null;
		return attrib.get(actualRow);
	}

	public int getRowCount() {
		return visibleRows().size();
	}

	public Object getValueAt(int row, int col) {
		return super.getValueAt(actualRow(row), col);
	}

	public Class getColumnClass(int col) {
		if(getRowCount() == 0)
			return String.class;
		return getValueAt(0, col).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		return super.isCellEditable(actualRow(row), col);
	}

	public void setValueAt(Object value, int row, int col) {
		super.setValueAt(value, actualRow(row), col);
	}
}

class FilteredMachineTableModel extends MyTableModel {
	FilteredMachineTableModel(LinkedList<LinkedList<ObjAttribute>> globalL, DrawArea da) {
		super((LinkedList<ObjAttribute>)globalL.get(0), globalL, da);
	}

	private ArrayList<Integer> visibleRows() {
		ArrayList<Integer> rows = new ArrayList<Integer>();
		for(int i = 0; i < attrib.size(); i++) {
			if(!attrib.get(i).getType().equals("parameter"))
				rows.add(new Integer(i));
		}
		return rows;
	}

	int actualRow(int visibleRow) {
		return visibleRows().get(visibleRow).intValue();
	}

	ObjAttribute attributeAt(int visibleRow) {
		ArrayList<Integer> rows = visibleRows();
		if(visibleRow < 0 || visibleRow >= rows.size())
			return null;
		int actualRow = rows.get(visibleRow).intValue();
		if(actualRow < 0 || actualRow >= attrib.size())
			return null;
		return attrib.get(actualRow);
	}

	public int getRowCount() {
		return visibleRows().size();
	}

	public Object getValueAt(int row, int col) {
		return super.getValueAt(actualRow(row), col);
	}

	public Class getColumnClass(int col) {
		if(getRowCount() == 0)
			return String.class;
		return getValueAt(0, col).getClass();
	}

	public boolean isCellEditable(int row, int col) {
		return super.isCellEditable(actualRow(row), col);
	}

	public void setValueAt(Object value, int row, int col) {
		super.setValueAt(value, actualRow(row), col);
	}
}

class MyJColorRenderer extends JLabel implements TableCellRenderer {

	public MyJColorRenderer() {
		setOpaque(true);
	}
	public Component getTableCellRendererComponent(JTable arg0, Object arg1,
			boolean arg2, boolean arg3, int arg4, int arg5) {
		Color newColor = (Color)arg1;
        setBackground(newColor);
        return this;
	}

}

class TransitionSectionRenderer extends DefaultTableCellRenderer {
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		if(component instanceof JLabel)
		{
			JLabel label = (JLabel)component;
			label.setBorder(sectionStartsAt(table, row) ? BorderFactory.createMatteBorder(3, 0, 0, 0, new Color(190, 205, 220)) : null);
			if(isSelected)
				label.setBackground(table.getSelectionBackground());
			else if(sectionStartsAt(table, row))
				label.setBackground(new Color(245, 248, 252));
			else
				label.setBackground(table.getBackground());
		}
		return component;
	}

	private boolean sectionStartsAt(JTable table, int viewRow)
	{
		if(viewRow < 0)
			return false;
		String field = String.valueOf(table.getValueAt(viewRow, 0));
		if(field.equals("priority"))
			return true;
		if(field.startsWith("Action: "))
			return firstActionRow(table, viewRow);
		if(table.getModel().getColumnCount() > 3)
		{
			int modelRow = table.convertRowIndexToModel(viewRow);
			Object type = table.getModel().getValueAt(modelRow, 3);
			if("output".equals(String.valueOf(type)))
				return firstOutputRow(table, viewRow);
		}
		return false;
	}

	private boolean firstActionRow(JTable table, int viewRow)
	{
		for(int i = viewRow - 1; i >= 0; i--)
			if(String.valueOf(table.getValueAt(i, 0)).startsWith("Action: "))
				return false;
		return true;
	}

	private boolean firstOutputRow(JTable table, int viewRow)
	{
		if(table.getModel().getColumnCount() <= 3)
			return false;
		for(int i = viewRow - 1; i >= 0; i--)
		{
			int modelRow = table.convertRowIndexToModel(i);
			Object type = table.getModel().getValueAt(modelRow, 3);
			if("output".equals(String.valueOf(type)))
				return false;
		}
		return true;
	}
}

// http://java.sun.com/docs/books/tutorial/uiswing/components/table.html

class MyJColorEditor extends AbstractCellEditor implements ActionListener, TableCellEditor {

	

	Color currColor;
	JButton button;
	JColorChooser colorChooser;
	JDialog dialog;
	
	public MyJColorEditor(JColorChooser c)
	{
		button = new JButton();
		button.setActionCommand("edit");
		button.addActionListener(this);
		button.setBorderPainted(false);
		
		colorChooser = new JColorChooser();
		dialog = JColorChooser.createDialog(button,
		               "Pick a Color",
		               true,  //modal
		               colorChooser,
		               this,  //OK button handler
		               null); //no CANCEL button handler
		
		
	}
	
	public Object getCellEditorValue() {
		return currColor;
	}


	public void actionPerformed(ActionEvent e) {
		if ("edit".equals(e.getActionCommand())) {
			button.setBackground(currColor);
			colorChooser.setColor(currColor);
			dialog.setVisible(true);
		
			//Make the renderer reappear.
			fireEditingStopped();
	
		} else { //User pressed dialog's "OK" button.
			currColor = colorChooser.getColor();
		}
		
	}


	public Component getTableCellEditorComponent(JTable arg0, Object arg1,
			boolean arg2, int arg3, int arg4) {
		currColor = (Color)arg1;
		return button;
	}
	
}

    
class MyJComboBoxEditor extends DefaultCellEditor {
    public MyJComboBoxEditor(String[] items) {
        super(new JComboBox(items));
    }
}

class AttributeTableReorder {
	static void moveSelectedRows(JTable table, LinkedList<ObjAttribute> attributes, int direction) {
		if (table == null || attributes == null || attributes.size() < 2)
			return;
		if (table.isEditing())
			table.getCellEditor().stopCellEditing();

		int[] selectedRows = table.getSelectedRows();
		if (selectedRows.length == 0)
			return;

		int[] modelRows = new int[selectedRows.length];
		for (int i = 0; i < selectedRows.length; i++)
			modelRows[i] = table.convertRowIndexToModel(selectedRows[i]);
		Arrays.sort(modelRows);

		if (direction < 0) {
			for (int i = 0; i < modelRows.length; i++) {
				int row = modelRows[i];
				if (row <= 0)
					continue;
				ObjAttribute attr = attributes.remove(row);
				attributes.add(row - 1, attr);
				modelRows[i] = row - 1;
			}
		} else {
			for (int i = modelRows.length - 1; i >= 0; i--) {
				int row = modelRows[i];
				if (row >= attributes.size() - 1)
					continue;
				ObjAttribute attr = attributes.remove(row);
				attributes.add(row + 1, attr);
				modelRows[i] = row + 1;
			}
		}

		if (table.getModel() instanceof MyTableModel)
			((MyTableModel)table.getModel()).fireTableDataChanged();
		table.clearSelection();
		for (int i = 0; i < modelRows.length; i++) {
			int viewRow = table.convertRowIndexToView(modelRows[i]);
			if (viewRow >= 0)
				table.addRowSelectionInterval(viewRow, viewRow);
		}
		table.revalidate();
		table.repaint();
	}
}

class DialogLayoutUtil {
	static void hideColumns(JTable table, int... modelColumns) {
		TableColumnModel columnModel = table.getColumnModel();
		for(int i = 0; i < modelColumns.length; i++)
		{
			int viewColumn = table.convertColumnIndexToView(modelColumns[i]);
			if(viewColumn >= 0)
				columnModel.removeColumn(columnModel.getColumn(viewColumn));
		}
	}

	static void makeTableResizeUseful(JTable table) {
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setFillsViewportHeight(true);
		PropertyTableNavigation.install(table);
	}

	static void installDialogButtons(JDialog dialog, JButton okButton, JButton cancelButton) {
		dialog.getRootPane().setDefaultButton(okButton);
		dialog.getRootPane().putClientProperty("fizzim.cancelButton", cancelButton);
		InputMap inputMap = dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		ActionMap actionMap = dialog.getRootPane().getActionMap();
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "dialogCancel");
		actionMap.put("dialogCancel", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				cancelButton.doClick();
			}
		});
	}
}

class PropertyTableNavigation {
	static void install(final JTable table) {
		table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
		InputMap inputMap = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
		ActionMap actionMap = table.getActionMap();
		bind(inputMap, actionMap, KeyEvent.VK_UP, 0, "editNavigateUp", -1, 0, false);
		bind(inputMap, actionMap, KeyEvent.VK_DOWN, 0, "editNavigateDown", 1, 0, false);
		bind(inputMap, actionMap, KeyEvent.VK_LEFT, 0, "editNavigateLeft", 0, -1, false);
		bind(inputMap, actionMap, KeyEvent.VK_RIGHT, 0, "editNavigateRight", 0, 1, false);
		bind(inputMap, actionMap, KeyEvent.VK_ENTER, 0, "editNavigateEnter", 1, 0, true);
		bind(inputMap, actionMap, KeyEvent.VK_TAB, 0, "editNavigateTab", 0, 1, false);
		bind(inputMap, actionMap, KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK, "editNavigateShiftTab", 0, -1, false);
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "editCancelOrClear");
		actionMap.put("editCancelOrClear", new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				Object source = evt.getSource();
				if(source instanceof JTable)
					cancelEditOrSelection((JTable)source);
			}
		});
		table.addKeyListener(new KeyAdapter() {
			public void keyTyped(KeyEvent evt) {
				startReplacingSelection(table, evt);
			}
		});
	}

	private static void bind(InputMap inputMap, ActionMap actionMap, int key, int modifiers, String name, final int rowDelta, final int colDelta, final boolean defaultOnIdle) {
		inputMap.put(KeyStroke.getKeyStroke(key, modifiers), name);
		actionMap.put(name, new AbstractAction() {
			public void actionPerformed(ActionEvent evt) {
				Object source = evt.getSource();
				if(source instanceof JTable)
					commitMoveAndEdit((JTable)source, rowDelta, colDelta, defaultOnIdle);
			}
		});
	}

	private static void commitMoveAndEdit(JTable table, int rowDelta, int colDelta, boolean defaultOnIdle) {
		boolean wasEditing = table.isEditing();
		int row = table.getEditingRow();
		int col = table.getEditingColumn();
		if(row < 0 || col < 0)
		{
			row = table.getSelectedRow();
			col = table.getSelectedColumn();
		}
		if(row < 0 || col < 0)
			return;
		if(table.isEditing() && !table.getCellEditor().stopCellEditing())
			return;
		int[] target = wasEditing ? nextEditableTextCell(table, row, col, rowDelta, colDelta) : nextVisibleCell(table, row, col, rowDelta, colDelta);
		if(!wasEditing && defaultOnIdle)
		{
			clickDefaultButton(table);
			return;
		}
		if(target == null)
			return;
		table.changeSelection(target[0], target[1], false, false);
	}

	private static int[] nextVisibleCell(JTable table, int row, int col, int rowDelta, int colDelta) {
		int nextRow = Math.max(0, Math.min(table.getRowCount() - 1, row + rowDelta));
		int nextCol = Math.max(0, Math.min(table.getColumnCount() - 1, col + colDelta));
		return new int[] {nextRow, nextCol};
	}

	private static int[] nextEditableTextCell(JTable table, int row, int col, int rowDelta, int colDelta) {
		int nextRow = row + rowDelta;
		int nextCol = col + colDelta;
		while(nextRow >= 0 && nextRow < table.getRowCount() && nextCol >= 0 && nextCol < table.getColumnCount())
		{
			if(isEditableTextCell(table, nextRow, nextCol))
				return new int[] {nextRow, nextCol};
			nextRow += rowDelta;
			nextCol += colDelta;
			if(rowDelta == 0 && colDelta == 0)
				break;
		}
		return null;
	}

	private static boolean isEditableTextCell(JTable table, int row, int col) {
		if(!table.isCellEditable(row, col))
			return false;
		int modelCol = table.convertColumnIndexToModel(col);
		if(modelCol == 2 || modelCol == 3 || modelCol == 5)
			return false;
		return table.getModel().getColumnClass(modelCol) == String.class || table.getValueAt(row, col) instanceof String;
	}

	private static void cancelEditOrSelection(JTable table) {
		if(table.isEditing())
		{
			table.getCellEditor().cancelCellEditing();
			return;
		}
		clickCancelButton(table);
	}

	private static void clickDefaultButton(JTable table) {
		JRootPane rootPane = SwingUtilities.getRootPane(table);
		if(rootPane != null && rootPane.getDefaultButton() != null)
			rootPane.getDefaultButton().doClick();
	}

	private static void clickCancelButton(JTable table) {
		JRootPane rootPane = SwingUtilities.getRootPane(table);
		if(rootPane == null)
			return;
		Object cancel = rootPane.getClientProperty("fizzim.cancelButton");
		if(cancel instanceof JButton)
			((JButton)cancel).doClick();
	}

	private static void startReplacingSelection(final JTable table, KeyEvent evt) {
		if(table.isEditing())
			return;
		if(evt.isAltDown() || evt.isControlDown() || evt.isMetaDown())
			return;
		char ch = evt.getKeyChar();
		if(ch == KeyEvent.CHAR_UNDEFINED || Character.isISOControl(ch))
			return;
		int row = table.getSelectedRow();
		int col = table.getSelectedColumn();
		if(row < 0 || col < 0 || !isEditableTextCell(table, row, col))
			return;
		evt.consume();
		startEditingWithText(table, row, col, String.valueOf(ch));
	}

	private static void startEditingWithText(final JTable table, final int row, final int col, final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(table.editCellAt(row, col))
				{
					Component editor = table.getEditorComponent();
					if(editor != null)
					{
						editor.requestFocusInWindow();
						if(editor instanceof JTextComponent)
						{
							JTextComponent textEditor = (JTextComponent)editor;
							textEditor.setText(text);
							textEditor.setCaretPosition(textEditor.getText().length());
						}
					}
				}
			}
		});
	}
}



class TransProperties extends javax.swing.JDialog {

	TransitionObj trans;
	DrawArea drawArea;
	StateObj start;
	StateObj end;
	StateObj pref;
	Vector<StateObj> stateObjs;
	boolean loopback = false;
	boolean stub = false;
	boolean transitionWasNew = false;
	LinkedList<LinkedList<ObjAttribute>> globalList;
	Component window = this;
	JColorChooser colorChooser;
	
	/** Creates new form TransP */
	public TransProperties(DrawArea DA,java.awt.Frame parent, boolean modal, TransitionObj t, Vector<StateObj> states,boolean b,StateObj state) {
		super(parent, modal);
		trans = t;
		drawArea = DA;
		stateObjs = states;
		loopback = b;
		pref = state;
		globalList = drawArea.getGlobalList();
		colorChooser = drawArea.getColorChooser();
		drawArea.updateTrans();
		transitionWasNew = trans.getStartState() == null;
		if(trans.getType() == 1)
		{
			StateTransitionObj t1 = (StateTransitionObj) t;
			stub = t1.getStub();
		}
		
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
		TPLabel = new javax.swing.JLabel();
		TPScroll = new javax.swing.JScrollPane();
		TPTable = new javax.swing.JTable();
		TPNew = new javax.swing.JButton();
		TPDelete = new javax.swing.JButton();
		TPUp = new javax.swing.JButton();
		TPDown = new javax.swing.JButton();
		TPCancel = new javax.swing.JButton();
		TPOK = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jComboBox1 = new javax.swing.JComboBox();
		jComboBox2 = new javax.swing.JComboBox();
		jCheckBox1 = new javax.swing.JCheckBox();

		
		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setResizable(true);
		if(!loopback)
		{
			setTitle("Edit State Transition Properties");
			TPLabel.setText("Edit the properties of the selected state transition:");
		}
		else
		{
			setTitle("Edit Loopback Transition Properties");
			TPLabel.setText("Edit the properties of the selected loopback transition:");
		}
		TPTable.setModel(new MyTableModel(trans,this,globalList,4));
		TPTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		FizzimFonts.applyCodeFont(TPTable);
		DialogLayoutUtil.makeTableResizeUseful(TPTable);
		TPTable.setDefaultRenderer(Object.class, new TransitionSectionRenderer());

		//use dropdown boxes
		String[] options = new String[]{"No", "Yes", "Only non-default"};
		TableColumn column = TPTable.getColumnModel().getColumn(2);
		column.setCellEditor(new MyJComboBoxEditor(options));
		
		column = TPTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(TPTable.getRowHeight());
		column.setCellEditor(new MyJColorEditor(colorChooser));
		column.setCellRenderer(new MyJColorRenderer());
		DialogLayoutUtil.hideColumns(TPTable, 3, 6, 7);
		TPNew.setVisible(false);
		TPDelete.setVisible(false);


		TPScroll.setViewportView(TPTable);

		TPNew.setText("New");
		TPNew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPNewActionPerformed(evt);
			}
		});

		TPDelete.setText("Delete");
		TPDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPDeleteActionPerformed(evt);
			}
		});

		TPUp.setText("\u2191");
		TPUp.setToolTipText("Move selected attribute up");
		TPUp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPUpActionPerformed(evt);
			}
		});

		TPDown.setText("\u2193");
		TPDown.setToolTipText("Move selected attribute down");
		TPDown.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPDownActionPerformed(evt);
			}
		});

		TPCancel.setText("Cancel");
		TPCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPCancelActionPerformed(evt);
			}
		});

		TPOK.setText("OK");
		TPOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				TPOKActionPerformed(evt);
			}
		});
		DialogLayoutUtil.installDialogButtons(this, TPOK, TPCancel);



		if(!loopback)
		{
			jLabel1.setText("Start State:");
			jLabel2.setText("End State:");
		}
		else
		{
			jLabel1.setText("State:");
			jLabel2.setVisible(false);
			jCheckBox1.setVisible(false);
		}
		
		
		jLabel3.setPreferredSize(new Dimension(50,20));
		jLabel3.setMinimumSize(new Dimension(50,20));
		jLabel3.setOpaque(true);
		jLabel3.setVisible(true);
		
		//set background color to color of transition and add action listener
		jLabel3.setBackground(trans.getColor());
		jLabel3.setBorder(new LineBorder(Color.black, 1));
		jLabel3.addMouseListener(new MouseListener() {
			

			ActionListener colorSel = new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					jLabel3.setBackground(colorChooser.getColor());
					trans.setColor(colorChooser.getColor());
				}	
			};	
			
			public void mouseClicked(MouseEvent e)
			{
				JDialog dialog;		
				dialog = JColorChooser.createDialog(window, "Choose Color", true, colorChooser, colorSel, null);
				dialog.setVisible(true);	
			}		
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});
		
		int size = stateObjs.size();
		
		jComboBox1.setModel(new javax.swing.DefaultComboBoxModel(stateObjs));
		if(trans.getStartState() == null)
		{
			
			if(!loopback)
			{
				jComboBox1.setSelectedIndex(size-2);
				start = stateObjs.get(size-2);
			}
			else
			{
				if(pref == null)
				{
					jComboBox1.setSelectedIndex(size-1);
					start = stateObjs.get(size-1);
				}
				else
				{
					int index = 0;
					for(int i = 0; i < stateObjs.size(); i++)
					{
						if(stateObjs.get(i).equals(pref))
						{
							index = i;
							break;
						}
					}
					jComboBox1.setSelectedIndex(index);
					start = pref;
				}
					
			}
				
		}
		else
		{
			start = trans.getStartState();
			jComboBox1.setSelectedIndex(stateObjs.indexOf(start));
		}
		if(!loopback)
		{
			jComboBox2.setModel(new javax.swing.DefaultComboBoxModel(stateObjs));
			if(trans.getEndState() == null)
			{
				jComboBox2.setSelectedIndex(size-1);
				end = stateObjs.get(size-1);
			}
			else
			{
				end = trans.getEndState();
				jComboBox2.setSelectedIndex(stateObjs.indexOf(end));
			}
		}
		else
			jComboBox2.setVisible(false);
		
		jComboBox1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				StartStateActionPerformed(evt);
			}
		});
		
		if(!loopback)
		{
			jComboBox2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					EndStateActionPerformed(evt);
				}
			});
		}
		
		if(!loopback)
		{
			jCheckBox1.setText("Stub?");
			jCheckBox1.setSelected(stub);
			jCheckBox1.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0,
					0, 0));
			jCheckBox1.setMargin(new java.awt.Insets(0, 0, 0, 0));
		}
		
		
		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																TPScroll,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																480,
																Short.MAX_VALUE)
														.add(TPLabel)
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.TRAILING,
																								false)
																						.add(
																								layout
																										.createSequentialGroup()
																										.add(
																												jLabel2)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED,
																												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																												Short.MAX_VALUE)
																										.add(
																												jComboBox2,
																												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																						.add(
																								org.jdesktop.layout.GroupLayout.LEADING,
																								layout
																										.createSequentialGroup()
																										.add(
																												jLabel1)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED,
																												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																												Short.MAX_VALUE)
																										.add(
																												jComboBox1,
																												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																												org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																						.add(
																								org.jdesktop.layout.GroupLayout.LEADING,
																								layout
																										.createSequentialGroup()
																										.add(
																												TPNew)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(
																												TPDelete)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(
																												TPUp)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(
																												TPDown)))
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.LEADING)
																						.add(
																								layout
																										.createSequentialGroup()
																										.add(
																												240,
																												240,
																												240)
																										.add(
																												TPOK)
																										.addPreferredGap(
																												org.jdesktop.layout.LayoutStyle.RELATED)
																										.add(
																												TPCancel))
																						.add(
																								layout
																										.createSequentialGroup()
																										.add(
																												42,
																												42,
																												42)
																										.add(
																												jLabel3))
																						.add(
																								layout
																										.createSequentialGroup()
																										.add(
																												42,
																												42,
																												42)
																										.add(
																												jCheckBox1)))))
										.addContainerGap()));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(TPLabel)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												TPScroll,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												151,
												Short.MAX_VALUE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(TPNew).add(
																TPDelete).add(
																		TPUp).add(
																				TPDown))
										.add(22, 22, 22)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(jLabel1)
														.add(
																jComboBox1,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
														.add(jLabel3)
														)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																layout
																		.createSequentialGroup()
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED,
																				11,
																				Short.MAX_VALUE)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								TPCancel)
																						.add(
																								TPOK))
																		.addContainerGap())
														.add(
																layout
																		.createSequentialGroup()
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								jLabel2)
																						.add(
																								jComboBox2,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																						.add(jCheckBox1))
																		.addContainerGap()))));
		pack();
		setMinimumSize(getSize());
	}// </editor-fold>//GEN-END:initComponents

	
	private void StartStateActionPerformed(ActionEvent evt) {
		JComboBox cb = (JComboBox)evt.getSource();
        StateObj selState = (StateObj)cb.getSelectedItem();
        start = selState;
		
	}
	
	private void EndStateActionPerformed(ActionEvent evt) {
		JComboBox cb = (JComboBox)evt.getSource();
        StateObj selState = (StateObj)cb.getSelectedItem();
        end = selState;
	}
	
	//GEN-FIRST:event_TPCancelActionPerformed
	private void TPCancelActionPerformed(java.awt.event.ActionEvent evt) {
		drawArea.cancel();
		dispose();
	}//GEN-LAST:event_TPCancelActionPerformed

	//GEN-FIRST:event_TPOKActionPerformed
	private void TPOKActionPerformed(java.awt.event.ActionEvent evt) {
		if(TPTable.isEditing())
			TPTable.getCellEditor().stopCellEditing();
		if(!drawArea.checkTransNames())
		{
			JOptionPane.showMessageDialog(this,
                    "Two different transitions cannot have the same name.",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(drawArea.checkReservedTransNames())
		{
			boolean endpointsChanged = trans.getStartState() != start || (!loopback && trans.getEndState() != end);
			boolean stubChanged = trans.getType() == 1 && jCheckBox1.isSelected() != stub;
			if(!drawArea.pendingUndoChanges() && !endpointsChanged && !stubChanged)
			{
				drawArea.cancel();
				dispose();
				return;
			}
			if(!loopback)
			{
				if(start != end)
					trans.initTrans(start,end);
				if(transitionWasNew)
					drawArea.assignPriorityForNewTransition(trans);
				boolean b = jCheckBox1.isSelected();
				if(b != stub)
				{
					if(trans.getType() == 1)
					{
						StateTransitionObj t1 = (StateTransitionObj) trans;
						t1.setStub(b);
					}
				}
					
				if(start != end)
					
				{
			
					drawArea.commitUndo();
					dispose();
				}
				else
				{
					JOptionPane.showMessageDialog(this,
		                    "'Start State' and 'End State' must be different.",
		                    "error",
		                    JOptionPane.ERROR_MESSAGE);
				}
			}
			else
			{
				trans.initTrans(start);
				if(transitionWasNew)
					drawArea.assignPriorityForNewTransition(trans);
				drawArea.commitUndo();
				dispose();
			}
		}
	}//GEN-LAST:event_TPOKActionPerformed

	//GEN-FIRST:event_TPDeleteActionPerformed
	private void TPDeleteActionPerformed(java.awt.event.ActionEvent evt) {
		// delete selected rows
		int[] rows = TPTable.getSelectedRows();
		for(int i = rows.length - 1; i > -1; i--)
		{
			int type = trans.getAttributeList().get(rows[i]).getEditable(0);
			if(type != ObjAttribute.GLOBAL_FIXED && type != ObjAttribute.ABS)
			{
				trans.getAttributeList().remove(rows[i]);
				TPTable.revalidate();
			}
			else
			{
				JOptionPane.showMessageDialog(this,
                        "Cannot delete a global attribute.\n"
						+ "Must be removed from global attribute properties.",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
			}
		}
	}//GEN-LAST:event_TPDeleteActionPerformed

	//GEN-FIRST:event_TPNewActionPerformed
	private void TPNewActionPerformed(java.awt.event.ActionEvent evt) {
		ObjAttribute newObj = new ObjAttribute("","",ObjAttribute.NO,"","",Color.black,"","");
		trans.getAttributeList().addLast(newObj);
		TPTable.revalidate();
	}//GEN-LAST:event_TPNewActionPerformed

	private void TPUpActionPerformed(java.awt.event.ActionEvent evt) {
		AttributeTableReorder.moveSelectedRows(TPTable, trans.getAttributeList(), -1);
	}

	private void TPDownActionPerformed(java.awt.event.ActionEvent evt) {
		AttributeTableReorder.moveSelectedRows(TPTable, trans.getAttributeList(), 1);
	}


	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JButton TPCancel;
	private javax.swing.JButton TPDelete;
	private javax.swing.JButton TPDown;
	private javax.swing.JLabel TPLabel;
	private javax.swing.JButton TPNew;
	private javax.swing.JButton TPOK;
	private javax.swing.JButton TPUp;
	private javax.swing.JScrollPane TPScroll;
	private javax.swing.JTable TPTable;
	private javax.swing.JComboBox jComboBox1;
	private javax.swing.JComboBox jComboBox2;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JCheckBox jCheckBox1;
	// End of variables declaration//GEN-END:variables

}

class StateProperties extends javax.swing.JDialog {
	StateObj state;
	DrawArea drawArea;
	Component window = this;
	JColorChooser colorChooser;
	LinkedList<LinkedList<ObjAttribute>> globalList;
		
	
	/** Creates new form StateProperties 
	 * @param drawArea */

	public StateProperties(DrawArea DA, java.awt.Frame parent, boolean modal, StateObj s) {
		super(parent, modal);
		state = s;
		oldName = new String(state.getName());
		drawArea = DA;
		globalList = drawArea.getGlobalList();
		colorChooser = drawArea.getColorChooser();
		initComponents();
	}
	
	private void initComponents() {
		SPLabel = new javax.swing.JLabel();
		SPScroll = new javax.swing.JScrollPane();
		SPTable = new javax.swing.JTable();
		SPW = new javax.swing.JLabel();
		SPH = new javax.swing.JLabel();
		SPEntryStateLabel = new javax.swing.JLabel();
		SPEntryState = new javax.swing.JComboBox();
		SPC = new javax.swing.JLabel();
		SPWField = new javax.swing.JFormattedTextField(NumberFormat.getIntegerInstance());
		SPHField = new javax.swing.JFormattedTextField(NumberFormat.getIntegerInstance());
		SPCancel = new javax.swing.JButton();
		SPOK = new javax.swing.JButton();
		SPNew = new javax.swing.JButton();
		SPDelete = new javax.swing.JButton();
		SPUp = new javax.swing.JButton();
		SPDown = new javax.swing.JButton();

		setResizable(true);

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle("Edit State Properties");
		SPLabel.setText("Edit the properties of the selected state:");

                // Type column
		SPTable.setModel(new MyTableModel(state,this,globalList,3));
		SPTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		FizzimFonts.applyCodeFont(SPTable);
		DialogLayoutUtil.makeTableResizeUseful(SPTable);

		//use dropdown boxes
		String[] options = new String[]{"No", "Yes", "Only non-default"};
		TableColumn column = SPTable.getColumnModel().getColumn(2);
		column.setCellEditor(new MyJComboBoxEditor(options));
		
                // Color column
		column = SPTable.getColumnModel().getColumn(5);
		column.setPreferredWidth(SPTable.getRowHeight());
		column.setCellEditor(new MyJColorEditor(colorChooser));
		column.setCellRenderer(new MyJColorRenderer());
		DialogLayoutUtil.hideColumns(SPTable, 3, 6, 7);

		SPNew.setVisible(false);
		SPDelete.setVisible(false);


		SPScroll.setViewportView(SPTable);

		SPW.setText("Width:");
		SPH.setText("Height:");
		SPEntryStateLabel.setText("Default entry:");
		if(state.getType() == 5)
		{
			LinkedList<String> childNames = drawArea.getStateGroupChildNames((StateGroupObj)state);
			for(int i = 0; i < childNames.size(); i++)
				SPEntryState.addItem(childNames.get(i));
			if(((StateGroupObj)state).getEntryState() != null && !((StateGroupObj)state).getEntryState().equals(""))
				SPEntryState.setSelectedItem(((StateGroupObj)state).getEntryState());
		}
		else
		{
			SPEntryStateLabel.setVisible(false);
			SPEntryState.setVisible(false);
		}
		
		SPC.setPreferredSize(new Dimension(50,20));
		SPC.setMinimumSize(new Dimension(50,20));
		SPC.setOpaque(true);
		SPC.setVisible(true);
		
		//set background color to color of transition and add action listener
		SPC.setBackground(state.getColor());
		SPC.setBorder(new LineBorder(Color.black, 1));
		SPC.addMouseListener(new MouseListener() {
			
			ActionListener colorSel = new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					SPC.setBackground(colorChooser.getColor());
					state.setColor(colorChooser.getColor());
				}	
			};	
			
			public void mouseClicked(MouseEvent e)
			{
				JDialog dialog;		
				dialog = JColorChooser.createDialog(window, "Choose Color", true, colorChooser, colorSel, null);
				dialog.setVisible(true);	
			}		
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mousePressed(MouseEvent e) {}
			public void mouseReleased(MouseEvent e) {}
		});

			

		SPWField.setValue(new Integer(state.getWidth()));
		SPWField.setColumns(10);
		SPHField.setValue(new Integer(state.getHeight()));
		SPHField.setColumns(10);

		
		SPCancel.setText("Cancel");
		SPCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPCancelActionPerformed(evt);
			}
		});

		SPOK.setText("OK");
		SPOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPOKActionPerformed(evt);
			}
		});
		DialogLayoutUtil.installDialogButtons(this, SPOK, SPCancel);

		SPNew.setText("New");
		SPNew.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPNewActionPerformed(evt);
			}
		});

		SPDelete.setText("Delete");
		SPDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPDeleteActionPerformed(evt);
			}
		});

		SPUp.setText("\u2191");
		SPUp.setToolTipText("Move selected attribute up");
		SPUp.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPUpActionPerformed(evt);
			}
		});

		SPDown.setText("\u2193");
		SPDown.setToolTipText("Move selected attribute down");
		SPDown.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				SPDownActionPerformed(evt);
			}
		});

		org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
				getContentPane());
		getContentPane().setLayout(layout);
		layout
				.setHorizontalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																SPScroll,
																org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																480,
																Short.MAX_VALUE)
														.add(SPLabel)
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.LEADING)
																						.add(
																								SPW)
																						.add(
																								SPH)
																						.add(
																								SPEntryStateLabel)
																						)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.LEADING)
																						.add(
																								SPHField,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																						.add(
																								SPWField,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																						.add(
																								SPEntryState,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																		.add(
																												42,
																												42,
																												42)
																								.add(SPC)
																		.add(
																				259,
																				259,
																				259)
																		.add(
																				SPOK)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				SPCancel))
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				SPNew)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				SPDelete)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				SPUp)
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				SPDown)))
										.addContainerGap()));
		layout
				.setVerticalGroup(layout
						.createParallelGroup(
								org.jdesktop.layout.GroupLayout.LEADING)
						.add(
								layout
										.createSequentialGroup()
										.addContainerGap()
										.add(SPLabel)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												SPScroll,
												org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
												151,
												Short.MAX_VALUE)
										.addPreferredGap(
												org.jdesktop.layout.LayoutStyle.RELATED)
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.BASELINE)
														.add(SPNew).add(
																SPDelete).add(
																		SPUp).add(
																				SPDown))
										.add(
												layout
														.createParallelGroup(
																org.jdesktop.layout.GroupLayout.LEADING)
														.add(
																layout
																		.createSequentialGroup()
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED,
																				55,
																				Short.MAX_VALUE)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								SPCancel)
																						.add(
																								SPOK))

																		.addContainerGap())
														.add(
																layout
																		.createSequentialGroup()
																		.add(
																				22,
																				22,
																				22)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								SPW)
																						.add(
																								SPWField,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
																								
																						.add(
																								SPC))
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								SPH)
																						.add(
																								SPHField,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																		.addPreferredGap(
																				org.jdesktop.layout.LayoutStyle.RELATED)
																		.add(
																				layout
																						.createParallelGroup(
																								org.jdesktop.layout.GroupLayout.BASELINE)
																						.add(
																								SPEntryStateLabel)
																						.add(
																								SPEntryState,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE,
																								org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																								org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
																		.addContainerGap()))));
		pack();
		setMinimumSize(getSize());
	}// </editor-fold>//GEN-END:initComponents

	//GEN-FIRST:event_SPNewActionPerformed
	private void SPNewActionPerformed(java.awt.event.ActionEvent evt) {
		ObjAttribute newObj = new ObjAttribute("","",ObjAttribute.NO,"","",Color.black,"","");
		state.getAttributeList().addLast(newObj);
		SPTable.revalidate();
		}//GEN-LAST:event_SPNewActionPerformed

	//GEN-FIRST:event_SPDeleteActionPerformed
	private void SPDeleteActionPerformed(java.awt.event.ActionEvent evt) {
		// delete selected rows
		int[] rows = SPTable.getSelectedRows();
		for(int i = rows.length - 1; i > -1; i--)
		{
			int type = state.getAttributeList().get(rows[i]).getEditable(0);
			if(type != ObjAttribute.GLOBAL_FIXED && type != ObjAttribute.ABS)
			{
				state.getAttributeList().remove(rows[i]);
				SPTable.revalidate();
			}
			else
			{
				JOptionPane.showMessageDialog(this,
                        "Cannot delete a global attribute.\n"
						+ "Must be removed from global attribute properties.",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
			}
		}
		//notify attribute list?
	}//GEN-LAST:event_SPDeleteActionPerformed

	private void SPUpActionPerformed(java.awt.event.ActionEvent evt) {
		AttributeTableReorder.moveSelectedRows(SPTable, state.getAttributeList(), -1);
	}

	private void SPDownActionPerformed(java.awt.event.ActionEvent evt) {
		AttributeTableReorder.moveSelectedRows(SPTable, state.getAttributeList(), 1);
	}

	//GEN-FIRST:event_SPOKActionPerformed
	private void SPOKActionPerformed(java.awt.event.ActionEvent evt) {
		if(SPTable.isEditing())
			SPTable.getCellEditor().stopCellEditing();
		if(!drawArea.checkStateNames())
		{
			JOptionPane.showMessageDialog(this,
                    "Two different states cannot have the same name.",
                    "error",
                    JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(!drawArea.checkReservedStateNames())
			return;
		//temp
		try {
			SPWField.commitEdit();
			SPHField.commitEdit();
		} catch (ParseException e) {
			// TODsO Auto-generated catch block
			e.printStackTrace();
		}
		for(int j = 0; j < globalList.get(0).size(); j++)
		{
			if(globalList.get(0).get(j).getName().equals("reset_state") && globalList.get(0).get(j).getValue().equals(oldName))
			 {
				globalList.get(0).get(j).setValue(state.getName());
			 }
		}
		int width = ((Number) SPWField.getValue()).intValue();
		int height = ((Number) SPHField.getValue()).intValue();
		boolean geometryChanged = width != state.getWidth() || height != state.getHeight();
		if(geometryChanged)
			state.setSize(width,height);
		if(state.getType() == 5 && SPEntryState.getSelectedItem() != null)
		{
			String entryState = (String)SPEntryState.getSelectedItem();
			if(!entryState.equals(((StateGroupObj)state).getEntryState()))
				((StateGroupObj)state).setEntryState(entryState);
		}
		if(!drawArea.pendingUndoChanges())
		{
			drawArea.cancel();
			dispose();
			return;
		}
		if(geometryChanged)
		{
			state.setStateModifiedTrue();
			drawArea.updateTransitions();
		}
		drawArea.updateStates();
		drawArea.updateGlobalTable();
		drawArea.commitUndo();
		dispose();
	}//GEN-LAST:event_SPOKActionPerformed

	//GEN-FIRST:event_SPCancelActionPerformed
	private void SPCancelActionPerformed(java.awt.event.ActionEvent evt) {
		drawArea.cancel();
		dispose();
	}//GEN-LAST:event_SPCancelActionPerformed

	/**
	 * @param args the command line arguments
	 */


	//GEN-BEGIN:variables
	// Variables declaration - do not modify
	private javax.swing.JButton SPCancel;
	private javax.swing.JButton SPDelete;
	private javax.swing.JButton SPDown;
	private javax.swing.JLabel SPH;
	private javax.swing.JComboBox SPEntryState;
	private javax.swing.JLabel SPEntryStateLabel;
	private javax.swing.JFormattedTextField SPHField;
	private javax.swing.JLabel SPLabel;
	private javax.swing.JButton SPNew;
	private javax.swing.JButton SPOK;
	private javax.swing.JButton SPUp;
	private javax.swing.JScrollPane SPScroll;
	private javax.swing.JTable SPTable;
	private javax.swing.JLabel SPW;
	private javax.swing.JLabel SPC;
	private javax.swing.JFormattedTextField SPWField;
	private String oldName;
	
	// End of variables declaration//GEN-END:variables


	
}

/**
*
* @author  __USER__
*/
class GlobalProperties extends javax.swing.JDialog {
	
	private static final int TAB_MACHINE = 0;
	private static final int TAB_PARAMETERS = 1;
	private static final int TAB_INPUTS = 2;
	private static final int TAB_OUTPUTS = 3;
	private static final int TAB_INTERNALS = 4;
	private static final int TAB_STATES = 5;
	private static final int TAB_TRANSITIONS = 6;

	LinkedList<LinkedList<ObjAttribute>> globalLists;
	DrawArea drawArea;
	
	String[] options = new String[]{"No", "Yes", "Only non-default"};
        // pz
	//String[] outputOptions = new String[] {"reg","comb","regdp"};
	//String[] outputOptions = new String[] {"reg","comb","regdp","flag"};
	String[] outputOptions = new String[] {"statebit","comb","regdp","flag"};
	String[] reset_signal = new String[] {"posedge","negedge","positive","negative"};
	MyJComboBoxEditor reset_signal_editor = new MyJComboBoxEditor(reset_signal);
	String[] clock = new String[] {"posedge","negedge"};
	MyJComboBoxEditor clock_editor = new MyJComboBoxEditor(clock);
	String[] resetType = new String[] {"allzeros","allones","anyvalue"};
	MyJComboBoxEditor resetType_editor = new MyJComboBoxEditor(resetType);
	String[] stateObjs;
	MyJComboBoxEditor stateSelect_editor;
	private JTable currTable = null;
	private int currTab = 0;
	private FilteredMachineTableModel machineTableModel;
	private FilteredOutputTableModel outputTableModel;
	private FilteredOutputTableModel internalTableModel;
	private FilteredParameterTableModel parameterTableModel;
	int[] editable = { ObjAttribute.GLOBAL_FIXED, ObjAttribute.GLOBAL_VAR,
			ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR};
	int[] editable2 = { ObjAttribute.ABS, ObjAttribute.GLOBAL_VAR,
			ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR, ObjAttribute.GLOBAL_VAR};

	JColorChooser colorChooser;
	
	/** Creates new form GlobalP */
	public GlobalProperties(DrawArea DA, java.awt.Frame parent, boolean modal,LinkedList<LinkedList<ObjAttribute>> globals, int tab) {
		super(parent, modal);
		globalLists = globals;
		drawArea = DA;
		stateObjs = drawArea.getStateNames();
		stateSelect_editor = new MyJComboBoxEditor(stateObjs);
		colorChooser = drawArea.getColorChooser();
		initComponents();
		int uiTab = tabToUiTab(tab);
		GPTabbedPane.setSelectedIndex(uiTab);
		currTab = uiTab;
		setTable(uiTab);
		
	}
	
                private void setcolumnwidths(JTable table) {
			FizzimFonts.applyCodeFont(table);
			table.setRowSelectionAllowed(true);
			table.setColumnSelectionAllowed(false);

                        // Name
			setColumnWidth(table, 0, 40);
                        // Default value
			setColumnWidth(table, 1, 15);
                        // Visibility
			setColumnWidth(table, 2, 30);
                        // Type
			setColumnWidth(table, 3, 10);
                        // Comment
			setColumnWidth(table, 4, 100);
                        // Color
			setColumnWidth(table, 5, 5);
                        // UserAtts
			setColumnWidth(table, 6, 100);
                        // Resetval
			setColumnWidth(table, 7, 15);
                }

		private void setColumnWidth(JTable table, int modelColumn, int width) {
			int viewColumn = table.convertColumnIndexToView(modelColumn);
			if(viewColumn >= 0)
				table.getColumnModel().getColumn(viewColumn).setPreferredWidth(width);
		}

		private void initComponents() {
			
			
			GPLabel = new javax.swing.JLabel();
			GPLabel2 = new javax.swing.JLabel();
			GPTabbedPane = new javax.swing.JTabbedPane();
			GPScrollMachine = new javax.swing.JScrollPane();
			GPTableMachine = new javax.swing.JTable()
			{
				public TableCellEditor getCellEditor(int row, int column)
				{
					int modelColumn = convertColumnIndexToModel( column );
					String name = (String) this.getValueAt(row,0);
					if (modelColumn == 3 && name.equals("reset_signal"))
						return (TableCellEditor)reset_signal_editor;
					else if (modelColumn == 3 && name.equals("clock"))
						return (TableCellEditor)clock_editor;
					else if (modelColumn == 3 && name.equals("reset_state"))
						return (TableCellEditor)resetType_editor;
					else if (modelColumn == 1 && name.equals("reset_state"))
						return (TableCellEditor)stateSelect_editor;
					else
						return super.getCellEditor(row, column);
				}
			};
			GPScrollParameters = new javax.swing.JScrollPane();
			GPTableParameters = new javax.swing.JTable();
			GPScrollState = new javax.swing.JScrollPane();
			GPTableState = new javax.swing.JTable();
			GPScrollTrans = new javax.swing.JScrollPane();
			GPTableTrans = new javax.swing.JTable();
			GPScrollInputs = new javax.swing.JScrollPane();
			GPTableInputs = new javax.swing.JTable();
			GPScrollOutputs = new javax.swing.JScrollPane();
			GPTableOutputs = new javax.swing.JTable();
			GPScrollInternals = new javax.swing.JScrollPane();
			GPTableInternals = new javax.swing.JTable();
			GPCancel = new javax.swing.JButton();
			GPOK = new javax.swing.JButton();
			GPOption1 = new javax.swing.JButton();
			GPOption2 = new javax.swing.JButton();
			GPOption3 = new javax.swing.JButton();
			GPOption4 = new javax.swing.JButton();
			GPOption5 = new javax.swing.JButton();
			GPOption6 = new javax.swing.JButton();
			GPUp = new javax.swing.JButton();
			GPDown = new javax.swing.JButton();
			
			setTitle("Edit Global Properties");
			setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
			setResizable(true); // controls resizability on global attributes table
                        setPreferredSize(new java.awt.Dimension(900,400)); // sets default size of global attributes table

			TableColumn column;

			GPLabel
					.setText("Here you can change the global attributes of all objects.  Once an attribute is added, its default");

			GPLabel2
					.setText("value can be overridden by right clicking on an object and selecting to 'Edit Properties.'");

			machineTableModel = new FilteredMachineTableModel(globalLists, drawArea);
			GPTableMachine.setModel(machineTableModel);
			GPTableMachine.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableMachine);
			column = GPTableMachine.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
			column = GPTableMachine.getColumnModel().getColumn(5);
			//column.setPreferredWidth(GPTableMachine.getRowHeight());
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableMachine, 2, 5, 6, 7);
			GPScrollMachine.setViewportView(GPTableMachine);
			GPTabbedPane.addTab("State Machine", GPScrollMachine);

			parameterTableModel = new FilteredParameterTableModel(globalLists, drawArea);
			GPTableParameters.setModel(parameterTableModel);
			GPTableParameters.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableParameters);
			column = GPTableParameters.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
			column = GPTableParameters.getColumnModel().getColumn(5);
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableParameters, 2, 3, 5, 6, 7);
			GPScrollParameters.setViewportView(GPTableParameters);
			GPTabbedPane.addTab("Parameters", GPScrollParameters);

			GPTableInputs.setModel(new MyTableModel((LinkedList<ObjAttribute>)globalLists.get(1),globalLists, drawArea));
			GPTableInputs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableInputs);
			column = GPTableInputs.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
			column = GPTableInputs.getColumnModel().getColumn(5);
			//column.setPreferredWidth(GPTableInputs.getRowHeight());
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableInputs, 1, 2, 3, 5, 6, 7);
			GPScrollInputs.setViewportView(GPTableInputs);
			GPTabbedPane.addTab("Inputs", GPScrollInputs);
			
			outputTableModel = new FilteredOutputTableModel(globalLists, drawArea, false);
			GPTableOutputs.setModel(outputTableModel);
			GPTableOutputs.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableOutputs);
                        // Visibility
			column = GPTableOutputs.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
                        // Type
			column = GPTableOutputs.getColumnModel().getColumn(3);
			column.setCellEditor(new MyJComboBoxEditor(outputOptions));
                        // Color
			column = GPTableOutputs.getColumnModel().getColumn(5);
			//column.setPreferredWidth(GPTableOutputs.getRowHeight());
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableOutputs, 5, 6);
			GPScrollOutputs.setViewportView(GPTableOutputs);
			GPTabbedPane.addTab("Outputs", GPScrollOutputs);

			internalTableModel = new FilteredOutputTableModel(globalLists, drawArea, true);
			GPTableInternals.setModel(internalTableModel);
			GPTableInternals.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableInternals);
                        // Visibility
			column = GPTableInternals.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
                        // Type
			column = GPTableInternals.getColumnModel().getColumn(3);
			column.setCellEditor(new MyJComboBoxEditor(outputOptions));
                        // Color
			column = GPTableInternals.getColumnModel().getColumn(5);
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableInternals, 5, 6);
			GPScrollInternals.setViewportView(GPTableInternals);
			GPTabbedPane.addTab("Internals", GPScrollInternals);

			GPTableState.setModel(new MyTableModel((LinkedList<ObjAttribute>)globalLists.get(3),globalLists, drawArea));
			GPTableState.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableState);
			column = GPTableState.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
			column = GPTableState.getColumnModel().getColumn(5);
			//column.setPreferredWidth(GPTableState.getRowHeight());
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableState, 3, 6, 7);
			GPScrollState.setViewportView(GPTableState);

			GPTableTrans.setModel(new MyTableModel((LinkedList<ObjAttribute>)globalLists.get(4),globalLists, drawArea));
			GPTableTrans.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			DialogLayoutUtil.makeTableResizeUseful(GPTableTrans);
			column = GPTableTrans.getColumnModel().getColumn(2);
			column.setCellEditor(new MyJComboBoxEditor(options));
			column = GPTableTrans.getColumnModel().getColumn(5);
			column.setCellEditor(new MyJColorEditor(colorChooser));
			column.setCellRenderer(new MyJColorRenderer());
			DialogLayoutUtil.hideColumns(GPTableTrans, 3, 6, 7);
			GPScrollTrans.setViewportView(GPTableTrans);

			
                        // set default column widths
                        setcolumnwidths(GPTableMachine);
                        setcolumnwidths(GPTableInputs);
                        setcolumnwidths(GPTableOutputs);
                        setcolumnwidths(GPTableInternals);
                        setcolumnwidths(GPTableState);
                        setcolumnwidths(GPTableTrans);

			
			GPTabbedPane.addChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e)
				{
					GPTabbedPaneActionPerformed(e);
				}
			});

			GPCancel.setText("Cancel");
			GPCancel.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPCancelActionPerformed(evt);
				}
			});

			GPOK.setText("OK");
			GPOK.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOKActionPerformed(evt);
				}
			});
			DialogLayoutUtil.installDialogButtons(this, GPOK, GPCancel);

			GPOption1.setText("Delete");
			GPOption1.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption1ActionPerformed(evt);
				}
			});

			GPOption2.setText("User");
			GPOption2.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption2ActionPerformed(evt);
				}
			});
			
			GPOption3.setText("Option3");
			GPOption3.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption3ActionPerformed(evt);
				}
			});
			
			GPOption4.setText("Option4");
			GPOption4.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption4ActionPerformed(evt);
				}
			});
			
			GPOption5.setText("Option5");
			GPOption5.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption5ActionPerformed(evt);
				}
			});
			
			GPOption6.setText("Option6");
			GPOption6.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPOption6ActionPerformed(evt);
				}
			});
			GPOption6.setVisible(false);

			GPUp.setText("\u2191");
			GPUp.setToolTipText("Move selected attribute up");
			GPUp.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPUpActionPerformed(evt);
				}
			});

			GPDown.setText("\u2193");
			GPDown.setToolTipText("Move selected attribute down");
			GPDown.addActionListener(new java.awt.event.ActionListener() {
				public void actionPerformed(java.awt.event.ActionEvent evt) {
					GPDownActionPerformed(evt);
				}
			});

			org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(
					getContentPane());
			getContentPane().setLayout(layout);
			layout
					.setHorizontalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.LEADING)
															.add(
																	org.jdesktop.layout.GroupLayout.TRAILING,
																	layout
																			.createSequentialGroup()
																			.add(
																					GPOK)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					GPCancel))
															.add(
																	GPTabbedPane,
																	org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
																	480,
																	Short.MAX_VALUE)
															.add(GPLabel)
															.add(GPLabel2)
															.add(
																	layout
																			.createSequentialGroup()
																			.add(
																					GPOption1)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					GPOption2)
																					.addPreferredGap(
																							org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					GPOption3)
																			.addPreferredGap(
																					org.jdesktop.layout.LayoutStyle.RELATED)
																			.add(
																					GPOption4)
																					.addPreferredGap(
																							org.jdesktop.layout.LayoutStyle.RELATED)
																							.add(
																									GPOption5)
																									.addPreferredGap(
																											org.jdesktop.layout.LayoutStyle.RELATED)
																											.add(
																													GPOption6)
																											.addPreferredGap(
																													org.jdesktop.layout.LayoutStyle.RELATED)
																											.add(
																													GPUp)
																											.addPreferredGap(
																													org.jdesktop.layout.LayoutStyle.RELATED)
																											.add(
																													GPDown)))
											.addContainerGap()));
			layout
					.setVerticalGroup(layout
							.createParallelGroup(
									org.jdesktop.layout.GroupLayout.LEADING)
							.add(
									layout
											.createSequentialGroup()
											.addContainerGap()
											.add(GPLabel)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(GPLabel2)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													GPTabbedPane,
													org.jdesktop.layout.GroupLayout.DEFAULT_SIZE,
													179,
													Short.MAX_VALUE)
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.BASELINE)
															.add(GPOption1).add(
																	GPOption2).add(
																			GPOption3).add(GPOption4).add(GPOption5).add(GPOption6).add(GPUp).add(GPDown))
											.addPreferredGap(
													org.jdesktop.layout.LayoutStyle.RELATED,
													40, Short.MAX_VALUE)
											.add(
													layout
															.createParallelGroup(
																	org.jdesktop.layout.GroupLayout.BASELINE)
															.add(GPCancel)
															.add(GPOK))
											.addContainerGap()));
			pack();
			setMinimumSize(getSize());
		}// </editor-fold>//GEN-END:initComponents
		
		protected void GPTabbedPaneActionPerformed(ChangeEvent e) {
			int tab = GPTabbedPane.getSelectedIndex();
			currTab = tab;
			setTable(tab);
			currTable.revalidate();

		}
		
		private void setTable(int tab)
		{
			GPOption2.setVisible(true);
			if(tab == TAB_MACHINE)
			{
				currTable = GPTableMachine;
				GPOption3.setVisible(true);
				GPOption3.setText("Reset");
				if(checkNames(currTable,"reset_state") && checkNames(currTable,"reset_signal"))
					GPOption3.setEnabled(false);
				else
					GPOption3.setEnabled(true);
				GPOption4.setVisible(false);
				GPOption5.setVisible(false);
				GPOption6.setVisible(false);
			}
			if(tab == TAB_PARAMETERS)
			{
				currTable = GPTableParameters;
				GPOption3.setVisible(true);
				GPOption3.setEnabled(true);
				GPOption3.setText("Parameter");
				GPOption2.setVisible(false);
				GPOption4.setVisible(false);
				GPOption5.setVisible(false);
				GPOption6.setVisible(false);
			}
			if(tab == TAB_INPUTS)
			{
				currTable = GPTableInputs;
				GPOption3.setVisible(true);
				GPOption3.setEnabled(true);
				GPOption3.setText("Input");
				GPOption4.setVisible(true);
				GPOption4.setText("Multibit Input");
				GPOption5.setVisible(false);
				GPOption6.setVisible(false);
			}
			if(tab == TAB_OUTPUTS)
			{
				currTable = GPTableOutputs;
				GPOption3.setVisible(true);
				GPOption3.setEnabled(true);
				GPOption3.setText("Output");
				GPOption4.setVisible(true);
				GPOption4.setText("Multibit Output");
				GPOption5.setVisible(false);
				GPOption6.setVisible(true);
				GPOption6.setEnabled(true);
				GPOption6.setText("Make Internal");
			}
			if(tab == TAB_INTERNALS)
			{
				currTable = GPTableInternals;
				GPOption3.setVisible(true);
				GPOption3.setEnabled(true);
				GPOption3.setText("Internal");
				GPOption4.setVisible(true);
				GPOption4.setText("Multibit Internal");
				GPOption5.setVisible(true);
				GPOption5.setText("Flag");
				GPOption6.setVisible(true);
				GPOption6.setEnabled(true);
				GPOption6.setText("Make Output");
			}
			if(tab == TAB_STATES)
			{
				currTable = GPTableState;
				GPOption3.setVisible(false);
				GPOption4.setVisible(false);
				GPOption5.setVisible(false);
				GPOption6.setVisible(false);
			}
			if(tab == TAB_TRANSITIONS)
			{
				currTable = GPTableTrans;
				GPOption3.setVisible(true);
				GPOption3.setText("Graycode");
				if(checkNames(currTable,"graycode"))
					GPOption3.setEnabled(false);
				else
					GPOption3.setEnabled(true);
				GPOption4.setVisible(true);
				GPOption4.setText("Output");
				GPOption5.setVisible(true);
				GPOption5.setText("Priority");
				if(checkNames(currTable,"priority"))
					GPOption5.setEnabled(false);
				else
					GPOption5.setEnabled(true);
				GPOption6.setVisible(false);
				
			}
		}


		private int tabToUiTab(int tab)
		{
			if(tab == 6)
				return TAB_PARAMETERS;
			if(tab == 5)
				return TAB_INTERNALS;
			if(tab == 3)
				return TAB_STATES;
			if(tab == 4)
				return TAB_TRANSITIONS;
			if(tab == 1)
				return TAB_INPUTS;
			if(tab == 2)
				return TAB_OUTPUTS;
			return tab;
		}

		private int uiTabToGlobalTab(int tab)
		{
			if(tab == TAB_PARAMETERS)
				return 0;
			if(tab == TAB_INPUTS)
				return 1;
			if(tab == TAB_OUTPUTS)
				return 2;
			if(tab == TAB_INTERNALS)
				return 2;
			if(tab == TAB_STATES)
				return 3;
			if(tab == TAB_TRANSITIONS)
				return 4;
			return tab;
		}

		private int[] selectedRowsForDelete(JTable table)
		{
			int[] rows = table.getSelectedRows();
			if(rows.length > 0)
				return rows;
			if(table.getSelectedRow() >= 0)
				return new int[] { table.getSelectedRow() };
			if(table.getEditingRow() >= 0)
				return new int[] { table.getEditingRow() };
			int leadRow = table.getSelectionModel().getLeadSelectionIndex();
			if(leadRow >= 0 && leadRow < table.getRowCount())
				return new int[] { leadRow };
			return rows;
		}

		private int tableRowToGlobalRow(JTable table, int row)
		{
			int modelRow = table.convertRowIndexToModel(row);
			if(table.getModel() instanceof FilteredOutputTableModel)
				return ((FilteredOutputTableModel)table.getModel()).actualRow(modelRow);
			if(table.getModel() instanceof FilteredParameterTableModel)
				return ((FilteredParameterTableModel)table.getModel()).actualRow(modelRow);
			if(table.getModel() instanceof FilteredMachineTableModel)
				return ((FilteredMachineTableModel)table.getModel()).actualRow(modelRow);
			return modelRow;
		}

		private ObjAttribute tableRowToAttribute(JTable table, int row, LinkedList<ObjAttribute> list)
		{
			int modelRow = table.convertRowIndexToModel(row);
			if(table.getModel() instanceof FilteredOutputTableModel)
				return ((FilteredOutputTableModel)table.getModel()).attributeAt(modelRow);
			if(table.getModel() instanceof FilteredParameterTableModel)
				return ((FilteredParameterTableModel)table.getModel()).attributeAt(modelRow);
			if(table.getModel() instanceof FilteredMachineTableModel)
				return ((FilteredMachineTableModel)table.getModel()).attributeAt(modelRow);
			if(modelRow < 0 || modelRow >= list.size())
				return null;
			return list.get(modelRow);
		}

		private boolean hasFilteredGlobalRows(JTable table)
		{
			return table.getModel() instanceof FilteredOutputTableModel ||
				table.getModel() instanceof FilteredParameterTableModel ||
				table.getModel() instanceof FilteredMachineTableModel;
		}

		private void refreshGlobalPropertyViews()
		{
			if(GPTableInputs != null && GPTableInputs.getModel() instanceof MyTableModel)
				((MyTableModel)GPTableInputs.getModel()).fireTableDataChanged();
			if(outputTableModel != null)
				outputTableModel.fireTableDataChanged();
			if(internalTableModel != null)
				internalTableModel.fireTableDataChanged();
			if(parameterTableModel != null)
				parameterTableModel.fireTableDataChanged();
			if(machineTableModel != null)
				machineTableModel.fireTableDataChanged();
			if(GPTableState != null && GPTableState.getModel() instanceof MyTableModel)
				((MyTableModel)GPTableState.getModel()).fireTableDataChanged();
			if(GPTableTrans != null && GPTableTrans.getModel() instanceof MyTableModel)
				((MyTableModel)GPTableTrans.getModel()).fireTableDataChanged();
		}

		private void syncDerivedOutputOrder()
		{
			syncDerivedOutputOrder(globalLists.get(3));
			syncDerivedOutputOrder(globalLists.get(4));
		}

		private void syncDerivedOutputOrder(LinkedList<ObjAttribute> derivedList)
		{
			LinkedList<ObjAttribute> reordered = new LinkedList<ObjAttribute>();
			LinkedList<ObjAttribute> remaining = new LinkedList<ObjAttribute>(derivedList);

			for(int i = 0; i < derivedList.size(); i++)
			{
				ObjAttribute attr = derivedList.get(i);
				if(!attr.getType().equals("output"))
				{
					reordered.add(attr);
					remaining.remove(attr);
				}
			}

			for(int i = 0; i < globalLists.get(2).size(); i++)
			{
				String outputName = globalLists.get(2).get(i).getName();
				for(int j = 0; j < remaining.size(); j++)
				{
					ObjAttribute attr = remaining.get(j);
					if(attr.getType().equals("output") && attr.getName().equals(outputName))
					{
						reordered.add(attr);
						remaining.remove(j);
						j--;
					}
				}
			}

			reordered.addAll(remaining);
			derivedList.clear();
			derivedList.addAll(reordered);
		}

		private void removeOutputBackReferences(ObjAttribute obj)
		{
			drawArea.removeOutputAttributeEverywhere(obj.getName());
		}

		private boolean isOutputEditorTab(int tab)
		{
			return tab == TAB_OUTPUTS || tab == TAB_INTERNALS;
		}

		//GEN-FIRST:event_GPNewActionPerformed
		private void GPOption1ActionPerformed(java.awt.event.ActionEvent evt) {
			
			if(currTable.isEditing())
				currTable.getCellEditor().stopCellEditing();
			
			int tabAtDelete = currTab;
			int[] rows = selectedRowsForDelete(currTable);
			int globalTab = uiTabToGlobalTab(tabAtDelete);
			LinkedList<ObjAttribute> list = globalLists.get(globalTab);
			LinkedList<ObjAttribute> selectedAttributes = new LinkedList<ObjAttribute>();
			for(int i = 0; i < rows.length; i++)
			{
				ObjAttribute attr = tableRowToAttribute(currTable, rows[i], list);
				if(attr != null)
					selectedAttributes.add(attr);
			}
			for(int i = selectedAttributes.size() - 1; i > -1; i--)
			{
				ObjAttribute obj = selectedAttributes.get(i);
				if(isOutputEditorTab(tabAtDelete) || obj.getEditable(0) != ObjAttribute.ABS)
				{
					if((obj.getName().equals("reset_signal") || obj.getName().equals("reset_state")) && tabAtDelete == TAB_MACHINE)
						GPOption3.setEnabled(true);
					if(obj.getName().equals("graycode") && tabAtDelete == TAB_TRANSITIONS)
						GPOption3.setEnabled(true);
					if(obj.getName().equals("priority") && tabAtDelete == TAB_TRANSITIONS)
						GPOption5.setEnabled(true);
					
					//if output being deleted, delete in states and trans
					if(isOutputEditorTab(tabAtDelete))
						removeOutputBackReferences(obj);
					if(obj.getType().equals("output") && checkGlobalName(globalLists.get(2), obj.getName()) && !isOutputEditorTab(tabAtDelete))
					{
						JOptionPane.showMessageDialog(this,
		                        "Must remove from outputs tab",
		                        "error",
		                        JOptionPane.ERROR_MESSAGE);
					}
					else
						removeGlobalAttribute(list, obj);
				}
				else
				{
					JOptionPane.showMessageDialog(this,
	                        "Row cannot be removed",
	                        "error",
	                        JOptionPane.ERROR_MESSAGE);
				}
				currTable.revalidate();	
			}
			refreshGlobalPropertyViews();
				

		}//GEN-LAST:event_GPNewActionPerformed

		private void removeGlobalAttribute(LinkedList<ObjAttribute> list, ObjAttribute obj) {
			if(list.remove(obj))
				return;
			for(int i = list.size() - 1; i >= 0; i--)
			{
				ObjAttribute candidate = list.get(i);
				if(candidate.getName().equals(obj.getName()) && candidate.getType().equals(obj.getType()))
				{
					list.remove(i);
					return;
				}
			}
		}

		//GEN-FIRST:event_GPDeleteActionPerformed
		private void GPOption2ActionPerformed(java.awt.event.ActionEvent evt) {
			
			ObjAttribute newObj = new ObjAttribute("",isOutputEditorTab(currTab) ? "0" : "",ObjAttribute.NO,"","",Color.black,"","",editable);
			int tab1 = uiTabToGlobalTab(GPTabbedPane.getSelectedIndex());
			if(currTab == TAB_INTERNALS)
				OutputAttributeFilter.setInternal(newObj, true);
			globalLists.get(tab1).addLast(newObj);
			refreshGlobalPropertyViews();
			if(isOutputEditorTab(currTab))
				currTable.setValueAt("regdp", currTable.getRowCount()-1, 3);
			
			currTable.revalidate();


		}//GEN-LAST:event_GPDeleteActionPerformed
		
		private void GPOption3ActionPerformed(java.awt.event.ActionEvent evt) {
			if(currTab == TAB_MACHINE)
			{
				
				if(!checkNames(currTable,"reset_signal"))
				{
					globalLists.get(0).add(new ObjAttribute("reset_signal", FizzimGui.getDefaultResetName(), 0, FizzimGui.getDefaultResetEdge(),"",Color.black,"","",
                                        editable2));
				}
				if(!checkGlobalName(globalLists.get(1), FizzimGui.getDefaultResetName()))
				{
					globalLists.get(1).add(new ObjAttribute(FizzimGui.getDefaultResetName(), "", 0, "","",Color.black,"","",
                                        editable));
				}
				if(!checkNames(currTable,"reset_state"))
				{
					globalLists.get(0).add(new ObjAttribute("reset_state", "state0", 0, "","",Color.black,"","",
							editable2));
				}
				
				GPOption3.setEnabled(false);
				currTable.revalidate();
			}
			if(currTab == TAB_INPUTS)
			{
				globalLists.get(1).add(new ObjAttribute("in", "", 0, "","",Color.black,"","",
					editable));

				refreshGlobalPropertyViews();
				currTable.revalidate();
			}
			if(currTab == TAB_PARAMETERS)
			{
				globalLists.get(0).add(new ObjAttribute("PARAM", "0", 1, "parameter","",Color.black,"","",
					editable));
				refreshGlobalPropertyViews();
				currTable.revalidate();
			}
			if(currTab == TAB_OUTPUTS)
			{
				globalLists.get(2).add(new ObjAttribute("out", "0", 2, "","",Color.black,"","",
					editable));
				refreshGlobalPropertyViews();
				GPTableOutputs.setValueAt("regdp", GPTableOutputs.getRowCount()-1, 3);

				currTable.revalidate();
			}	
			if(currTab == TAB_INTERNALS)
			{
				ObjAttribute newObj = new ObjAttribute("internal", "0", 2, "","",Color.black,"suppress_portlist","",
					editable);
				globalLists.get(2).add(newObj);
				refreshGlobalPropertyViews();
				GPTableInternals.setValueAt("regdp", GPTableInternals.getRowCount()-1, 3);

				currTable.revalidate();
			}	
			if(currTab == TAB_TRANSITIONS)
			{
				if(!checkNames(currTable,"graycode"))
				{
					globalLists.get(4).add(new ObjAttribute("graycode", "", 1,
							"","",Color.black,"","",
                                                        editable));
				}
				GPOption3.setEnabled(false);
				currTable.revalidate();
			}
		
		
		}
		

		private void removeAttribute(int tab, String name)
		{
			for(int i = 0; i < globalLists.get(tab).size(); i++)
			{
				ObjAttribute obj = globalLists.get(tab).get(i);
				if(obj.getName().equals(name) && obj.getType().equals("output"))
					globalLists.get(tab).remove(i);
					
			}
		}

		private boolean checkGlobalName(LinkedList<ObjAttribute> list, String name) {
			for(int i = 0; i < list.size(); i++)
			{
				if(list.get(i).getName().equals(name))
					return true;
			}
			return false;
		}

		private void GPOption4ActionPerformed(java.awt.event.ActionEvent evt) {
			

			if(currTab == TAB_INPUTS)
			{
				globalLists.get(1).add(new ObjAttribute("in[1:0]", "", 0, "","",Color.black,"","",
					editable));

				refreshGlobalPropertyViews();
				currTable.revalidate();
			}
			if(currTab == TAB_OUTPUTS)
			{
				globalLists.get(2).add(new ObjAttribute("out[1:0]", "0", 2, "","",Color.black,"","",
					editable));
				refreshGlobalPropertyViews();
				currTable.setValueAt("regdp", currTable.getRowCount()-1, 3);

				currTable.revalidate();
			}	
			if(currTab == TAB_INTERNALS)
			{
				globalLists.get(2).add(new ObjAttribute("internal[1:0]", "0", 2, "","",Color.black,"suppress_portlist","",
					editable));
				refreshGlobalPropertyViews();
				currTable.setValueAt("regdp", currTable.getRowCount()-1, 3);

				currTable.revalidate();
			}	
			if(currTab == TAB_TRANSITIONS)
			{
				globalLists.get(4).add(new ObjAttribute("", "", 1,"output", "",Color.black,"","",
                                editable));
				
				currTable.revalidate();
			}
			
		}
		
		private void GPOption5ActionPerformed(java.awt.event.ActionEvent evt) {
			
			if(currTab == TAB_INTERNALS)
			{
				globalLists.get(2).add(new ObjAttribute("flag", "", 2, "","",Color.black,"suppress_portlist","",
					editable));
				refreshGlobalPropertyViews();
				currTable.setValueAt("flag", currTable.getRowCount()-1, 3);

				currTable.revalidate();
			}	
			if(currTab == TAB_TRANSITIONS)
			{
				if(!checkNames(currTable,"priority"))
				{
					globalLists.get(4).add(new ObjAttribute("priority", "1000", 1, "", "",Color.black,"","",
                                        editable));
				}
				GPOption5.setEnabled(false);
				currTable.revalidate();
			}	
		}

		private void GPOption6ActionPerformed(java.awt.event.ActionEvent evt) {
			if(currTab == TAB_OUTPUTS)
				moveSelectedOutputsBetweenPortListModes(true);
			else if(currTab == TAB_INTERNALS)
				moveSelectedOutputsBetweenPortListModes(false);
		}

		private void moveSelectedOutputsBetweenPortListModes(boolean makeInternal) {
			if(!(currTable.getModel() instanceof FilteredOutputTableModel))
				return;
			if(currTable.isEditing())
				currTable.getCellEditor().stopCellEditing();
			int[] rows = selectedRowsForDelete(currTable);
			if(rows.length == 0)
				return;

			LinkedList<ObjAttribute> moved = new LinkedList<ObjAttribute>();
			for(int i = 0; i < rows.length; i++)
			{
				int actualRow = tableRowToGlobalRow(currTable, rows[i]);
				ObjAttribute attr = globalLists.get(2).get(actualRow);
				OutputAttributeFilter.setInternal(attr, makeInternal);
				moved.add(attr);
			}

			refreshGlobalPropertyViews();
			GPTabbedPane.setSelectedIndex(makeInternal ? TAB_INTERNALS : TAB_OUTPUTS);
			currTable = makeInternal ? GPTableInternals : GPTableOutputs;
			currTab = makeInternal ? TAB_INTERNALS : TAB_OUTPUTS;
			setTable(currTab);
			selectMovedOutputRows(moved);
		}

		private void selectMovedOutputRows(LinkedList<ObjAttribute> moved) {
			if(!(currTable.getModel() instanceof FilteredOutputTableModel))
				return;
			FilteredOutputTableModel model = (FilteredOutputTableModel)currTable.getModel();
			currTable.clearSelection();
			for(int i = 0; i < model.getRowCount(); i++)
			{
				ObjAttribute attr = globalLists.get(2).get(model.actualRow(i));
				if(moved.contains(attr))
					currTable.addRowSelectionInterval(i, i);
			}
		}

		private void GPUpActionPerformed(java.awt.event.ActionEvent evt) {
			if(hasFilteredGlobalRows(currTable))
				moveSelectedFilteredGlobalRows(-1);
			else
				AttributeTableReorder.moveSelectedRows(currTable, globalLists.get(uiTabToGlobalTab(currTab)), -1);
		}

		private void GPDownActionPerformed(java.awt.event.ActionEvent evt) {
			if(hasFilteredGlobalRows(currTable))
				moveSelectedFilteredGlobalRows(1);
			else
				AttributeTableReorder.moveSelectedRows(currTable, globalLists.get(uiTabToGlobalTab(currTab)), 1);
		}

		private void moveSelectedFilteredGlobalRows(int direction) {
			if(currTable.isEditing())
				currTable.getCellEditor().stopCellEditing();
			int[] rows = selectedRowsForDelete(currTable);
			if(rows.length == 0)
				return;
			Arrays.sort(rows);
			int targetViewRow = direction < 0 ? rows[0] - 1 : rows[rows.length - 1] + 1;
			if(targetViewRow < 0 || targetViewRow >= currTable.getRowCount())
				return;

			LinkedList<ObjAttribute> list = globalLists.get(uiTabToGlobalTab(currTab));
			int[] actualRows = new int[rows.length];
			for(int i = 0; i < rows.length; i++)
				actualRows[i] = tableRowToGlobalRow(currTable, rows[i]);
			int targetActualRow = tableRowToGlobalRow(currTable, targetViewRow);
			LinkedList<ObjAttribute> moved = new LinkedList<ObjAttribute>();
			for(int i = 0; i < actualRows.length; i++)
				moved.add(list.get(actualRows[i]));
			for(int i = actualRows.length - 1; i >= 0; i--)
				list.remove(actualRows[i]);

			int insertRow = targetActualRow;
			if(direction > 0)
				insertRow = targetActualRow - actualRows.length + 1;
			for(int i = 0; i < moved.size(); i++)
				list.add(insertRow + i, moved.get(i));

			if(currTable.getModel() instanceof FilteredOutputTableModel)
				syncDerivedOutputOrder();
			refreshGlobalPropertyViews();
			currTable.clearSelection();
			selectVisibleRowsForAttributes(moved);
		}

		private void selectVisibleRowsForAttributes(LinkedList<ObjAttribute> moved) {
			for(int i = 0; i < currTable.getRowCount(); i++) {
				ObjAttribute attr = tableRowToAttribute(currTable, i, globalLists.get(uiTabToGlobalTab(currTab)));
				if(moved.contains(attr))
					currTable.addRowSelectionInterval(i, i);
			}
		}
		
		
		private boolean checkNames(JTable currTable2, String string) {
			for(int i = 0; i < currTable2.getRowCount(); i++)
			{
				if(currTable2.getValueAt(i,0).equals(string))
					return true;
			}
			return false;
		}
		
		
		//GEN-FIRST:event_GPOKActionPerformed
		private void GPOKActionPerformed(java.awt.event.ActionEvent evt) {
			stopTableEditing(GPTableMachine);
			stopTableEditing(GPTableState);
			stopTableEditing(GPTableTrans);
			stopTableEditing(GPTableInputs);
			stopTableEditing(GPTableParameters);
			stopTableEditing(GPTableOutputs);
			stopTableEditing(GPTableInternals);
			if(!checkReservedGlobalNames())
				return;
			int error = 0;
			for(int i = 0; i < globalLists.size(); i++)
			{
				for(int j = 0; j < globalLists.get(i).size(); j++)
				{
					if(i == 2 && !globalLists.get(i).get(j).getType().equals("reg") && !globalLists.get(i).get(j).getType().equals("comb") && !globalLists.get(i).get(j).getType().equals("regdp") && !globalLists.get(i).get(j).getType().equals("flag"))
						error = 2;
					for(int k = j+1; k < globalLists.get(i).size(); k++)
					{
						if(globalLists.get(i).get(j).getName().equals(globalLists.get(i).get(k).getName()))
							error = 1;
					}
				}
			}
			if(error == 0)
			{
			syncDerivedOutputOrder();
			drawArea.updateStates();
			drawArea.updateTrans();
			drawArea.updateGlobalTable();
			drawArea.commitUndo();
			dispose();
			}
			else if(error == 1)
			{
				JOptionPane.showMessageDialog(this,
                        "Two rows cannot contain the same name",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
			}
			else if(error == 2)
			{
				JOptionPane.showMessageDialog(this,
                        "An output must have a type set",
                        "error",
                        JOptionPane.ERROR_MESSAGE);
			}
	
		}//GEN-LAST:event_GPOKActionPerformed

		private void stopTableEditing(JTable table) {
			if(table != null && table.isEditing())
				table.getCellEditor().stopCellEditing();
		}

		private boolean checkReservedGlobalNames() {
			for(int i = 0; i < globalLists.get(0).size(); i++)
			{
				ObjAttribute obj = globalLists.get(0).get(i);
				if(obj.getName().equals("name") && VerilogNameValidator.showReservedWordError(this, obj.getValue(), "module name"))
					return false;
				if(obj.getType().equals("parameter") && VerilogNameValidator.showReservedWordError(this, VerilogNameValidator.parameterIdentifier(obj.getName()), "parameter name"))
					return false;
			}
			for(int i = 0; i < globalLists.get(1).size(); i++)
			{
				if(VerilogNameValidator.showReservedWordError(this, globalLists.get(1).get(i).getName(), "input name"))
					return false;
			}
			for(int i = 0; i < globalLists.get(2).size(); i++)
			{
				if(VerilogNameValidator.showReservedWordError(this, globalLists.get(2).get(i).getName(), "output name"))
					return false;
			}
			return true;
		}

		//GEN-FIRST:event_GPCancelActionPerformed
		private void GPCancelActionPerformed(java.awt.event.ActionEvent evt) {
			drawArea.cancel();
			dispose();
		}//GEN-LAST:event_GPCancelActionPerformed



		//GEN-BEGIN:variables
		// Variables declaration - do not modify
		private javax.swing.JButton GPCancel;
		private javax.swing.JButton GPOption1;
		private javax.swing.JButton GPOption2;
		private javax.swing.JButton GPOption3;
		private javax.swing.JButton GPOption4;
		private javax.swing.JButton GPOption5;
		private javax.swing.JButton GPOption6;
		private javax.swing.JButton GPDown;
		private javax.swing.JLabel GPLabel;
		private javax.swing.JLabel GPLabel2;
		private javax.swing.JButton GPOK;
		private javax.swing.JButton GPUp;
		private javax.swing.JScrollPane GPScrollMachine;
		private javax.swing.JScrollPane GPScrollParameters;
		private javax.swing.JScrollPane GPScrollState;
		private javax.swing.JScrollPane GPScrollTrans;
		private javax.swing.JScrollPane GPScrollInputs;
		private javax.swing.JScrollPane GPScrollOutputs;
		private javax.swing.JScrollPane GPScrollInternals;
		private javax.swing.JTabbedPane GPTabbedPane;
		private javax.swing.JTable GPTableMachine;
		private javax.swing.JTable GPTableParameters;
		private javax.swing.JTable GPTableState;
		private javax.swing.JTable GPTableTrans;
		private javax.swing.JTable GPTableInputs;
		private javax.swing.JTable GPTableOutputs;
		private javax.swing.JTable GPTableInternals;
		// End of variables declaration//GEN-END:variables
		
	}

public class Properties extends javax.swing.JDialog {


        

}
