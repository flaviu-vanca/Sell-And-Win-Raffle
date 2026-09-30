package raffle.ui;

import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.InputStream;

/** The window icon (the price tag of the logo) shown in the title bar and in the task bar. */
public final class AppIcon {

   private static final String RESOURCE = "/icons/app-icon.png";

   private static Image image;

   private AppIcon() {
   }

   /** Gives a window the application icon. A missing icon only means the default one is shown. */
   public static void apply(Window window) {
      if (window instanceof Stage stage && stage.getIcons().isEmpty()) {
         Image icon = load();
         if (icon != null) {
            stage.getIcons().add(icon);
         }// end of if block
      }// end of if block
   }// end of apply method

   private static Image load() {
      if (image == null) {
         try (InputStream in = AppIcon.class.getResourceAsStream(RESOURCE)) {
            image = in == null ? null : new Image(in);
         } catch (java.io.IOException e) {
            System.err.println("Could not load the application icon: " + e.getMessage());
         }// end of try-catch block
      }// end of if block
      return image;
   }// end of load method

}// end of AppIcon class
