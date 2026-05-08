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
			g.setColor(getColor());
			g.drawRoundRect(x0, y0, x1 - x0, y1 - y0, ARC, ARC);
			if (getSelectStatus() != NONE) {
				g.setColor(Color.red);
				g.drawRoundRect(x0, y0, x1 - x0, y1 - y0, ARC, ARC);
				g.fillRect(x0 - 3, y0 - 3, 7, 7);
				g.fillRect(x1 - 3, y0 - 3, 7, 7);
				g.fillRect(x0 - 3, y1 - 3, 7, 7);
				g.fillRect(x1 - 3, y1 - 3, 7, 7);
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
		Vector<Point> borderPts = new Vector<Point>(36);
		double cx = x0 + (x1 - x0) / 2.0;
		double cy = y0 + (y1 - y0) / 2.0;
		double halfW = (x1 - x0) / 2.0;
		double halfH = (y1 - y0) / 2.0;

		for (int i = 0; i < 36; i++) {
			double angle = (2 * Math.PI / 36) * i;
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			double scaleX = cos == 0 ? Double.MAX_VALUE : halfW / Math.abs(cos);
			double scaleY = sin == 0 ? Double.MAX_VALUE : halfH / Math.abs(sin);
			double scale = Math.min(scaleX, scaleY);
			int x = (int) Math.round(cx + scale * cos);
			int y = (int) Math.round(cy + scale * sin);
			borderPts.add(i, new Point(x, y));
		}
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
