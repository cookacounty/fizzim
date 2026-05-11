import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

/*
 * Java compatibility entry point for the Fizzim HDL backend.
 *
 * The Perl backend is still the golden HDL generator.  This class intentionally
 * delegates to the checked-in fizzim.pl so the Java entry point is line-for-line
 * compatible before the generator internals are ported.  Keeping this thin
 * compatibility layer lets the GUI and tests compare a Java-launched backend
 * against the direct Perl backend without changing generated RTL.
 */
public class FizzimJavaBackend {
	private static final String PERL_ENV = "FIZZIM_PERL";
	private static final String BACKEND_ENV = "FIZZIM_PERL_BACKEND";

	public static void main(String[] args) {
		try {
			ArrayList<String> command = new ArrayList<String>();
			command.add(resolvePerlCommand());
			command.add(resolveBackendScript().getAbsolutePath());
			for(int i = 0; i < args.length; i++)
				command.add(args[i]);

			ProcessBuilder builder = new ProcessBuilder(command);
			builder.inheritIO();
			Process process = builder.start();
			System.exit(process.waitFor());
		} catch (Exception ex) {
			System.err.println("FizzimJavaBackend failed: " + ex.getMessage());
			System.exit(1);
		}
	}

	private static String resolvePerlCommand() {
		String configured = System.getenv(PERL_ENV);
		if(configured != null && !configured.trim().equals(""))
			return configured.trim();
		return "perl";
	}

	private static File resolveBackendScript() throws IOException, URISyntaxException {
		String configured = System.getenv(BACKEND_ENV);
		if(configured != null && !configured.trim().equals(""))
		{
			File backend = new File(configured.trim());
			if(!backend.exists())
				throw new IOException(BACKEND_ENV + " points to a missing file: " + backend.getAbsolutePath());
			return backend;
		}

		File appDir = applicationDirectory();
		File backend = new File(appDir, "fizzim.pl");
		if(backend.exists())
			return backend;

		backend = new File(System.getProperty("user.dir"), "fizzim.pl");
		if(backend.exists())
			return backend;

		throw new IOException("Could not find fizzim.pl next to the jar/class files or in the current directory.");
	}

	private static File applicationDirectory() throws URISyntaxException {
		File location = new File(FizzimJavaBackend.class.getProtectionDomain().getCodeSource().getLocation().toURI());
		if(location.isFile())
			return location.getParentFile();
		return location;
	}
}
