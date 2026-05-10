import java.awt.*;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.IOException;
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

public class LoopbackTransitionObj extends TransitionObj  implements Cloneable {

	private int selectStatus = 0;
	
	public Point startPt, endPt, startCtrlPt,endCtrlPt;
	public int startStateIndex,endStateIndex;
	public StateObj state;
	public CubicCurve2D.Double loop;
	private Vector<Point> stateBorderPts;
	private int x,y;
	private boolean ready = false;
	String startS;
	

	
	public static final int NONE = 0;
	public static final int START = 1;
	public static final int STARTCTRL = 2;
	public static final int ENDCTRL = 3;
	public static final int END = 4;
	public static final int ALL = 5;
	public static final int TXT = 6;
	private static final int HANDLE_HIT_RADIUS = 10;
	
private int xTemp, yTemp, tempSI,tempEI,lengthS,lengthE;

private double ctrlAngleS, ctrlAngleE;
private Point bendStartCtrlPt, bendEndCtrlPt;
private Point bendStartPt, bendEndPt;
private int bendEndpoint = NONE;
private static final double BEND_ENDPOINT_PULL = 0.25;
	
	public LoopbackTransitionObj(int _x, int _y, int numb, int page, Color c)
	{
		objName = "trans" + numb;
		x = _x;
		y = _y;
		myPage = page;
		color = c;
	}
	
	public LoopbackTransitionObj(Point sp, Point ep, Point scp, Point ecp,
			LinkedList<ObjAttribute> newList,
			String name, String start, String end, int sIndex, int eIndex, int page, Color c) {
		startPt = sp;
		endPt = ep;
		startCtrlPt = scp;
		endCtrlPt = ecp;
		startStateIndex = sIndex;
		endStateIndex = eIndex;
		attrib = newList;
		objName = name;
		loop = new CubicCurve2D.Double(startPt.getX(),startPt.getY(),startCtrlPt.getX(),startCtrlPt.getY(),endCtrlPt.getX(),endCtrlPt.getY(),endPt.getX(),endPt.getY());
		ready = true;
		startS = start;
		myPage = page;
		color = c;
	}

	public void initTrans(StateObj _state)
	{
		if(_state != state)
		{
			state = _state;
			setEndPts(x,y);
			loop = new CubicCurve2D.Double(startPt.getX(),startPt.getY(),startCtrlPt.getX(),startCtrlPt.getY(),endCtrlPt.getX(),endCtrlPt.getY(),endPt.getX(),endPt.getY());
			ready = true;
		}			
	}

	public void resetRoute()
	{
		StateObj currentState = state;
		state = null;
		initTrans(currentState);
		modified = true;
	}

	private boolean nearPoint(Point point, int x, int y)
	{
		return Math.abs(point.getX() - x) <= HANDLE_HIT_RADIUS && Math.abs(point.getY() - y) <= HANDLE_HIT_RADIUS;
	}

	private int nearestBorderIndex(Vector<Point> borderPts, Point target)
	{
		double bestScore = Double.MAX_VALUE;
		int bestIndex = 0;
		for(int i = 0; i < borderPts.size(); i++)
		{
			double score = borderSnapScore(borderPts.get(i), target);
			if(score < bestScore)
			{
				bestScore = score;
				bestIndex = i;
			}
		}
		return bestIndex;
	}

	private double borderSnapScore(Point borderPt, Point target)
	{
		if(state.getType() == 4)
			return target.distanceSq(borderPt);

		int[] coords = state.getCoords();
		int x0 = coords[0];
		int y0 = coords[1];
		int x1 = coords[2];
		int y1 = coords[3];
		int w = Math.max(1, x1 - x0);
		int h = Math.max(1, y1 - y0);
		double score = target.distanceSq(borderPt);
		boolean nearLeft = Math.abs(borderPt.x - x0) <= 2;
		boolean nearRight = Math.abs(borderPt.x - x1) <= 2;
		boolean nearTop = Math.abs(borderPt.y - y0) <= 2;
		boolean nearBottom = Math.abs(borderPt.y - y1) <= 2;
		if((nearLeft || nearRight) && (nearTop || nearBottom))
			score += 100000;
		if(nearTop || nearBottom)
			score += edgeFractionPenalty((double)(borderPt.x - x0) / w) * 1200;
		if(nearLeft || nearRight)
			score += edgeFractionPenalty((double)(borderPt.y - y0) / h) * 1200;
		return score;
	}

	private double edgeFractionPenalty(double fraction)
	{
		double a = Math.abs(fraction - 0.25);
		double b = Math.abs(fraction - 0.50);
		double c = Math.abs(fraction - 0.75);
		return Math.min(a, Math.min(b, c));
	}

	private double getBorderPointAngle(Point point)
	{
		Point center = state.getRealCenter(myPage);
		return Math.atan2(point.getY() - center.getY(), point.getX() - center.getX());
	}

	public boolean isRouteHandleHit(int x, int y)
	{
		return currPage == myPage && (nearPoint(startPt, x, y) || nearPoint(startCtrlPt, x, y)
				|| nearPoint(endCtrlPt, x, y) || nearPoint(endPt, x, y));
	}

	private boolean isRouteLineHit(int x, int y)
	{
		return currPage == myPage && new BasicStroke(10).createStrokedShape(loop).contains(new Point2D.Double(x,y));
	}

	public boolean isRouteEditHit(int x, int y)
	{
		return isRouteHandleHit(x, y) || isRouteLineHit(x, y);
	}

	private int nearestEndpointTo(int x, int y)
	{
		Point click = new Point(x, y);
		return click.distanceSq(startPt) <= click.distanceSq(endPt) ? START : END;
	}

	private void moveAttributeTextOffsets(int dx, int dy)
	{
		if(attrib == null)
			return;
		for(int i = 0; i < attrib.size(); i++)
			attrib.get(i).moveTextOffset(dx, dy);
	}

	private boolean endpointGeometrySelected()
	{
		int status = state.getSelectStatus();
		return status == StateObj.CENTER || status == StateObj.TL || status == StateObj.TR
				|| status == StateObj.BL || status == StateObj.BR;
	}
	
	@SuppressWarnings("unchecked")
	public Object clone () 
    throws CloneNotSupportedException
    {
		LoopbackTransitionObj copy = (LoopbackTransitionObj)super.clone();
		copy.loop = (CubicCurve2D.Double)loop.clone();
		copy.startPt = (Point) startPt.clone();
		copy.endPt = (Point) endPt.clone();
		copy.startCtrlPt = (Point) startCtrlPt.clone();
		copy.endCtrlPt = (Point) endCtrlPt.clone();
		copy.stateBorderPts = (Vector<Point>) stateBorderPts.clone();
        if(attrib != null)
		{
    		copy.attrib = (LinkedList<ObjAttribute>)copy.attrib.clone();
    		for(int i = 0; i < attrib.size(); i++)
    		{
    			copy.attrib.set(i,(ObjAttribute)attrib.get(i).clone());
    		}
		}
		return copy;	
    }
	
	private void setEndPts(int x, int y)
	{
		//find start point on oval closest to click point
		stateBorderPts = state.getBorderPts();
		Point createPt = new Point(x,y);
		startStateIndex = nearestBorderIndex(stateBorderPts, createPt);
		
		// store two points on oval
		startPt = stateBorderPts.get(startStateIndex);
		Point center = state.getRealCenter(myPage);
		Point oppositePt = new Point((int)(center.x - (startPt.x - center.x)), (int)(center.y - (startPt.y - center.y)));
		endStateIndex = nearestBorderIndex(stateBorderPts, oppositePt);
		endPt = stateBorderPts.get(endStateIndex);
		
		//angle control points are from points on oval
		double angleStart = getBorderPointAngle(startPt);
		double angleEnd = getBorderPointAngle(endPt);
		
		
		//find distance from points on oval to control point
		int dist = (int) Math.sqrt(state.getSize());

		//scaling
		dist = (int) (dist*.65);


		
		//set up control points
		startCtrlPt = new Point();
		endCtrlPt = new Point(); 
		startCtrlPt.setLocation((int) (dist*Math.cos(angleStart)) + startPt.getX(), dist*Math.sin(angleStart) + startPt.getY());
		endCtrlPt.setLocation((int) (dist*Math.cos(angleEnd)) + endPt.getX(), dist*Math.sin(angleEnd) + endPt.getY());
		
		
	}
	
	public void paintComponent(Graphics g) {
		
		if(ready && currPage == myPage)
		{

			Graphics2D g2D = (Graphics2D) g;
			Stroke oldStroke = g2D.getStroke();
			if(isLintHighlighted())
			{
				g2D.setStroke(new BasicStroke(6.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
			else if(isHighestPriority())
			{
				float width = oldStroke instanceof BasicStroke ? ((BasicStroke)oldStroke).getLineWidth() : 1.0f;
				g2D.setStroke(new BasicStroke(Math.max(2.5f, width + 1.5f)));
			}
			Color drawColor = getDrawColor();
			g2D.setColor(isLintHighlighted() ? new Color(255, 140, 0) : drawColor);
			if(isHoverHighlighted())
			{
				g2D.setColor(new Color(65, 145, 220));
				g2D.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			}
			g2D.draw(loop);
			
			
			//draw arrow head
			
			// find angle between end point and end control point
			double alpha = getAngle(endCtrlPt,endPt);
	
			
	
			double adj = Math.PI/6;
	        //g2D.drawLine((int)endPt.getX(),(int)endPt.getY(),(int)endPt.getX() + (int)(arrowLength*Math.cos(alpha + adj)),(int)endPt.getY() - (int)(arrowLength*Math.sin(alpha + adj)));
	        //g2D.drawLine((int)endPt.getX(),(int)endPt.getY(),(int)endPt.getX() + (int)(arrowLength*Math.cos(alpha - adj)),(int)endPt.getY() - (int)(arrowLength*Math.sin(alpha - adj)));
			int[] xP = {(int)endPt.getX(),(int)endPt.getX() + (int)(13*Math.cos(alpha + adj)),(int)endPt.getX() + (int)(13*Math.cos(alpha - adj))};
			int[] yP = {(int)endPt.getY(),(int)endPt.getY() - (int)(13*Math.sin(alpha + adj)),(int)endPt.getY() - (int)(13*Math.sin(alpha - adj))};
			g2D.drawPolygon(xP,yP,3);
			g2D.fillPolygon(xP,yP,3);
			
			/*
			int eX = (int)endPt.getX();
			int eY = (int)endPt.getY();
			int dx = (int)endCtrlPt.getX() - eX;
			int dy = eY - (int)endCtrlPt.getY(); 
			double theta = 0;
			if(dx == 0)
			{
				if(dy >= 0)
					theta = Math.PI/2;
				else
					theta = 3*Math.PI/2;
			}
			else if (dx > 0 && dy >= 0)
				theta = Math.atan(dy/dx);
			else if (dx > 0 && dy <= 0)
				theta = 2*Math.PI + Math.atan(dy/dx);
			else if (dx < 0 && dy >= 0)
				theta = Math.PI + Math.atan(dy/dx);
			else if (dx < 0 && dy <= 0)
				theta = Math.PI + Math.atan(dy/dx);
			
			double adj = Math.PI/6;
	        g2D.drawLine(eX,eY,eX + (int)(20*Math.cos(theta + adj)),eY - (int)(20*Math.sin(theta + adj)));
	        g2D.drawLine(eX,eY,eX + (int)(20*Math.cos(theta - adj)),eY - (int)(20*Math.sin(theta - adj)));
			*/
			
	        //draw control points
			if(selectStatus != NONE)
	        {
	            g2D.setColor(Color.red);
	            g2D.fillRect((int)startPt.getX()-3,(int)startPt.getY()-3,7,7);
	            g2D.fillRect((int)endPt.getX()-3,(int)endPt.getY()-3,7,7);
	            g2D.fillRect((int)startCtrlPt.getX()-3,(int)startCtrlPt.getY()-3,7,7);
	            g2D.fillRect((int)endCtrlPt.getX()-3,(int)endCtrlPt.getY()-3,7,7);
	            g2D.drawLine((int)startPt.getX(),(int)startPt.getY(),(int)startCtrlPt.getX(),(int)startCtrlPt.getY());
	            g2D.drawLine((int)endPt.getX(),(int)endPt.getY(),(int)endCtrlPt.getX(),(int)endCtrlPt.getY());


	        }
			g2D.setStroke(oldStroke);


		}
		
	}

	@Override
	public void adjustShapeOrPosition(int x, int y) {

		if(currPage == myPage)
		{
			if(selectStatus == START)
			{
				Point oldPt = new Point(startPt);
				Point currPt = new Point(x,y);
				startStateIndex = nearestBorderIndex(stateBorderPts, currPt);
				startPt.setLocation(stateBorderPts.get(startStateIndex).getX(),stateBorderPts.get(startStateIndex).getY());
				moveControlWithEndpoint(startCtrlPt, oldPt, startPt);
			}
			if(selectStatus == STARTCTRL)
				startCtrlPt.setLocation(x,y);
			if(selectStatus == ENDCTRL)
				endCtrlPt.setLocation(x,y);
			if(selectStatus == END)
			{
				Point oldPt = new Point(endPt);
				Point currPt = new Point(x,y);
				endStateIndex = nearestBorderIndex(stateBorderPts, currPt);
				endPt.setLocation(stateBorderPts.get(endStateIndex).getX(),stateBorderPts.get(endStateIndex).getY());
				moveControlWithEndpoint(endCtrlPt, oldPt, endPt);
			}
			if(selectStatus == TXT)
			{
				if(attrib != null)
				{
					for (int i = 0; i < attrib.size(); i++)
					{
						ObjAttribute s = attrib.get(i);
						if(s.getSelectStatus() != 0)
						{
							s.adjustShapeOrPosition(x, y);
							break;
						}
					}
				} 
			}
			
			// Bend the loop by dragging the visible curve.
			if(selectStatus == 5)
			{
				if(bendStartCtrlPt != null && bendEndCtrlPt != null)
				{
					int dx = x - xTemp;
					int dy = y - yTemp;
					startCtrlPt.setLocation(startCtrlPt.x + dx, startCtrlPt.y + dy);
					endCtrlPt.setLocation(endCtrlPt.x + dx, endCtrlPt.y + dy);
					moveBendEndpointToward(x, y);
					xTemp = x;
					yTemp = y;
				}
			}

			
			loop.setCurve(startPt.getX(),startPt.getY(),startCtrlPt.getX(),startCtrlPt.getY(),endCtrlPt.getX(),endCtrlPt.getY(),endPt.getX(),endPt.getY());
			

				modified = true;
		}
	}

	@Override
	public int getSelectStatus() {
		return selectStatus;
	}

	@Override
	public boolean setSelectStatus(int x, int y) {

		if(currPage == myPage)
		{
			bendEndpoint = NONE;
			bendStartPt = null;
			bendEndPt = null;
			if(selectStatus != 5 || !loop.contains(x,y))
			{
				selectStatus = NONE;
				xTemp = x;
				yTemp = y;
				tempSI = startStateIndex;
				tempEI = endStateIndex;
				//ctrlAngleS = getAngle(startCtrlPt,startPt)-(36-startStateIndex)*2*Math.PI/36;
				//ctrlAngleE = getAngle(endCtrlPt,endPt)-(36-endStateIndex)*2*Math.PI/36;
				ctrlAngleS = getAngle(startCtrlPt,startPt);
				ctrlAngleE = getAngle(endCtrlPt,endPt);
				lengthS = (int)Math.round(startCtrlPt.distance(startPt));
				lengthE = (int)Math.round(endCtrlPt.distance(endPt));

			}

			//check for txt
	        if(attrib != null)
			{
				for(int j = 0; j < attrib.size(); j++)
				{
					ObjAttribute s = attrib.get(j);
					s.unselect();
				}
	        	for (int i = 0; i < attrib.size(); i++)
				{
	        		ObjAttribute s = attrib.get(i);
					if(s.setSelectStatus(x,y))
					{
						selectStatus = TXT;
						break;
					}
				}
			}
	        if(selectStatus != TXT)
	        {
				
				
				//check control points
				if(nearPoint(startPt, x, y))
					selectStatus = START;
				if(nearPoint(startCtrlPt, x, y))
		        	selectStatus = STARTCTRL;
		        if(nearPoint(endCtrlPt, x, y))
		        	selectStatus = ENDCTRL;
		        if(nearPoint(endPt, x, y))
		        	selectStatus = END;
				// if not a control point, search around line
		        if(selectStatus == NONE)
			//	for(int i = -4; i < 5; i++)
				//{
					//for(int j = -4; j < 5; j++)
					//{
						if(isRouteLineHit(x,y))
						{
							selectStatus = 5;
							bendEndpoint = nearestEndpointTo(x, y);
							bendStartCtrlPt = new Point(startCtrlPt);
							bendEndCtrlPt = new Point(endCtrlPt);
							bendStartPt = new Point(startPt);
							bendEndPt = new Point(endPt);
							//break;
						}
					//}
				//}
	        }
			if(selectStatus == NONE)
				return false;
			else
				return true;
		}
		else
			return false;
	}

	private void moveBendEndpointToward(int x, int y)
	{
		if(bendEndpoint == START)
		{
			Point basePt = bendStartPt == null ? startPt : bendStartPt;
			Point oldPt = new Point(startPt);
			Point currPt = easedEndpointTarget(basePt, x, y);
			startStateIndex = nearestBorderIndex(stateBorderPts, currPt);
			startPt.setLocation(stateBorderPts.get(startStateIndex).getX(), stateBorderPts.get(startStateIndex).getY());
			startCtrlPt.setLocation(startCtrlPt.x + startPt.x - oldPt.x, startCtrlPt.y + startPt.y - oldPt.y);
		}
		else if(bendEndpoint == END)
		{
			Point basePt = bendEndPt == null ? endPt : bendEndPt;
			Point oldPt = new Point(endPt);
			Point currPt = easedEndpointTarget(basePt, x, y);
			endStateIndex = nearestBorderIndex(stateBorderPts, currPt);
			endPt.setLocation(stateBorderPts.get(endStateIndex).getX(), stateBorderPts.get(endStateIndex).getY());
			endCtrlPt.setLocation(endCtrlPt.x + endPt.x - oldPt.x, endCtrlPt.y + endPt.y - oldPt.y);
		}
	}

	private Point easedEndpointTarget(Point basePt, int x, int y)
	{
		int targetX = (int)Math.round(basePt.x + (x - basePt.x) * BEND_ENDPOINT_PULL);
		int targetY = (int)Math.round(basePt.y + (y - basePt.y) * BEND_ENDPOINT_PULL);
		return new Point(targetX, targetY);
	}

	private void moveControlWithEndpoint(Point ctrlPt, Point oldEndpoint, Point newEndpoint)
	{
		ctrlPt.setLocation(ctrlPt.x + newEndpoint.x - oldEndpoint.x,
				ctrlPt.y + newEndpoint.y - oldEndpoint.y);
	}

	public int getType()
	{
		return 2;
	}
	public boolean isModified()
	{
		if(modified)
			return true;
		else
			return false;
		
	}

	//sets modified back to false
	public void setModifiedFalse()
	{

		modified = false;
	}
	public void setModifiedTrue()
	{
		modified = true;
	}
	
	public void updateObj()
	{
		int newPage = state.getPage();

		//moeve all related objects to new page
		if(newPage != myPage)
		{
			for(int i = 0; i < attrib.size();i++)
			{
				ObjAttribute obj = attrib.get(i);
				if(obj.getPage() == myPage)
					obj.setPage(newPage);
			}
			myPage = state.getPage();
		}
			
		
		if(isParentModified() || endpointGeometrySelected())
		{

			double angleS = getAngle(startCtrlPt,startPt);
			double angleE = getAngle(endCtrlPt,endPt);
			int lenS = (int)Math.round(startCtrlPt.distance(startPt));
			int lenE = (int)Math.round(endCtrlPt.distance(endPt));
			stateBorderPts = state.getBorderPts();
			startPt = stateBorderPts.get(startStateIndex);
			endPt = stateBorderPts.get(endStateIndex);
			
			
			startCtrlPt.setLocation((int) (lenS*Math.cos(angleS)) + startPt.getX(), -lenS*Math.sin(angleS) + startPt.getY());
			endCtrlPt.setLocation((int) (lenE*Math.cos(angleE)) + endPt.getX(), -lenE*Math.sin(angleE) + endPt.getY());
			
			
			loop.setCurve(startPt.getX(),startPt.getY(),startCtrlPt.getX(),startCtrlPt.getY(),endCtrlPt.getX(),endCtrlPt.getY(),endPt.getX(),endPt.getY());
		}
	}
	
	public void notifyChange(GeneralObj old, GeneralObj clone) {
		if(old.equals(state))
		{
			state = (StateObj) clone;
			
		}
		
	}
	
	public boolean isParentModified()
	{
		if(modifiedParent)
			return true;
		else
			return false;
		
	}


	
	public void unselect()
	{
		selectStatus = NONE;
	}
	
	public boolean containsParent(GeneralObj oldObj)
	{
		if(oldObj.equals(state))
			return true;
		else
			return false;
	}

	
	public Point getCenter(int page)
	{
		if(!ready)
			return new Point(0,0);
		return bezierPoint(startPt, startCtrlPt, endCtrlPt, endPt, 0.5);
	}

	private Point bezierPoint(Point p0, Point p1, Point p2, Point p3, double t)
	{
		double inv = 1.0 - t;
		double x = inv * inv * inv * p0.x
				+ 3 * inv * inv * t * p1.x
				+ 3 * inv * t * t * p2.x
				+ t * t * t * p3.x;
		double y = inv * inv * inv * p0.y
				+ 3 * inv * inv * t * p1.y
				+ 3 * inv * t * t * p2.y
				+ t * t * t * p3.y;
		return new Point((int)Math.round(x), (int)Math.round(y));
	}


	@Override
	public Point getStart() {
		// TODO make better
		return startCtrlPt;
	}
	public StateObj getStartState()
	{
		return state;
	}
	
	
	public void save(BufferedWriter writer) throws IOException {
		
		writer.write("## START LOOPBACK TRANSITION OBJECT\n");
		writer.write("<transition>\n");
		
		writer.write(i(1) + "<attributes>\n");
		for(int i = 0; i < attrib.size(); i++)
		{
			ObjAttribute obj = attrib.get(i);
			obj.save(writer,1);
		}
		writer.write(i(1) + "</attributes>\n");

		writer.write(i(1) + "<startState>\n" + i(1) + state.getName() + "\n" + i(1) + "</startState>\n");
		writer.write(i(1) + "<endState>\n" + i(1) + state.getName() + "\n" + i(1) + "</endState>\n");
		writer.write(i(1) + "<startPtX>\n" + i(1) + startPt.getX() + "\n" + i(1) + "</startPtX>\n");
		writer.write(i(1) + "<startPtY>\n" + i(1) + startPt.getY() + "\n" + i(1) + "</startPtY>\n");
		writer.write(i(1) + "<endPtX>\n" + i(1) + endPt.getX() + "\n" + i(1) + "</endPtX>\n");
		writer.write(i(1) + "<endPtY>\n" + i(1) + endPt.getY() + "\n" + i(1) + "</endPtY>\n");
		writer.write(i(1) + "<startCtrlPtX>\n" + i(1) + startCtrlPt.getX() + "\n" + i(1) + "</startCtrlPtX>\n");
		writer.write(i(1) + "<startCtrlPtY>\n" + i(1) + startCtrlPt.getY() + "\n" + i(1) + "</startCtrlPtY>\n");
		writer.write(i(1) + "<endCtrlPtY>\n" + i(1) + endCtrlPt.getX() + "\n" + i(1) + "</endCtrlPtY>\n");
		writer.write(i(1) + "<endCtrlPtY>\n" + i(1) + endCtrlPt.getY() + "\n" + i(1) + "</endCtrlPtY>\n");
		writer.write(i(1) + "<startStateIndex>\n" + i(1) + startStateIndex + "\n" + i(1) + "</startStateIndex>\n");
		writer.write(i(1) + "<endStateIndex>\n" + i(1) + endStateIndex + "\n" + i(1) + "</endStateIndex>\n");
		writer.write(i(1) + "<page>\n" + i(1) + myPage + "\n" + i(1) + "</page>\n");
		writer.write(i(1) + "<color>\n" + i(1) + color.getRGB() + "\n" + i(1) + "</color>\n");

		
		
		writer.write("</transition>\n");
		writer.write("## START LOOPBACK TRANSITION OBJECT\n");
	}

	public void makeConnections(Vector<Object> objList)
	{
		for(int i = 1; i < objList.size(); i++)
		{
			GeneralObj obj = (GeneralObj)objList.get(i);
			if(obj.getType() == 0)
			{
				if(obj.getName().equals(startS))
					state = (StateObj) obj;		
			}
		}
		stateBorderPts = state.getBorderPts();

	}

	@Override
	public boolean setBoxSelectStatus(int x0, int y0, int x1, int y1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateObjPages(int page) {
		myPage = page;
		if(attrib != null)
		{
			for(int i = 0; i < attrib.size(); i++)
			{
				ObjAttribute obj = attrib.get(i);
				obj.setPage(page);
			}
		}
		
		
	}

	

	
}
