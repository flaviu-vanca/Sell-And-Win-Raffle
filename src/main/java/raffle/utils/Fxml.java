package raffle.utils;

import javafx.fxml.FXMLLoader;

import java.util.Objects;

/** Creates FXML loaders that resolve {@code %key} texts from the active language bundle. */
public final class Fxml {

   private Fxml() {
   }

   public static FXMLLoader loader(String resourcePath) {
      return new FXMLLoader(Objects.requireNonNull(Fxml.class.getResource(resourcePath), resourcePath), Messages.bundle());
   }// end of loader method

}// end of Fxml class
