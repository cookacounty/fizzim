import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Vector;

// A fork is a small transition endpoint used to share one incoming condition
// across multiple outgoing branches without drawing it as a real state.
public class ForkObj extends StateObj {

	private static final int SIZE = 20;

	public ForkObj(int x, int y, int numb, int page, Color c, boolean grid, int gridS) {
		super(x - SIZE / 2, y - SIZE / 2, x + SIZE / 2, y + SIZE / 2, numb, page, c, grid, gridS);
		objName = "fork" + numb;
		attrib = new LinkedList<ObjAttribute>();
	}

	public ForkObj(int x0, int y0, int x1, int y1, String name, int page, Color c) {
		super(x0, y0, x1, y1, new LinkedList<ObjAttribute>(), name, false, page, c);
		normalizeSize();
	}

	private void normalizeSize() {
		int centerX = x0 + ((x1 - x0) / 2);
		int centerY = y0 + ((y1 - y0) / 2);
		x0 = centerX - SIZE / 2;
		y0 = centerY - SIZE / 2;
		x1 = x0 + SIZE;
		y1 = y0 + SIZE;
	}

	public boolean setSelectStatus(int x, int y) {
		boolean selected = super.setSelectStatus(x, y);
		if (selected) {
			setSelectStatus(true);
		}
		return selected;
	}

	public boolean containsPoint(int x, int y) {
		return myPage == currPage && x >= x0 && x <= x1 && y >= y0 && y <= y1;
	}

	public void setSize(int w, int h) {
		normalizeSize();
	}

	public void paintComponent(Graphics g) {
		if (myPage == currPage) {
			g.setColor(getColor());
			g.fillOval(x0, y0, x1 - x0, y1 - y0);
			if(isLintHighlighted()) {
				Graphics2D g2D = (Graphics2D)g;
				Stroke oldStroke = g2D.getStroke();
				g2D.setColor(new Color(255, 140, 0));
				g2D.setStroke(new BasicStroke(4.0f));
				g2D.drawOval(x0 - 6, y0 - 6, x1 - x0 + 12, y1 - y0 + 12);
				g2D.setStroke(oldStroke);
			}
			if(isHoverHighlighted()) {
				Graphics2D g2D = (Graphics2D)g;
				Stroke oldStroke = g2D.getStroke();
				g2D.setColor(new Color(65, 145, 220));
				g2D.setStroke(new BasicStroke(2.0f));
				g2D.drawOval(x0 - 5, y0 - 5, x1 - x0 + 10, y1 - y0 + 10);
				g2D.setStroke(oldStroke);
			}
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

	public Vector<Point> getBorderPts() {
		return getOvalBorderPts(36);
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
