package raffle.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import raffle.controllers.*;
import raffle.models.Item;
import raffle.ui.Dialogs;
import raffle.ui.Theme;
import raffle.utils.AppPaths;
import raffle.utils.BackupService;
import raffle.utils.Fxml;
import raffle.utils.Messages;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class App extends Application {

   private Stage primaryStage;

   @Override
   public void start(Stage primaryStage) {
      this.primaryStage = primaryStage;
      this.primaryStage.setTitle("Sell & Win Raffle");
      Messages.initFromSettings();
      Theme.initFromSettings();

      // Keep a copy of the data from before this session. A failed backup must never stop the app from starting.
      try {
         BackupService.snapshot(AppPaths.root());
      } catch (IOException e) {
         System.err.println(Messages.get("backup.failed") + " " + e.getMessage());
      }// end of try-catch block

      showLoadingView();
   }// end of start method

   // Method to show the Loading View with error handling
   private void showLoadingView() {
      try {
         FXMLLoader loader = Fxml.loader("/fxml_files/loading-view.fxml");
         Pane root = new Pane();
         root.setBackground(new Background(new BackgroundFill(Color.LIGHTBLUE, new CornerRadii(50), Insets.EMPTY)));
         root.getStyleClass().add("loading-view");
         root.getChildren().add(loader.load());
         Scene scene = new Scene(root);
         scene.setFill(null);

         Stage loadingStage = new Stage(StageStyle.TRANSPARENT);
         loadingStage.setScene(scene);

         // Make the window draggable
         final double[] xOffset = new double[1];
         final double[] yOffset = new double[1];
         scene.setOnMousePressed(event -> {
            xOffset[0] = event.getSceneX();
            yOffset[0] = event.getSceneY();
         });
         scene.setOnMouseDragged(event -> {
            loadingStage.setX(event.getScreenX() - xOffset[0]);
            loadingStage.setY(event.getScreenY() - yOffset[0]);
         });

         loadingStage.show();

         // automatically transition to the main view after a delay (simulate loading)
         javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(5));
         pause.setOnFinished(event -> {
            loadingStage.close();
            try {
               showMainView();
            } catch (Exception e) {
               e.printStackTrace();// the dialog below says what failed, the stack trace says why
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.loadApp"), true);
            }
         });
         pause.play();
      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showLoadingView method

   // Method to show the Main View
   public void showMainView() {
      try {
         primaryStage.setFullScreen(false);// the draw screen can be shown full screen
         FXMLLoader loader = Fxml.loader("/fxml_files/main-view.fxml");
         Scene scene = sceneOf(loader);
         primaryStage.setScene(scene);

         MainViewController mainViewController = loader.getController();
         mainViewController.setMainApp(this);

         primaryStage.setMinWidth(1040); // Set minimum width
         primaryStage.setMinHeight(600); // Set minimum height

         primaryStage.setOnCloseRequest(event -> {
            Platform.exit(); // This line will close the application when the window is closed
         });

         primaryStage.show();
      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showMainView method

   // Method to show the Draw View
   public void showDrawView(Item selectedItem) {
      try {
         FXMLLoader loader = Fxml.loader("/fxml_files/draw-view.fxml");
         Scene scene = sceneOf(loader);
         primaryStage.setScene(scene);
         primaryStage.setTitle(Messages.get("window.draw"));

         DrawController controller = loader.getController();
         controller.setItem(selectedItem);

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               e.printStackTrace();// the dialog below says what failed, the stack trace says why
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.loadDraw"), true);
            }
         });// end of setOnCloseRequest method

         primaryStage.setMinWidth(1000); // Set minimum width
         primaryStage.setMinHeight(800); // Set minimum height
         primaryStage.show();
      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showDrawView method

   // Method to show the View Item View
   public void showViewItemView(Item selectedItem) {
      try {
         FXMLLoader loader = Fxml.loader("/fxml_files/view-item-view.fxml");
         Scene scene = sceneOf(loader);
         primaryStage.setScene(scene);
         primaryStage.setTitle(Messages.get("window.viewItem"));

         ViewItemController controller = loader.getController();
         controller.setItem(selectedItem);

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               e.printStackTrace();// the dialog below says what failed, the stack trace says why
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.loadItem"), true);
            }
         });// end of setOnCloseRequest method

         primaryStage.setMinWidth(1000); // Set minimum width
         primaryStage.setMinHeight(800); // Set minimum height
         primaryStage.show();
      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showViewItemView method

   // Method to show the Add Item View
   public void showAddItemView() {
      try {
         FXMLLoader loader = Fxml.loader("/fxml_files/add-item-view.fxml");
         Scene scene = sceneOf(loader);
         primaryStage.setScene(scene);
         primaryStage.setTitle(Messages.get("window.addItem"));

         primaryStage.setMinWidth(900); // set minimum width
         primaryStage.setMinHeight(800); // set minimum height

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               e.printStackTrace();// the dialog below says what failed, the stack trace says why
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.loadAddItem"), true);
            }
         });// end of setOnCloseRequest method

         primaryStage.show();// show the stage
      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showAddItemView method

   public void showAddPlayerView(Item selectedItem) {
      try {
         String userHome = System.getProperty("user.home");
         Path dataFilePath = Paths.get(userHome, "Sell & Win Raffle", "data", "data.csv");

         // Check if the data.csv file is accessible
         if (new AddPlayerController().isFileAccessibleForWriting(dataFilePath)) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.fileAccess"), Messages.get("err.fileOpen", "data.csv"), false);
            return; // Return early, do not open the Add Player window
         }

         FXMLLoader loader = Fxml.loader("/fxml_files/add-player-view.fxml");
         Scene scene = sceneOf(loader);
         primaryStage.setScene(scene);
         primaryStage.setTitle(Messages.get("window.addPlayer"));

         AddPlayerController controller = loader.getController();
         controller.setItem(selectedItem); // Set the selected item in the controller


         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               e.printStackTrace();// the dialog below says what failed, the stack trace says why
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.loadAddPlayer"), true);
            }
         });

         primaryStage.setMinWidth(1050); // Set minimum width
         primaryStage.setMinHeight(600); // Set minimum height
         primaryStage.show();

      } catch (Exception e) {
         e.printStackTrace();// the dialog below says what failed, the stack trace says why
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("app.err.unexpected"), true);
      }// end of try-catch block
   }// end of showAddPlayerView method

   // A scene whose root carries the stylesheet and the current theme
   private static Scene sceneOf(FXMLLoader loader) throws java.io.IOException {
      Parent root = loader.load();
      Theme.apply(root);
      return new Scene(root);
   }// end of sceneOf method

   // Method to show an alert dialog, with an option to close the application based on a flag
   private void showAlert(Alert.AlertType alertType, String title, String message, boolean shouldCloseApp) {
      Platform.runLater(() -> {
         Alert alert = Dialogs.build(alertType, title, null, message);

         // Add a custom icon to the alert dialog
         alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && shouldCloseApp) {
               Platform.exit(); // Close the application if OK is pressed and shouldCloseApp is true
            }
         });// end of showAndWait method
      });// end of Platform.runLater method
   }// end of showAlert method

   // Entry point of the application
   public static void main(String[] args) {
      launch(args);
   }// end of main method
}// end of App class
