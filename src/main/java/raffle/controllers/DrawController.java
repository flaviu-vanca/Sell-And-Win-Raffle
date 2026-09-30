package raffle.controllers;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import raffle.models.Item;
import raffle.models.Player;
import raffle.services.DrawHistory;
import raffle.services.RaffleDraw;
import raffle.utils.AppPaths;
import raffle.utils.Messages;
import raffle.utils.PlayerDataReaderAndWriter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class DrawController {

   // Fast roll while the operator lets the numbers run
   private static final Duration ROLL_INTERVAL = Duration.millis(70);
   // After Stop the numbers slow down and come to rest on the winner
   private static final int LANDING_STEPS = 14;

   @FXML
   private Label generatedNumber;

   @FXML
   private Label winerLabel;

   @FXML
   private Button checkPlayerStatus;

   @FXML
   private Button startStopButton;

   private final RaffleDraw raffleDraw = new RaffleDraw();
   // Only used to animate the numbers on screen. It never decides the winner.
   private final Random animationRandom = new Random();
   private final DrawHistory drawHistory = new DrawHistory(AppPaths.drawHistoryFile());

   private List<Player> soldTickets = List.of();
   private String itemTitle;
   private Timeline rollTimeline;
   private Timeline landingTimeline;

   @FXML
   private void initialize() {
      addTooltip(checkPlayerStatus, "Check Player's Status");
      addTooltip(startStopButton, "Start the Number Generation.\nStop the Number Generation to Pick a Winner");
      addTooltip(generatedNumber, "Last Generated Number");
      addTooltip(winerLabel, "Winner of the Draw");
      startStopButton.setText(Messages.get("draw.start"));
      winerLabel.setWrapText(true);// longer translations must wrap instead of being cut off with "..."

      // Stop the animations when this view is replaced by another scene, otherwise they keep running unseen
      generatedNumber.sceneProperty().addListener((observable, oldScene, scene) -> {
         if (scene != null) {
            scene.windowProperty().addListener((obs, oldWindow, window) -> {
               if (window == null) {
                  stopAnimations();
               }// end of if block
            });
         }// end of if block
      });
   }// end of initialize method

   @FXML
   private void handleStartStop() {
      if (isRunning(landingTimeline)) {
         return; // the numbers are already coming to rest on the winner
      }// end of if block

      if (isRunning(rollTimeline)) {
         stopAndPickWinner();
      } else {
         startRolling();
      }// end of if-else block
   }// end of handleStartStop method

   // Start the fast number roll
   private void startRolling() {
      if (soldTickets.isEmpty()) {
         return;
      }// end of if block

      rollTimeline = new Timeline(new KeyFrame(ROLL_INTERVAL, event -> generatedNumber.setText(randomSoldTicketId())));
      rollTimeline.setCycleCount(Animation.INDEFINITE);
      rollTimeline.play();

      winerLabel.setText(Messages.get("draw.rolling"));
      startStopButton.setText(Messages.get("draw.stop"));
      startStopButton.getTooltip().setText("Stop the number generation and pick a winner !");
   }// end of startRolling method

   // Pick the winner at the moment of Stop, then let the numbers slow down and land on it
   private void stopAndPickWinner() {
      rollTimeline.stop();

      Optional<Player> winner = raffleDraw.pickWinner(soldTickets, Set.of());
      if (winner.isEmpty()) {
         resetButton();
         return;
      }// end of if block

      startStopButton.setDisable(true);
      landingTimeline = new Timeline();
      double elapsed = 0;
      for (int step = 0; step < LANDING_STEPS; step++) {
         elapsed += 40 + 2.5 * step * step;// each step takes longer than the one before
         landingTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(elapsed), event -> generatedNumber.setText(randomSoldTicketId())));
      }// end of for loop
      elapsed += 40 + 2.5 * LANDING_STEPS * LANDING_STEPS;
      landingTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(elapsed), event -> showWinner(winner.get())));
      landingTimeline.play();
   }// end of stopAndPickWinner method

   private void showWinner(Player winner) {
      generatedNumber.setText(String.valueOf(winner.getId()));
      winerLabel.setText(Messages.get("draw.winner") + "\n" + winner.getName() + "\n" + Messages.get("draw.ticketId", String.valueOf(winner.getId())));
      resetButton();

      try {
         drawHistory.record(itemTitle, winner, Instant.now());
      } catch (IOException e) {
         showAlert(Alert.AlertType.WARNING, "Warning", Messages.get("draw.saveFailed"));
      }// end of try-catch block
   }// end of showWinner method

   private void resetButton() {
      startStopButton.setDisable(false);
      startStopButton.setText(Messages.get("draw.start"));
      startStopButton.getTooltip().setText("Start the number generation !");
   }// end of resetButton method

   private String randomSoldTicketId() {
      return String.valueOf(soldTickets.get(animationRandom.nextInt(soldTickets.size())).getId());
   }// end of randomSoldTicketId method

   private void stopAnimations() {
      if (rollTimeline != null) {
         rollTimeline.stop();
      }// end of if block
      if (landingTimeline != null) {
         landingTimeline.stop();
      }// end of if block
   }// end of stopAnimations method

   private static boolean isRunning(Animation animation) {
      return animation != null && animation.getStatus() == Animation.Status.RUNNING;
   }// end of isRunning method

   // Method to check the player status
   @FXML
   private void handleCheckPlayerStatus() throws Exception {
      // Get the current window and hide it
      Stage stage = (Stage) checkPlayerStatus.getScene().getWindow();
      stage.hide();

      // Load the FXML file for the Player Status window
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml_files/player-status-view.fxml"));

      // Create a new scene with the loaded FXML file
      Scene scene = new Scene(loader.load());

      // Create a new stage for the Player Status window
      Stage newStage = new Stage();
      newStage.setTitle("Player Status");
      newStage.setScene(scene);

      // Get the controller for the Player Status window and set the main app
      PlayerStatusController controller = loader.getController();
      controller.setItem(itemTitle);

      // Show the Player Status window and show the Draw window when it's closed
      newStage.showAndWait();

      // Show the Draw window
      stage.show();
   }// end of handleCheckPlayerStatus method

   // Method to add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

   // Method to show an alert. Deferred because dialogs cannot be opened while an animation is being processed.
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Platform.runLater(() -> {
         Alert alert = new Alert(alertType);
         alert.setTitle(title);
         alert.setHeaderText(null);
         alert.setContentText(message);
         alert.showAndWait();
      });
   }// end of showAlert method

   // Set the item in the controller
   public void setItem(Item selectedItem) {
      if (selectedItem == null || selectedItem.getTitle() == null) {
         showAlert(Alert.AlertType.ERROR, "Error", "Item title is null. Please set the item title before setting the item !");
         return;
      }// end of if block

      this.itemTitle = selectedItem.getTitle();
      int totalTickets = 0;
      try {
         List<Player> ledger = PlayerDataReaderAndWriter.readPlayersFromFile(AppPaths.recordsFile(itemTitle));
         totalTickets = ledger.size();
         soldTickets = RaffleDraw.soldTickets(ledger);
      } catch (IOException | NumberFormatException e) {
         soldTickets = List.of();
         showAlert(Alert.AlertType.ERROR, "Error", Messages.get("draw.loadFailed"));
      }// end of try-catch block

      generatedNumber.setText(Messages.get("draw.unknownNumber"));
      if (soldTickets.isEmpty()) {
         winerLabel.setText(Messages.get("draw.noneSold"));
         startStopButton.setDisable(true);
      } else {
         winerLabel.setText(itemTitle + "\n" + Messages.get("draw.soldCount", String.valueOf(soldTickets.size()), String.valueOf(totalTickets)));
      }// end of if-else block
   }// end of setItem method

}// end of DrawController class
