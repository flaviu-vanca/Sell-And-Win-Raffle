package raffle.ui;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import raffle.utils.Messages;

import java.util.Optional;

/** The one place that builds alerts and confirmations, so they follow the theme and the active language. */
public final class Dialogs {

   private Dialogs() {
   }

   public static Alert build(Alert.AlertType type, String title, String header, String content) {
      Alert alert = new Alert(type);
      alert.setTitle(title);
      alert.setHeaderText(header);
      alert.setContentText(content);
      Theme.apply(alert.getDialogPane());
      return alert;
   }// end of build method

   /**
    * Shows a message and waits for it to be closed. A "confirmation" message is only a notice that something
    * worked, so it gets the single OK button of an information dialog (questions go through {@link #confirm}).
    */
   public static void show(Alert.AlertType type, String title, String message) {
      Alert.AlertType shown = type == Alert.AlertType.CONFIRMATION ? Alert.AlertType.INFORMATION : type;
      build(shown, title, null, message).showAndWait();
   }// end of show method

   /**
    * Like {@link #show} but deferred: JavaFX does not allow a dialog to be opened while an animation or a layout
    * pass is running, so anything triggered from those must use this.
    */
   public static void showLater(Alert.AlertType type, String title, String message) {
      Platform.runLater(() -> show(type, title, message));
   }// end of showLater method

   /** A Yes/No question. */
   public static boolean confirm(String title, String header) {
      Alert alert = build(Alert.AlertType.CONFIRMATION, title, header, Messages.get("dialog.chooseOption"));
      ButtonType yes = new ButtonType(Messages.get("dialog.yes"));
      ButtonType no = new ButtonType(Messages.get("dialog.no"));
      alert.getButtonTypes().setAll(yes, no);

      Optional<ButtonType> answer = alert.showAndWait();
      return answer.isPresent() && answer.get() == yes;
   }// end of confirm method

}// end of Dialogs class
