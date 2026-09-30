package raffle.ui;

import javafx.scene.layout.Pane;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThemeTest {

   @Test
   void otherModeIsTheOppositeTheme() {
      assertEquals(Theme.Mode.LIGHT, Theme.Mode.DARK.other());
      assertEquals(Theme.Mode.DARK, Theme.Mode.LIGHT.other());
   }

   @Test
   void savedNamesAreParsedAndUnknownOnesMeanDark() {
      assertEquals(Theme.Mode.LIGHT, Theme.parse("light"));
      assertEquals(Theme.Mode.LIGHT, Theme.parse(" LIGHT "));
      assertEquals(Theme.Mode.DARK, Theme.parse("dark"));
      assertEquals(Theme.Mode.DARK, Theme.parse("purple"));
   }

   @Test
   void lightThemeIsAStyleClassOnTheRootAndDarkHasNone() {
      Pane root = new Pane();

      Theme.apply(root, Theme.Mode.LIGHT);
      assertTrue(root.getStyleClass().contains("theme-light"));

      Theme.apply(root, Theme.Mode.DARK);
      assertFalse(root.getStyleClass().contains("theme-light"));
   }

   @Test
   void applyingTwiceDoesNotDuplicateTheStylesheetOrTheClass() {
      Pane root = new Pane();

      Theme.apply(root, Theme.Mode.LIGHT);
      Theme.apply(root, Theme.Mode.LIGHT);

      assertEquals(1, root.getStylesheets().size());
      assertEquals(1, root.getStyleClass().stream().filter("theme-light"::equals).count());
   }

   @Test
   void theStylesheetShipsWithTheApplication() {
      assertTrue(Theme.stylesheet().endsWith("/stylesheets/styles.css"));
   }
}
