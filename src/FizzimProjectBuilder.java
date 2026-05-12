import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

/**
 * Headless project HDL build support used by the command-line entry point.
 */
public class FizzimProjectBuilder {
	private static final String PREF_HDL_PERL = "hdlPerlCommand";
	private static final String PREF_HDL_BACKEND = "hdlBackendPath";
	private static final String PREF_HDL_OUTPUT_DIR = "hdlOutputDir";
	private static final String PREF_HDL_USE_MODULE_FILENAME = "hdlUseModuleFilename";
	private static final String PREF_HDL_OUTPUT_FILENAME = "hdlOutputFilename";
	private static final String PREF_HDL_EXTRA_ARGS = "hdlExtraArgs";
	private static final String PREF_HDL_SOURCE_CHECKSUM = "hdlSourceChecksum";
	private static final String PREF_HDL_COMPARE_ENABLED = "hdlCompareEnabled";
	private static final String PREF_HDL_COMPARE_COMMAND = "hdlCompareCommand";
	private static final String PREF_HDL_COMPARE_BACKEND = "hdlCompareBackendPath";
	private static final String PREF_HDL_COMPARE_ARGS = "hdlCompareArgs";
	private static final String PREF_HDL_COMPARE_SUFFIX = "hdlCompareSuffix";
	private static final String HDL_STATE_ATTR = "fizzim2_hdl_generated";
	private static final String HDL_OUTPUT_ATTR = "fizzim2_hdl_output";
	private static final String HDL_VERSION_ATTR = "fizzim2_hdl_version";
	private static final String HDL_BUILD_ATTR = "fizzim2_hdl_build";
	private static final Preferences USER_PREFS = Preferences.userNodeForPackage(FizzimGui.class);

	public static void main(String[] args) {
		System.exit(runCommandLine(args));
	}

	public static boolean isBuildCommand(String[] args) {
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i].toLowerCase();
			if(arg.equals("--build-project") || arg.equals("--build-all")
					|| arg.equals("-build-project") || arg.equals("-build-all"))
				return true;
		}
		return false;
	}

	public static int runCommandLine(String[] args) {
		File projectFile = null;
		for(int i = 0; i < args.length; i++)
		{
			String arg = args[i];
			String lower = arg.toLowerCase();
			if(lower.equals("--build-project") || lower.equals("--build-all")
					|| lower.equals("-build-project") || lower.equals("-build-all"))
			{
				if(i + 1 < args.length)
				{
					projectFile = new File(args[++i]);
					continue;
				}
				System.err.println("Missing project file after " + arg);
				printUsage();
				return 2;
			}
			else if(lower.endsWith(".fzp"))
				projectFile = new File(arg);
		}
		if(projectFile == null)
		{
			printUsage();
			return 2;
		}
		try {
			BuildReport report = buildProject(projectFile);
			System.out.print(report.text.toString());
			System.out.println("Build All: " + report.pass + " passed, " + report.fail + " failed");
			return report.fail == 0 ? 0 : 1;
		} catch (Exception ex) {
			System.err.println("Build All failed: " + ex.getMessage());
			return 1;
		}
	}

	public static void printUsage() {
		System.out.println("Usage: java -jar fizzim.jar --build-project <project.fzp>");
		System.out.println("       java -jar fizzim.jar --build-all <project.fzp>");
	}

	private static BuildReport buildProject(File requestedProjectFile) throws IOException {
		File projectFile = ensureProjectExtension(requestedProjectFile).getAbsoluteFile();
		if(!projectFile.exists())
			throw new IOException("Project file not found: " + projectFile.getAbsolutePath());
		LinkedList<File> diagrams = loadProject(projectFile);
		BuildReport report = new BuildReport();
		report.text.append("Fizzim Project Build Report\n");
		report.text.append("===========================\n\n");
		report.text.append("Project: ").append(projectFile.getAbsolutePath()).append("\n");
		report.text.append("Diagrams: ").append(diagrams.size()).append("\n\n");
		if(diagrams.size() == 0)
			return report;
		for(int i = 0; i < diagrams.size(); i++)
		{
			File fzm = diagrams.get(i);
			if(!fzm.exists())
			{
				report.fail++;
				report.text.append("FAIL ").append(pathRelativeToDirectory(projectFile.getParentFile(), fzm)).append("\n");
				report.text.append("  Missing diagram file\n\n");
				continue;
			}
			try {
				String machineName = getMachineNameFromFile(fzm);
				File output = resolveHdlOutputFile(fzm, machineName);
				File parent = output.getParentFile();
				if(parent != null && !parent.exists() && !parent.mkdirs())
					throw new IOException("Could not create output directory: " + parent.getAbsolutePath());
				HdlGenerationResult result = runConfiguredHdlBackend(output, getHdlPerlCommand(),
						getHdlBackendPath(), getHdlExtraArgs(), fzm);
				if(result.exitCode != 0)
				{
					report.fail++;
					report.text.append("FAIL ").append(pathRelativeToDirectory(projectFile.getParentFile(), fzm)).append("\n");
					report.text.append("  ").append(result.stderr).append("\n\n");
					continue;
				}
				if(getHdlCompareEnabled() && !runProjectHdlComparison(fzm, output, report))
				{
					report.fail++;
					continue;
				}
				recordGeneratedStatus(fzm, output);
				report.pass++;
				report.text.append("PASS ").append(pathRelativeToDirectory(projectFile.getParentFile(), fzm))
						.append(" -> ").append(pathRelativeToFile(fzm, output)).append("\n\n");
			} catch (Exception ex) {
				report.fail++;
				report.text.append("FAIL ").append(pathRelativeToDirectory(projectFile.getParentFile(), fzm)).append("\n");
				report.text.append("  ").append(ex.getMessage()).append("\n\n");
			}
		}
		return report;
	}

	private static LinkedList<File> loadProject(File projectFile) throws IOException {
		LinkedList<File> diagrams = new LinkedList<File>();
		File projectDir = projectFile.getParentFile();
		java.util.List<String> lines = Files.readAllLines(projectFile.toPath(), StandardCharsets.UTF_8);
		for(int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();
			if(line.equals("") || line.startsWith("#"))
				continue;
			File diagram = new File(line);
			if(!diagram.isAbsolute())
				diagram = new File(projectDir, line);
			diagrams.add(diagram.getAbsoluteFile());
		}
		Collections.sort(diagrams, new Comparator<File>() {
			public int compare(File a, File b) {
				return a.getAbsolutePath().compareToIgnoreCase(b.getAbsolutePath());
			}
		});
		return diagrams;
	}

	private static boolean runProjectHdlComparison(File fzm, File primaryOutput, BuildReport report) throws IOException, InterruptedException {
		File compareOutput = comparisonOutputFile(primaryOutput);
		HdlGenerationResult compare = runConfiguredHdlBackend(compareOutput, getHdlCompareCommand(),
				getHdlCompareBackendPath(), getHdlCompareArgs(), fzm);
		if(compare.exitCode != 0)
		{
			report.text.append("FAIL ").append(fzm.getName()).append("\n");
			report.text.append("  Comparison generation failed with exit code ").append(compare.exitCode).append("\n");
			report.text.append("  ").append(compare.stderr).append("\n\n");
			return false;
		}
		File diffFile = new File(primaryOutput.getParentFile(), stripExtension(primaryOutput.getName()) + ".diff.txt");
		String diff = diffFiles(primaryOutput, compareOutput);
		if(diff.equals(""))
		{
			if(diffFile.exists())
				diffFile.delete();
			if(compareOutput.exists())
				compareOutput.delete();
			return true;
		}
		Files.write(diffFile.toPath(), diff.getBytes(StandardCharsets.UTF_8));
		report.text.append("FAIL ").append(fzm.getName()).append("\n");
		report.text.append("  Generated HDL mismatch detected\n");
		report.text.append("  Primary: ").append(primaryOutput.getAbsolutePath()).append("\n");
		report.text.append("  Comparison: ").append(compareOutput.getAbsolutePath()).append("\n");
		report.text.append("  Diff: ").append(diffFile.getAbsolutePath()).append("\n\n");
		return false;
	}

	private static HdlGenerationResult runConfiguredHdlBackend(File output, String commandName, String backendPath,
			String backendArgs, File fzmFile) throws IOException, InterruptedException {
		File fzmDir = fzmFile.getAbsoluteFile().getParentFile();
		File errorFile = File.createTempFile("fizzim-hdl-generation", ".log");
		ArrayList<String> command = new ArrayList<String>();
		if(isJavaBackendClass(commandName, backendPath))
		{
			command.add(resolveJavaCommand(commandName));
			command.add("-cp");
			command.add(applicationClassPath());
			command.add(backendPath.trim());
			addHdlProvenanceArgs(command, fzmFile);
			command.addAll(splitCommandArgs(backendArgs));
			command.add(fzmFile.getName());
		}
		else
		{
			File backend = resolveRelativeToApp(backendPath);
			if(!backend.exists())
				throw new IOException("Backend script not found: " + backend.getAbsolutePath());
			command.add(commandName);
			command.add(backend.getAbsolutePath());
			addHdlProvenanceArgs(command, fzmFile);
			command.addAll(splitCommandArgs(backendArgs));
			command.add(fzmFile.getName());
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(fzmDir);
		builder.redirectOutput(output);
		builder.redirectError(errorFile);
		Process process = builder.start();
		int exit = process.waitFor();
		String stderr = new String(Files.readAllBytes(errorFile.toPath()), StandardCharsets.UTF_8);
		errorFile.delete();
		return new HdlGenerationResult(exit, stderr);
	}

	private static File resolveHdlOutputFile(File fzmFile, String machineName) throws IOException {
		String persistedOutput = getMachineAttributeValueFromFile(fzmFile, HDL_OUTPUT_ATTR);
		if(!persistedOutput.equals(""))
			return resolveRelativeToFzm(fzmFile, persistedOutput);
		return resolveHdlOutputFileFromSettings(fzmFile, machineName, getHdlOutputDir(),
				getHdlUseModuleFilename(), getHdlOutputFilename());
	}

	private static File resolveHdlOutputFileFromSettings(File fzmFile, String machineName, String outputDirSetting,
			boolean useModuleFilename, String outputFilenameSetting) {
		File fzmDir = fzmFile.getAbsoluteFile().getParentFile();
		File outputDir = resolveRelativeToFzm(fzmFile, outputDirSetting);
		String filename;
		if(useModuleFilename)
			filename = sanitizeHdlFilename(machineName) + ".v";
		else
			filename = outputFilenameSetting;
		if(filename == null || filename.trim().equals(""))
			filename = sanitizeHdlFilename(machineName) + ".v";
		if(!filename.toLowerCase().endsWith(".v"))
			filename += ".v";
		File output = new File(filename);
		if(output.isAbsolute())
			return output;
		return new File(outputDir == null ? fzmDir : outputDir, filename);
	}

	private static void recordGeneratedStatus(File fzmFile, File output) throws IOException {
		setMachineAttributeValueInFile(fzmFile, HDL_STATE_ATTR, "1");
		setMachineAttributeValueInFile(fzmFile, HDL_OUTPUT_ATTR, pathRelativeToFile(fzmFile, output));
		setMachineAttributeValueInFile(fzmFile, HDL_VERSION_ATTR, FizzimVersion.RELEASE_VERSION);
		setMachineAttributeValueInFile(fzmFile, HDL_BUILD_ATTR, FizzimVersion.BUILD_NUMBER);
	}

	private static String getMachineNameFromFile(File fzmFile) throws IOException {
		String name = getMachineAttributeValueFromFile(fzmFile, "name");
		if(!name.equals(""))
			return name;
		return stripExtension(fzmFile.getName());
	}

	private static String getMachineAttributeValueFromFile(File fzmFile, String attributeName) throws IOException {
		java.util.List<String> lines = Files.readAllLines(fzmFile.toPath(), StandardCharsets.UTF_8);
		boolean inGlobals = false;
		boolean inMachine = false;
		boolean inAttribute = false;
		boolean inValue = false;
		for(int i = 0; i < lines.size(); i++)
		{
			String line = lines.get(i).trim();
			if(line.equals("<globals>"))
				inGlobals = true;
			else if(line.equals("</globals>"))
				inGlobals = false;
			else if(inGlobals && line.equals("<machine>"))
				inMachine = true;
			else if(inMachine && line.equals("</machine>"))
				inMachine = false;
			else if(inMachine && line.equals("<" + attributeName + ">"))
				inAttribute = true;
			else if(inAttribute && line.equals("</" + attributeName + ">"))
				inAttribute = false;
			else if(inAttribute && line.equals("<value>"))
				inValue = true;
			else if(inValue && line.equals("</value>"))
				inValue = false;
			else if(inValue && !line.equals("") && !line.startsWith("<"))
				return line;
		}
		return "";
	}

	private static void setMachineAttributeValueInFile(File fzmFile, String attributeName, String value) throws IOException {
		String current = getMachineAttributeValueFromFile(fzmFile, attributeName);
		if(current.equals(value))
			return;
		java.util.List<String> lines = Files.readAllLines(fzmFile.toPath(), StandardCharsets.UTF_8);
		ArrayList<String> updated = new ArrayList<String>();
		boolean inGlobals = false;
		boolean inMachine = false;
		boolean inAttribute = false;
		boolean inValue = false;
		boolean valueWritten = false;
		boolean foundAttribute = false;
		for(int i = 0; i < lines.size(); i++)
		{
			String rawLine = lines.get(i);
			String line = rawLine.trim();
			if(inMachine && line.equals("</machine>") && !foundAttribute)
			{
				appendMachineAttributeXml(updated, attributeName, value);
				foundAttribute = true;
			}
			if(line.equals("<globals>"))
				inGlobals = true;
			else if(line.equals("</globals>"))
				inGlobals = false;
			else if(inGlobals && line.equals("<machine>"))
				inMachine = true;
			else if(inMachine && line.equals("</machine>"))
				inMachine = false;
			else if(inMachine && line.equals("<" + attributeName + ">"))
			{
				inAttribute = true;
				foundAttribute = true;
			}
			else if(inAttribute && line.equals("</" + attributeName + ">"))
				inAttribute = false;
			else if(inAttribute && line.equals("<value>"))
			{
				inValue = true;
				valueWritten = false;
			}
			else if(inValue && !valueWritten && (line.equals("") || !line.startsWith("<")))
			{
				updated.add("         " + value);
				valueWritten = true;
				continue;
			}
			else if(inValue && line.equals("</value>"))
				inValue = false;
			updated.add(rawLine);
		}
		Files.write(fzmFile.toPath(), updated, StandardCharsets.UTF_8);
	}

	private static void appendMachineAttributeXml(ArrayList<String> lines, String name, String value) {
		lines.add("      <" + name + ">");
		lines.add("            <status>");
		lines.add("            ABS");
		lines.add("            </status>");
		lines.add("         <value>");
		lines.add("         " + value);
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </value>");
		lines.add("         <vis>");
		lines.add("         0");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </vis>");
		lines.add("         <type>");
		lines.add("         attribute");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </type>");
		lines.add("         <comment>");
		lines.add("");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </comment>");
		lines.add("         <color>");
		lines.add("         -16777216");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </color>");
		lines.add("         <useratts>");
		lines.add("");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </useratts>");
		lines.add("         <resetval>");
		lines.add("");
		lines.add("            <status>");
		lines.add("            GLOBAL_VAR");
		lines.add("            </status>");
		lines.add("         </resetval>");
		lines.add("         <x2Obj>");
		lines.add("         0");
		lines.add("         </x2Obj>");
		lines.add("         <y2Obj>");
		lines.add("         0");
		lines.add("         </y2Obj>");
		lines.add("         <page>");
		lines.add("         -1");
		lines.add("         </page>");
		lines.add("      </" + name + ">");
	}

	private static File ensureProjectExtension(File file) {
		if(file.getName().toLowerCase().endsWith(".fzp"))
			return file;
		return new File(file.getAbsolutePath() + ".fzp");
	}

	private static File resolveRelativeToFzm(File fzmFile, String path) {
		File candidate = new File(path);
		if(candidate.isAbsolute())
			return candidate;
		return new File(fzmFile.getAbsoluteFile().getParentFile(), path);
	}

	private static File resolveRelativeToApp(String path) {
		File candidate = new File(path);
		if(candidate.isAbsolute())
			return candidate;
		File appRelative = new File(applicationDirectory(), path);
		if(appRelative.exists())
			return appRelative;
		return new File(System.getProperty("user.dir"), path);
	}

	private static File applicationDirectory() {
		File classPath = new File(applicationClassPath());
		if(classPath.isFile())
			return classPath.getParentFile();
		return classPath;
	}

	private static String applicationClassPath() {
		try {
			return new File(FizzimProjectBuilder.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath();
		} catch (Exception ex) {
			return new File(System.getProperty("user.dir")).getAbsolutePath();
		}
	}

	private static boolean isJavaBackendClass(String commandName, String backendPath) {
		if(backendPath == null || backendPath.trim().equals(""))
			return false;
		String backend = backendPath.trim();
		String command = commandName == null ? "" : commandName.trim().toLowerCase();
		return backend.equals("FizzimJavaBackend")
				&& (command.equals("") || command.equals("java") || command.equals("javaw") || command.endsWith("\\java.exe")
						|| command.endsWith("/java") || command.endsWith("/java.exe"));
	}

	private static String resolveJavaCommand(String commandName) {
		String command = commandName == null ? "" : commandName.trim();
		String lower = command.toLowerCase();
		if(!command.equals("") && !lower.equals("java") && !lower.equals("javaw"))
			return command;
		String executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
		return new File(new File(System.getProperty("java.home"), "bin"), executable).getAbsolutePath();
	}

	private static String sanitizeHdlFilename(String name) {
		if(name == null || name.trim().equals(""))
			return "fsm";
		return name.trim().replaceAll("[^A-Za-z0-9_.$-]", "_");
	}

	private static void addHdlProvenanceArgs(ArrayList<String> command, File fzmFile) {
		command.add("-fizzimversion");
		command.add(FizzimVersion.FILE_VERSION);
		if(fzmFile != null)
		{
			command.add("-sourcefile");
			command.add(fzmFile.getName());
		}
		if(getHdlSourceChecksum())
			command.add("-sourcechecksum");
	}

	private static ArrayList<String> splitCommandArgs(String args) {
		ArrayList<String> parts = new ArrayList<String>();
		if(args == null || args.trim().equals(""))
			return parts;
		StringTokenizer tokenizer = new StringTokenizer(args);
		while(tokenizer.hasMoreTokens())
			parts.add(tokenizer.nextToken());
		return parts;
	}

	private static String pathRelativeToFile(File baseFile, File file) {
		return pathRelativeToDirectory(baseFile.getAbsoluteFile().getParentFile(), file);
	}

	private static String pathRelativeToDirectory(File directory, File file) {
		try {
			return directory.toPath().toAbsolutePath().normalize().relativize(file.toPath().toAbsolutePath().normalize()).toString();
		} catch (Exception ex) {
			return file.getAbsolutePath();
		}
	}

	private static File comparisonOutputFile(File primaryOutput) {
		String suffix = getHdlCompareSuffix();
		if(suffix == null || suffix.trim().equals(""))
			suffix = ".java";
		String base = stripExtension(primaryOutput.getName());
		return new File(primaryOutput.getParentFile(), base + suffix + ".v");
	}

	private static String diffFiles(File primary, File comparison) throws IOException {
		java.util.List<String> primaryLines = Files.readAllLines(primary.toPath(), StandardCharsets.UTF_8);
		java.util.List<String> comparisonLines = Files.readAllLines(comparison.toPath(), StandardCharsets.UTF_8);
		StringBuffer diff = new StringBuffer();
		int max = Math.max(primaryLines.size(), comparisonLines.size());
		for(int i = 0; i < max; i++)
		{
			String a = i < primaryLines.size() ? primaryLines.get(i) : null;
			String b = i < comparisonLines.size() ? comparisonLines.get(i) : null;
			if(a == null || b == null || !a.equals(b))
			{
				diff.append("@@ line ").append(i + 1).append(" @@\n");
				diff.append("- ").append(a == null ? "<missing>" : a).append("\n");
				diff.append("+ ").append(b == null ? "<missing>" : b).append("\n");
			}
		}
		return diff.toString();
	}

	private static String stripExtension(String filename) {
		int dot = filename.lastIndexOf('.');
		if(dot <= 0)
			return filename;
		return filename.substring(0, dot);
	}

	private static String getHdlPerlCommand() {
		return USER_PREFS.get(PREF_HDL_PERL, "perl");
	}

	private static String getHdlBackendPath() {
		return USER_PREFS.get(PREF_HDL_BACKEND, "fizzim.pl");
	}

	private static String getHdlOutputDir() {
		return USER_PREFS.get(PREF_HDL_OUTPUT_DIR, ".");
	}

	private static boolean getHdlUseModuleFilename() {
		return USER_PREFS.getBoolean(PREF_HDL_USE_MODULE_FILENAME, true);
	}

	private static String getHdlOutputFilename() {
		return USER_PREFS.get(PREF_HDL_OUTPUT_FILENAME, "");
	}

	private static String getHdlExtraArgs() {
		String args = USER_PREFS.get(PREF_HDL_EXTRA_ARGS, "");
		if(args != null && args.trim().equals("-noaddversion"))
			return "";
		return args == null ? "" : args;
	}

	private static boolean getHdlSourceChecksum() {
		return USER_PREFS.getBoolean(PREF_HDL_SOURCE_CHECKSUM, false);
	}

	private static boolean getHdlCompareEnabled() {
		return USER_PREFS.getBoolean(PREF_HDL_COMPARE_ENABLED, false);
	}

	private static String getHdlCompareCommand() {
		return USER_PREFS.get(PREF_HDL_COMPARE_COMMAND, "java");
	}

	private static String getHdlCompareBackendPath() {
		return USER_PREFS.get(PREF_HDL_COMPARE_BACKEND, "FizzimJavaBackend");
	}

	private static String getHdlCompareArgs() {
		String args = USER_PREFS.get(PREF_HDL_COMPARE_ARGS, "");
		if(args == null || args.trim().equals(""))
			return getHdlExtraArgs();
		return args;
	}

	private static String getHdlCompareSuffix() {
		return USER_PREFS.get(PREF_HDL_COMPARE_SUFFIX, ".java");
	}

	private static class HdlGenerationResult {
		int exitCode;
		String stderr;

		HdlGenerationResult(int code, String err) {
			exitCode = code;
			stderr = err == null ? "" : err;
		}
	}

	private static class BuildReport {
		int pass = 0;
		int fail = 0;
		StringBuffer text = new StringBuffer();
	}
}
