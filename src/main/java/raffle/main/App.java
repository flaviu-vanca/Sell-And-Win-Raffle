package raffle.main;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
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
import raffle.utils.AppPaths;
import raffle.utils.BackupService;
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
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/loading-view.fxml"));
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
               showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while loading the application !", true);
            }
         });
         pause.play();
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showLoadingView method

   // Method to show the Main View
   public void showMainView() {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/main-view.fxml"));
         Scene scene = new Scene(loader.load());
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
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showMainView method

   // Method to show the Draw View
   public void showDrawView(Item selectedItem) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/draw-view.fxml"));
         Scene scene = new Scene(loader.load());
         primaryStage.setScene(scene);
         primaryStage.setTitle("Draw");

         DrawController controller = loader.getController();
         controller.setItem(selectedItem);

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while loading Draw View !", true);
            }
         });// end of setOnCloseRequest method

         primaryStage.setMinWidth(1000); // Set minimum width
         primaryStage.setMinHeight(800); // Set minimum height
         primaryStage.show();
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showDrawView method

   // Method to show the View Item View
   public void showViewItemView(Item selectedItem) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/view-item-view.fxml"));
         Scene scene = new Scene(loader.load());
         primaryStage.setScene(scene);
         primaryStage.setTitle("View Item");

         ViewItemController controller = loader.getController();
         controller.setItem(selectedItem);

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while loading Item View !", true);
            }
         });// end of setOnCloseRequest method

         primaryStage.setMinWidth(1000); // Set minimum width
         primaryStage.setMinHeight(800); // Set minimum height
         primaryStage.show();
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showViewItemView method

   // Method to show the Add Item View
   public void showAddItemView() {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/add-item-view.fxml"));
         Scene scene = new Scene(loader.load());
         primaryStage.setScene(scene);
         primaryStage.setTitle("Add Item");

         primaryStage.setMinWidth(900); // set minimum width
         primaryStage.setMinHeight(800); // set minimum height

         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while loading Add Item View !", true);
            }
         });// end of setOnCloseRequest method

         primaryStage.show();// show the stage
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showAddItemView method

   public void showAddPlayerView(Item selectedItem) {
      try {
         String userHome = System.getProperty("user.home");
         Path dataFilePath = Paths.get(userHome, "Sell & Win Raffle", "data", "data.csv");

         // Check if the data.csv file is accessible
         if (new AddPlayerController().isFileAccessibleForWriting(dataFilePath)) {
            showAlert(Alert.AlertType.ERROR, "File Access Error", "The data.csv file is open in another application. Please close it and try again.", false);
            return; // Return early, do not open the Add Player window
         }

         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/add-player-view.fxml"));
         Scene scene = new Scene(loader.load());
         primaryStage.setScene(scene);
         primaryStage.setTitle("Add Player");

         AddPlayerController controller = loader.getController();
         controller.setItem(selectedItem); // Set the selected item in the controller


         primaryStage.setOnCloseRequest(event -> {
            event.consume(); // This line prevents the window from closing
            try {
               showMainView();
               primaryStage.setTitle("Sell & Win Raffle");
            } catch (Exception e) {
               showAlert(Alert.AlertType.ERROR, "Error", "An error occurred while loading Add Player View !", true);
            }
         });

         primaryStage.setMinWidth(1050); // Set minimum width
         primaryStage.setMinHeight(600); // Set minimum height
         primaryStage.show();

      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, "Error", "An unexpected error occurred during the loading process !", true);
      }// end of try-catch block
   }// end of showAddPlayerView method

   // Method to show an alert dialog, with an option to close the application based on a flag
   private void showAlert(Alert.AlertType alertType, String title, String message, boolean shouldCloseApp) {
      Platform.runLater(() -> {
         Alert alert = new Alert(alertType);
         alert.setTitle(title);
         alert.setHeaderText(null);
         alert.setContentText(message);

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
