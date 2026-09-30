package raffle.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * User facing text, looked up from the {@code i18n/messages*.properties} bundles that ship inside the jar.
 * English is the default; Romanian is picked up automatically when the system language is Romanian.
 */
public final class Messages {

   private static final String BUNDLE_NAME = "i18n.messages";

   private static ResourceBundle bundle = load(Locale.getDefault());

   private Messages() {
   }

   public static void setLocale(Locale locale) {
      bundle = load(locale);
   }// end of setLocale method

   public static String get(String key, Object... arguments) {
      String pattern;
      try {
         pattern = bundle.getString(key);
      } catch (MissingResourceException e) {
         return key;// a missing key is visible on screen instead of crashing the app
      }// end of try-catch block
      return arguments.length == 0 ? pattern : new MessageFormat(pattern, bundle.getLocale()).format(arguments);
   }// end of get method

   private static ResourceBundle load(Locale locale) {
      return ResourceBundle.getBundle(BUNDLE_NAME, locale);
   }// end of load method

}// end of Messages class
