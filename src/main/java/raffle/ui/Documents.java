package raffle.ui;

import javafx.application.HostServices;
import javafx.scene.control.Alert;
import raffle.utils.Messages;

import java.nio.file.Path;

/** Opens a document (a report, a receipt) in the program the computer uses for it: for an HTML page, the browser. */
public final class Documents {

   private static HostServices hostServices;

   private Documents() {
   }

   public static void init(HostServices services) {
      hostServices = services;
   }// end of init method

   /** Opens the file; when this is not possible the operator is told where the file is. */
   public static void open(Path file) {
      if (hostServices == null) {
         Dialogs.show(Alert.AlertType.INFORMATION, Messages.get("report.title"), Messages.get("report.saved", file.toString()));
         return;
      }// end of if block
      hostServices.showDocument(file.toUri().toString());
   }// end of open method

}// end of Documents class
