package raffle.ui;

import javafx.scene.Parent;
import raffle.utils.AppSettings;

import java.io.IOException;
import java.util.Objects;

/**
 * Dark and light colour themes. The colours themselves live in the stylesheet as variables; a theme is just the
 * style class {@code theme-light} on the scene root (the dark theme is the default and has no class).
 */
public final class Theme {

   public enum Mode {
      DARK, LIGHT;

      public Mode other() {
         return this == DARK ? LIGHT : DARK;
      }// end of other method
   }

   private static final String SETTING = "theme";
   private static final String LIGHT_CLASS = "theme-light";

   private static Mode current = Mode.DARK;

   private Theme() {
   }

   /** Applies the saved theme; the dark one when nothing was saved. */
   public static void initFromSettings() {
      current = AppSettings.get(SETTING).map(Theme::parse).orElse(Mode.DARK);
   }// end of initFromSettings method

   static Mode parse(String text) {
      return "light".equalsIgnoreCase(text.trim()) ? Mode.LIGHT : Mode.DARK;
   }// end of parse method

   public static Mode mode() {
      return current;
   }// end of mode method

   /** Switches to the other theme and remembers the choice. Screens built afterwards use it. */
   public static void toggle() {
      current = current.other();
      try {
         AppSettings.put(SETTING, current.name().toLowerCase(java.util.Locale.ROOT));
      } catch (IOException e) {
         System.err.println("Could not save the theme setting: " + e.getMessage());
      }// end of try-catch block
   }// end of toggle method

   /** Styles a screen's root node with the stylesheet and the current theme. */
   public static void apply(Parent root) {
      apply(root, current);
   }// end of apply method

   static void apply(Parent root, Mode mode) {
      String stylesheet = stylesheet();
      if (! root.getStylesheets().contains(stylesheet)) {
         root.getStylesheets().add(stylesheet);
      }// end of if block
      root.getStyleClass().remove(LIGHT_CLASS);
      if (mode == Mode.LIGHT) {
         root.getStyleClass().add(LIGHT_CLASS);
      }// end of if block
   }// end of apply method

   public static String stylesheet() {
      return Objects.requireNonNull(Theme.class.getResource("/stylesheets/styles.css"), "styles.css").toExternalForm();
   }// end of stylesheet method

}// end of Theme class
