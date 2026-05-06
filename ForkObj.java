import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;

// A fork is a small transition endpoint used to share one incoming condition
// across multiple outgoing branches without drawing it as a real state.
public class ForkObj extends StateObj {

	private static final int SIZE = 10;

	public ForkObj(int x, int y, int numb, int page, Color c, boolean grid, int gridS) {
		super(x - SIZE / 2, y - SIZE / 2, x + SIZE / 2, y + SIZE / 2, numb, page, c, grid, gridS);
		objName = "fork" + numb;
		attrib = new LinkedList<ObjAttribute>();
	}

	public ForkObj(int x0, int y0, int x1, int y1, String name, int page, Color c) {
		super(x0, y0, x1, y1, new LinkedList<ObjAttribute>(), name, false, page, c);
	}

	public void paintComponent(Graphics g) {
		if (myPage == currPage) {
			g.setColor(getColor());
			g.fillOval(x0, y0, x1 - x0, y1 - y0);
			if (getSelectStatus() != NONE) {
				g.setColor(Color.red);
				g.drawRect(x0 - 3, y0 - 3, x1 - x0 + 6, y1 - y0 + 6);
			}
		}
	}

	public int getType() {
		return 4;
	}

	public Point getCenter(int page) {
		return getRealCenter(page);
	}

	public Point getStart() {
		return getRealCenter(myPage);
	}

	public void save(BufferedWriter writer) throws IOException {
		writer.write("## START FORK OBJECT\n");
		writer.write("<fork>\n");
		writer.write(i(1) + "<name>\n" + i(1) + objName + "\n" + i(1) + "</name>\n");
		writer.write(i(1) + "<x0>\n" + i(1) + x0 + "\n" + i(1) + "</x0>\n");
		writer.write(i(1) + "<y0>\n" + i(1) + y0 + "\n" + i(1) + "</y0>\n");
		writer.write(i(1) + "<x1>\n" + i(1) + x1 + "\n" + i(1) + "</x1>\n");
		writer.write(i(1) + "<y1>\n" + i(1) + y1 + "\n" + i(1) + "</y1>\n");
		writer.write(i(1) + "<page>\n" + i(1) + myPage + "\n" + i(1) + "</page>\n");
		writer.write(i(1) + "<color>\n" + i(1) + getColor().getRGB() + "\n" + i(1) + "</color>\n");
		writer.write("</fork>\n");
		writer.write("## END FORK OBJECT\n");
	}
}
