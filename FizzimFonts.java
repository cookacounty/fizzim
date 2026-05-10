import java.awt.Font;
import javax.swing.JTable;

public class FizzimFonts {
	public static final String CODE_FONT_FAMILY = Font.MONOSPACED;
	public static final int CANVAS_FONT_SIZE = 11;
	public static final int TABLE_FONT_SIZE = 12;

	public static Font canvasFont() {
		return codeFont(CANVAS_FONT_SIZE);
	}

	public static Font tableFont() {
		return codeFont(TABLE_FONT_SIZE);
	}

	public static Font codeFont(int size) {
		return new Font(CODE_FONT_FAMILY, Font.PLAIN, size);
	}

	public static Font normalizeCodeFont(Font font, int fallbackSize) {
		int size = fallbackSize;
		int style = Font.PLAIN;
		if(font != null) {
			size = font.getSize();
			style = font.getStyle();
		}
		return new Font(CODE_FONT_FAMILY, style, size);
	}

	public static void applyCodeFont(JTable table) {
		Font font = tableFont();
		table.setFont(font);
		table.getTableHeader().setFont(font);
		table.setRowHeight(Math.max(table.getRowHeight(), font.getSize() + 8));
	}
}
