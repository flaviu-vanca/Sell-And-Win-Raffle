package raffle.utils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * User facing text, looked up from the {@code i18n/messages*.properties} bundles that ship inside the jar.
 * <p>
 * English is the default. The language is taken from the saved setting, otherwise from the system language;
 * anything that is not a supported language falls back to English.
 */
public final class Messages {

   public static final List<Locale> SUPPORTED = List.of(Locale.ENGLISH, Locale.forLanguageTag("ro"));

   private static final String BUNDLE_NAME = "i18n.messages";
   private static final String LANGUAGE_SETTING = "language";

   private static Locale current = supported(Locale.getDefault());
   private static ResourceBundle bundle = load(current);

   private Messages() {
   }

   /** Applies the saved language, or the system language when none was saved yet. */
   public static void initFromSettings() {
      setLocale(AppSettings.get(LANGUAGE_SETTING).map(Locale::forLanguageTag).orElseGet(Locale::getDefault));
   }// end of initFromSettings method

   public static void setLocale(Locale locale) {
      current = supported(locale);
      bundle = load(current);
   }// end of setLocale method

   /** Switches to the next supported language and remembers the choice. */
   public static void switchLanguage() {
      setLocale(other());
      try {
         AppSettings.put(LANGUAGE_SETTING, current.getLanguage());
      } catch (java.io.IOException e) {
         System.err.println("Could not save the language setting: " + e.getMessage());
      }// end of try-catch block
   }// end of switchLanguage method

   public static Locale locale() {
      return current;
   }// end of locale method

   /** The language the language button would switch to. */
   public static Locale other() {
      return SUPPORTED.stream().filter(locale -> ! locale.getLanguage().equals(current.getLanguage())).findFirst().orElse(Locale.ENGLISH);
   }// end of other method

   public static ResourceBundle bundle() {
      return bundle;
   }// end of bundle method

   public static String get(String key, Object... arguments) {
      String pattern;
      try {
         pattern = bundle.getString(key);
      } catch (MissingResourceException e) {
         return key;// a missing key is visible on screen instead of crashing the app
      }// end of try-catch block
      return arguments.length == 0 ? pattern : new MessageFormat(pattern, current).format(arguments);
   }// end of get method

   private static Locale supported(Locale locale) {
      return SUPPORTED.stream().filter(candidate -> candidate.getLanguage().equals(locale.getLanguage())).findFirst().orElse(Locale.ENGLISH);
   }// end of supported method

   private static ResourceBundle load(Locale locale) {
      return ResourceBundle.getBundle(BUNDLE_NAME, locale);
   }// end of load method

}// end of Messages class
