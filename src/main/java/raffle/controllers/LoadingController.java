package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Rectangle;

import raffle.utils.Messages;

import java.util.Objects;

public class LoadingController {

   @FXML
   private ProgressBar loadingProgress;

   @FXML
   private ImageView logoImageView;

   @FXML
   private void initialize() {
      // Set the logo image
      Image logo = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/logo.png")));

      if (logo.isError()) {
         /*
          * If the image is not loaded properly, show an error message and exit the
          * application. This is a critical error and the application cannot continue
          * without the logo.
          */
         Alert.AlertType type = Alert.AlertType.ERROR;
         String title = Messages.get("alert.title.criticalError");
         String header = Messages.get("app.err.critical");
         Alert alert = new Alert(type);
         alert.setTitle(title);
         alert.setHeaderText(header);
         alert.showAndWait();
         System.exit(1);
      } else {
         Rectangle clip = new Rectangle(logoImageView.getFitWidth(), logoImageView.getFitHeight());

         clip.setArcWidth(50);
         clip.setArcHeight(50);

         logoImageView.setClip(clip);

         logoImageView.setImage(logo);
      }// end of if-else
   }// end of method initialize

}// end of class LoadingController
