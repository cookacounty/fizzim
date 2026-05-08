import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

// A state group is a one-level parent container around real states. It can carry
// shared state attributes and transitions, while the real states remain the
// encoded states in generated logic.
public class StateGroupObj extends StateObj {

	private LinkedList<String> childNames = new LinkedList<String>();
	private String entryState = "";
	private static final int ARC = 30;
	private static final Color GROUP_FILL = new Color(240, 247, 255);

	public StateGroupObj(int _x0, int _y0, int _x1, int _y1, int numb, int page, Color c, boolean b, int i) {
		super(_x0, _y0, _x1, _y1, numb, page, c, b, i);
		objName = "stateGroup" + numb;
	}

	public StateGroupObj(int x0, int y0, int x1, int y1,
			LinkedList<ObjAttribute> newList, String name, int page, Color c) {
		super(x0, y0, x1, y1, newList, name, false, page, c);
	}

	public int getType() {
		return 5;
	}

	public void paintComponent(Graphics g) {
		if (myPage == currPage) {
			Graphics2D g2D = (Graphics2D) g;
			Stroke oldStroke = g2D.getStroke();
			g2D.setColor(GROUP_FILL);
			g2D.fillRoundRect(x0, y0, x1 - x0, y1 - y0, ARC, ARC);
			g2D.setColor(getColor());
			float width = oldStroke instanceof BasicStroke ? ((BasicStroke)oldStroke).getLineWidth() : 1.0f;
			g2D.setStroke(new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10.0f, new float[] {8.0f, 5.0f}, 0.0f));
			g2D.drawRoundRect(x0, y0, x1 - x0, y1 - y0, ARC, ARC);
			g2D.setStroke(oldStroke);
			if (getSelectStatus() != NONE) {
				g2D.setColor(Color.red);
				g2D.drawRoundRect(x0, y0, x1 - x0, y1 - y0, ARC, ARC);
				g2D.fillRect(x0 - 3, y0 - 3, 7, 7);
				g2D.fillRect(x1 - 3, y0 - 3, 7, 7);
				g2D.fillRect(x0 - 3, y1 - 3, 7, 7);
				g2D.fillRect(x1 - 3, y1 - 3, 7, 7);
			}
		}
	}

	public boolean containsState(StateObj state) {
		if (state.getPage() != myPage || state == this)
			return false;

		int[] coords = state.getCoords();
		return coords[0] >= x0 && coords[1] >= y0
				&& coords[2] <= x1 && coords[3] <= y1;
	}

	public boolean overlapsState(StateObj state) {
		if (state.getPage() != myPage || state == this)
			return false;

		int[] coords = state.getCoords();
		return coords[0] < x1 && coords[2] > x0
				&& coords[1] < y1 && coords[3] > y0;
	}

	public void setChildNames(LinkedList<String> names) {
		childNames = names;
	}

	public LinkedList<String> getChildNames() {
		return childNames;
	}

	public void setEntryState(String name) {
		entryState = name == null ? "" : name;
	}

	public String getEntryState() {
		return entryState;
	}

	public Vector<Point> getBorderPts() {
		Vector<Point> borderPts = getRectangleBorderPts(36);
		appendScaledRectangleBorderPts(borderPts);
		return borderPts;
	}

	public void save(BufferedWriter writer) throws IOException {
		writer.write("## START STATE GROUP OBJECT\n");
		writer.write("<stategroup>\n");

		writer.write(i(1) + "<attributes>\n");
		for (int j = 0; j < attrib.size(); j++) {
			ObjAttribute obj = attrib.get(j);
			obj.save(writer,1);
		}
		writer.write(i(1) + "</attributes>\n");

		writer.write(i(1) + "<x0>\n" + i(1) + x0 + "\n" + i(1) + "</x0>\n");
		writer.write(i(1) + "<y0>\n" + i(1) + y0 + "\n" + i(1) + "</y0>\n");
		writer.write(i(1) + "<x1>\n" + i(1) + x1 + "\n" + i(1) + "</x1>\n");
		writer.write(i(1) + "<y1>\n" + i(1) + y1 + "\n" + i(1) + "</y1>\n");
		writer.write(i(1) + "<page>\n" + i(1) + myPage + "\n" + i(1) + "</page>\n");
		writer.write(i(1) + "<color>\n" + i(1) + getColor().getRGB() + "\n" + i(1) + "</color>\n");
		writer.write(i(1) + "<entryState>\n" + i(1) + entryState + "\n" + i(1) + "</entryState>\n");
		writer.write(i(1) + "<children>\n");
		for (int j = 0; j < childNames.size(); j++) {
			writer.write(i(2) + "<child>\n" + i(2) + childNames.get(j) + "\n" + i(2) + "</child>\n");
		}
		writer.write(i(1) + "</children>\n");

		writer.write("</stategroup>\n");
		writer.write("## END STATE GROUP OBJECT\n");
	}
}
