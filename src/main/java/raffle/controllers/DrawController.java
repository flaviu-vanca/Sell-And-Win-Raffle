package raffle.controllers;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import raffle.models.Item;
import raffle.models.Player;
import raffle.services.DrawHistory;
import raffle.services.DrawSession;
import raffle.services.RaffleDraw;
import raffle.ui.ConfettiCanvas;
import raffle.utils.AppPaths;
import raffle.utils.Fxml;
import raffle.utils.ItemImages;
import raffle.utils.Messages;
import raffle.utils.PlayerDataReaderAndWriter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class DrawController {

   // The content is laid out at this size and scaled to the window, so it also fills a projector in full screen
   private static final double BASE_WIDTH = 1000;
   private static final double BASE_HEIGHT = 800;
   // Fast roll while the operator lets the numbers run
   private static final Duration ROLL_INTERVAL = Duration.millis(70);
   // After Stop the numbers slow down and come to rest on the winner
   private static final int LANDING_STEPS = 14;
   // How many earlier winners stay visible under the current one
   private static final int LISTED_WINNERS = 6;

   private enum Phase {IDLE, ROLLING, LANDING, DONE}

   @FXML
   private StackPane root;
   @FXML
   private Region content;
   @FXML
   private ImageView prizeImage;
   @FXML
   private Label prizeTitle;
   @FXML
   private Label prizeSubtitle;
   @FXML
   private Label generatedNumber;
   @FXML
   private Label winerLabel;
   @FXML
   private Label winnersListLabel;
   @FXML
   private Spinner<Integer> winnersSpinner;
   @FXML
   private CheckBox onePerPerson;
   @FXML
   private Button checkPlayerStatus;
   @FXML
   private Button fullscreenButton;
   @FXML
   private Button startStopButton;

   private final RaffleDraw raffleDraw = new RaffleDraw();
   // Only used to animate the numbers on screen. It never decides the winner.
   private final Random animationRandom = new Random();
   private final DrawHistory drawHistory = new DrawHistory(AppPaths.drawHistoryFile());
   private final ConfettiCanvas confetti = new ConfettiCanvas();

   private List<Player> ledger = List.of();
   private List<Player> soldTickets = List.of();
   private String itemTitle;
   private DrawSession session;
   private Phase phase = Phase.IDLE;
   private Timeline rollTimeline;
   private Timeline landingTimeline;

   @FXML
   private void initialize() {
      addTooltip(checkPlayerStatus, Messages.get("draw.tip.status"));
      addTooltip(generatedNumber, Messages.get("draw.tip.number"));
      addTooltip(winerLabel, Messages.get("draw.tip.winner"));
      addTooltip(winnersSpinner, Messages.get("draw.tip.winners"));
      addTooltip(onePerPerson, Messages.get("draw.tip.onePerPerson"));
      addTooltip(fullscreenButton, Messages.get("draw.tip.fullscreen"));
      addTooltip(startStopButton, Messages.get("draw.tip.startNow"));

      winnersSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1));
      winnersListLabel.setManaged(false);// only takes space once several winners are being drawn
      winnersListLabel.setVisible(false);

      // Confetti sits on top of everything and follows the window size
      confetti.widthProperty().bind(root.widthProperty());
      confetti.heightProperty().bind(root.heightProperty());
      root.getChildren().add(confetti);

      root.widthProperty().addListener((observable, oldWidth, newWidth) -> updateScale());
      root.heightProperty().addListener((observable, oldHeight, newHeight) -> updateScale());

      root.sceneProperty().addListener((observable, oldScene, scene) -> {
         if (scene != null) {
            scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
               if (event.getCode() == KeyCode.F11) {
                  handleToggleFullscreen();
                  event.consume();
               }// end of if block
            });
            // Stop the animations when this view is replaced by another scene, otherwise they keep running unseen
            scene.windowProperty().addListener((obs, oldWindow, window) -> {
               if (window == null) {
                  stopAnimations();
               }// end of if block
            });
            Platform.runLater(() -> startStopButton.requestFocus());// Space and Enter run the draw
         }// end of if block
      });
   }// end of initialize method

   // Keeps the fixed-size content filling the window, in a window and in full screen alike
   private void updateScale() {
      double scale = Math.min(root.getWidth() / BASE_WIDTH, root.getHeight() / BASE_HEIGHT);
      if (scale > 0) {
         content.setScaleX(scale);
         content.setScaleY(scale);
      }// end of if block
   }// end of updateScale method

   @FXML
   private void handleStartStop() {
      switch (phase) {
         case IDLE -> startRolling();
         case ROLLING -> stopAndPickWinner();
         case DONE -> newDraw();
         case LANDING -> {
            // the numbers are already coming to rest on the winner
         }
      }// end of switch
   }// end of handleStartStop method

   // Start the fast number roll
   private void startRolling() {
      if (soldTickets.isEmpty()) {
         return;
      }// end of if block

      if (session == null) {
         session = new DrawSession(raffleDraw, ledger, winnersSpinner.getValue(), onePerPerson.isSelected());
         winnersSpinner.setDisable(true);// the rules cannot change once winners have been drawn
         onePerPerson.setDisable(true);
      }// end of if block

      confetti.stop();
      generatedNumber.getStyleClass().remove("revealed");
      rollTimeline = new Timeline(new KeyFrame(ROLL_INTERVAL, event -> generatedNumber.setText(randomSoldTicketId())));
      rollTimeline.setCycleCount(Animation.INDEFINITE);
      rollTimeline.play();

      phase = Phase.ROLLING;
      winerLabel.setText(Messages.get("draw.rolling"));
      setButton(Messages.get("draw.stop"), Messages.get("draw.tip.stopNow"));
   }// end of startRolling method

   // Pick the winner at the moment of Stop, then let the numbers slow down and land on it
   private void stopAndPickWinner() {
      rollTimeline.stop();

      Optional<Player> winner = session.drawNext();
      if (winner.isEmpty()) {
         finishSession(Messages.get("draw.noMoreEligible"));
         return;
      }// end of if block

      phase = Phase.LANDING;
      startStopButton.setDisable(true);
      landingTimeline = new Timeline();
      double elapsed = 0;
      for (int step = 0; step < LANDING_STEPS; step++) {
         elapsed += 40 + 2.5 * step * step;// each step takes longer than the one before
         landingTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(elapsed), event -> generatedNumber.setText(randomSoldTicketId())));
      }// end of for loop
      elapsed += 40 + 2.5 * LANDING_STEPS * LANDING_STEPS;
      landingTimeline.getKeyFrames().add(new KeyFrame(Duration.millis(elapsed), event -> revealWinner(winner.get())));
      landingTimeline.play();
   }// end of stopAndPickWinner method

   private void revealWinner(Player winner) {
      List<Player> winners = session.winners();
      generatedNumber.setText(String.valueOf(winner.getId()));
      generatedNumber.getStyleClass().add("revealed");
      // Only the name and ticket are shown: the screen may be on a projector, so no phone numbers
      String heading = session.winnersWanted() > 1
              ? Messages.get("draw.winnerN", String.valueOf(winners.size()), String.valueOf(session.winnersWanted()))
              : Messages.get("draw.winner");
      winerLabel.setText(heading + "\n" + winner.getName() + "\n" + Messages.get("draw.ticketId", String.valueOf(winner.getId())));
      showWinnersList(winners);

      ScaleTransition pop = new ScaleTransition(Duration.millis(380), generatedNumber);
      pop.setFromX(1);
      pop.setFromY(1);
      pop.setToX(1.12);
      pop.setToY(1.12);
      pop.setCycleCount(2);
      pop.setAutoReverse(true);
      pop.setInterpolator(Interpolator.EASE_OUT);
      pop.play();
      confetti.burst();

      try {
         drawHistory.record(itemTitle, winner, Instant.now());
      } catch (IOException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.warning"), Messages.get("draw.saveFailed"));
      }// end of try-catch block

      startStopButton.setDisable(false);
      if (session.canDrawMore()) {
         phase = Phase.IDLE;
         setButton(Messages.get("draw.next"), Messages.get("draw.tip.startNow"));
      } else if (session.isComplete()) {
         finishSession(session.winnersWanted() > 1 ? Messages.get("draw.complete", String.valueOf(session.winnersWanted())) : null);
      } else {
         finishSession(Messages.get("draw.noMoreEligible"));
      }// end of if-else block
      startStopButton.requestFocus();
   }// end of revealWinner method

   // The wanted winners were drawn (or nobody eligible is left): the next click starts a fresh draw
   private void finishSession(String message) {
      phase = Phase.DONE;
      startStopButton.setDisable(false);
      setButton(Messages.get("draw.newDraw"), Messages.get("draw.tip.startNow"));
      if (message != null) {
         prizeSubtitle.setText(message);
      }// end of if block
   }// end of finishSession method

   // Forget this sitting's winners and go back to the starting screen. The draw history file is untouched.
   private void newDraw() {
      session = null;
      phase = Phase.IDLE;
      confetti.stop();
      generatedNumber.getStyleClass().remove("revealed");
      generatedNumber.setText(Messages.get("draw.unknownNumber"));
      winnersListLabel.setVisible(false);
      winnersListLabel.setManaged(false);
      winnersSpinner.setDisable(false);
      onePerPerson.setDisable(false);
      showItemSummary();
      setButton(Messages.get("draw.start"), Messages.get("draw.tip.startNow"));
   }// end of newDraw method

   // Earlier winners of this sitting, newest last, under the big current winner
   private void showWinnersList(List<Player> winners) {
      if (session.winnersWanted() <= 1) {
         return;
      }// end of if block
      StringBuilder text = new StringBuilder();
      int first = Math.max(0, winners.size() - LISTED_WINNERS);
      if (first > 0) {
         text.append(Messages.get("draw.more", String.valueOf(first))).append("\n");
      }// end of if block
      for (int i = first; i < winners.size(); i++) {
         Player winner = winners.get(i);
         text.append(Messages.get("draw.winnerLine", String.valueOf(i + 1), winner.getName(), String.valueOf(winner.getId())));
         if (i < winners.size() - 1) {
            text.append("\n");
         }// end of if block
      }// end of for loop
      winnersListLabel.setText(text.toString());
      winnersListLabel.setManaged(true);
      winnersListLabel.setVisible(true);
   }// end of showWinnersList method

   @FXML
   private void handleToggleFullscreen() {
      if (root.getScene() == null || ! (root.getScene().getWindow() instanceof Stage stage)) {
         return;
      }// end of if block
      stage.setFullScreenExitHint(Messages.get("draw.fullscreenHint"));
      stage.setFullScreen(! stage.isFullScreen());
      fullscreenButton.setText(Messages.get(stage.isFullScreen() ? "draw.btn.exitFullscreen" : "draw.btn.fullscreen"));
      startStopButton.requestFocus();// Space and Enter must keep running the draw, not toggle full screen again
   }// end of handleToggleFullscreen method

   private void setButton(String text, String tooltip) {
      startStopButton.setText(text);
      startStopButton.getTooltip().setText(tooltip);
   }// end of setButton method

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
      confetti.stop();
   }// end of stopAnimations method

   // Method to check the player status
   @FXML
   private void handleCheckPlayerStatus() throws Exception {
      // Get the current window and hide it
      Stage stage = (Stage) checkPlayerStatus.getScene().getWindow();
      stage.setFullScreen(false);
      stage.hide();

      // Load the FXML file for the Player Status window
      FXMLLoader loader = Fxml.loader("/fxml_files/player-status-view.fxml");

      // Create a new scene with the loaded FXML file
      Scene scene = new Scene(loader.load());

      // Create a new stage for the Player Status window
      Stage newStage = new Stage();
      newStage.setTitle(Messages.get("status.window.title"));
      newStage.setScene(scene);

      // Get the controller for the Player Status window and set the main app
      PlayerStatusController controller = loader.getController();
      controller.setItem(itemTitle);

      // Show the Player Status window and show the Draw window when it's closed
      newStage.showAndWait();

      // Show the Draw window
      stage.show();
      startStopButton.requestFocus();
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

   private void showItemSummary() {
      prizeTitle.setText(itemTitle);
      prizeSubtitle.setText(soldTickets.isEmpty()
                                    ? Messages.get("draw.noneSold")
                                    : Messages.get("draw.soldCount", String.valueOf(soldTickets.size()), String.valueOf(ledger.size())));
      winerLabel.setText(soldTickets.isEmpty() ? "" : Messages.get("draw.hint"));
   }// end of showItemSummary method

   // Set the item in the controller
   public void setItem(Item selectedItem) {
      if (selectedItem == null || selectedItem.getTitle() == null) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("draw.err.noTitle"));
         return;
      }// end of if block

      this.itemTitle = selectedItem.getTitle();
      prizeImage.setImage(ItemImages.load(selectedItem.getImage(), 170, 110));
      try {
         ledger = PlayerDataReaderAndWriter.readPlayersFromFile(AppPaths.recordsFile(itemTitle));
         soldTickets = RaffleDraw.soldTickets(ledger);
      } catch (IOException | NumberFormatException e) {
         ledger = List.of();
         soldTickets = List.of();
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("draw.loadFailed"));
      }// end of try-catch block

      generatedNumber.setText(Messages.get("draw.unknownNumber"));
      showItemSummary();
      startStopButton.setDisable(soldTickets.isEmpty());
   }// end of setItem method

}// end of DrawController class
