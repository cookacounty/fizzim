import java.awt.Component;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JOptionPane;

public class VerilogNameValidator {
	private static final Set<String> RESERVED_WORDS = new HashSet<String>(Arrays.asList(new String[] {
			"accept_on", "alias", "always", "always_comb", "always_ff", "always_latch", "and", "assert",
			"assign", "assume", "automatic", "before", "begin", "bind", "bins", "binsof", "bit", "break",
			"buf", "bufif0", "bufif1", "byte", "case", "casex", "casez", "cell", "chandle", "checker", "class",
			"clocking", "cmos", "config", "const", "constraint", "context", "continue", "cover",
			"covergroup", "coverpoint", "cross", "deassign", "default", "defparam", "design", "disable",
			"dist", "do", "edge", "else", "end", "endcase", "endclass", "endclocking", "endconfig",
			"endchecker", "endfunction", "endgenerate", "endgroup", "endinterface", "endmodule", "endpackage",
			"endprimitive", "endprogram", "endproperty", "endspecify", "endsequence", "endtable", "endtask",
			"enum", "event", "expect", "export", "extends", "extern", "final", "first_match", "for",
			"eventually", "force", "foreach", "forever", "fork", "function", "generate", "genvar", "global", "highz0", "highz1",
			"if", "iff", "ifnone", "ignore_bins", "illegal_bins", "implements", "implies", "import", "incdir", "include", "initial",
			"inout", "input", "inside", "instance", "int", "integer", "interface", "intersect", "join",
			"join_any", "join_none", "large", "let", "liblist", "library", "localparam", "logic", "longint",
			"macromodule", "matches", "medium", "modport", "module", "nand", "negedge", "new", "nexttime",
			"nettype", "nmos", "nor", "noshowcancelled", "not", "notif0", "notif1", "null", "or", "output", "package",
			"packed", "parameter", "pmos", "posedge", "primitive", "priority", "program", "property",
			"protected", "pull0", "pull1", "pulldown", "pullup", "pulsestyle_ondetect", "pulsestyle_onevent",
			"pure", "rand", "randc", "randcase", "randsequence", "rcmos", "real", "realtime", "ref", "reg",
			"release", "repeat", "restrict", "return", "rnmos", "rpmos", "rtran", "rtranif0", "rtranif1",
			"s_always", "s_eventually", "s_nexttime", "s_until", "s_until_with", "scalared", "sequence",
			"shortint", "shortreal", "showcancelled", "signed", "small", "soft", "solve", "specify", "specparam",
			"static", "string", "strong", "strong0", "strong1", "struct", "super", "supply0", "supply1",
			"sync_accept_on", "sync_reject_on", "table", "tagged", "task", "this", "throughout", "time",
			"timeprecision", "timeunit", "tran", "tranif0", "tranif1", "tri", "tri0", "tri1", "triand",
			"trior", "trireg", "type", "typedef", "union", "unique", "unique0", "unsigned", "until",
			"until_with", "untyped", "use", "var", "vectored", "virtual", "void", "wait", "wait_order",
			"wand", "weak", "weak0", "weak1", "while", "wildcard", "wire", "with", "within", "wor",
			"xnor", "xor"
	}));

	private VerilogNameValidator() {
	}

	public static String reservedWordInIdentifier(String name) {
		String identifier = baseIdentifier(name);
		if(identifier.equals("") || identifier.startsWith("\\"))
			return null;
		String lowerIdentifier = identifier.toLowerCase();
		if(RESERVED_WORDS.contains(lowerIdentifier))
			return identifier;
		return null;
	}

	public static boolean showReservedWordError(Component parent, String name, String nameType) {
		String reservedWord = reservedWordInIdentifier(name);
		if(reservedWord == null)
			return false;
		JOptionPane.showMessageDialog(parent,
				"\"" + reservedWord + "\" is a Verilog/SystemVerilog reserved word.\n"
				+ "Choose a different " + nameType + ".",
				"Reserved Word",
				JOptionPane.ERROR_MESSAGE);
		return true;
	}

	private static String baseIdentifier(String name) {
		if(name == null)
			return "";
		String identifier = name.trim();
		int bracket = identifier.indexOf('[');
		if(bracket > -1)
			identifier = identifier.substring(0, bracket).trim();
		return identifier;
	}
}
