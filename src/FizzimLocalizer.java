import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class FizzimLocalizer {
	private static final String PREF_LANGUAGE = "guiLanguage";
	private static final String BUNDLE_NAME = "FizzimMessages";
	private static Locale locale = Locale.ENGLISH;
	private static ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);

	public static void load(Preferences prefs) {
		setLanguage(prefs.get(PREF_LANGUAGE, "en"), prefs);
	}

	public static void setLanguage(String language, Preferences prefs) {
		if(language == null || language.trim().equals(""))
			language = "en";
		language = language.trim();
		if(language.equals("ja"))
			locale = Locale.JAPANESE;
		else if(language.equals("zh_CN"))
			locale = Locale.SIMPLIFIED_CHINESE;
		else if(language.equals("zh_TW"))
			locale = Locale.TRADITIONAL_CHINESE;
		else if(language.equals("ko"))
			locale = Locale.KOREAN;
		else if(language.equals("de"))
			locale = Locale.GERMAN;
		else if(language.equals("fr"))
			locale = Locale.FRENCH;
		else if(language.equals("es"))
			locale = new Locale("es");
		else if(language.equals("pt"))
			locale = new Locale("pt");
		else if(language.equals("hi"))
			locale = new Locale("hi");
		else if(language.equals("ru"))
			locale = new Locale("ru");
		else
		{
			language = "en";
			locale = Locale.ENGLISH;
		}
		bundle = ResourceBundle.getBundle(BUNDLE_NAME, locale);
		if(prefs != null)
		{
			prefs.put(PREF_LANGUAGE, language);
			try {
				prefs.flush();
			} catch (Exception e) { }
		}
	}

	public static String getLanguage() {
		if(locale.equals(Locale.SIMPLIFIED_CHINESE))
			return "zh_CN";
		if(locale.equals(Locale.TRADITIONAL_CHINESE))
			return "zh_TW";
		String language = locale.getLanguage();
		if(language.equals("ja") || language.equals("ko") || language.equals("de")
				|| language.equals("fr") || language.equals("es") || language.equals("pt")
				|| language.equals("hi") || language.equals("ru"))
			return language;
		return "en";
	}

	public static String t(String key) {
		try {
			return bundle.getString(key);
		} catch (MissingResourceException ex) {
			return key;
		}
	}
}
