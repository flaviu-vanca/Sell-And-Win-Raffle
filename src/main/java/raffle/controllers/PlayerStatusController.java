package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import raffle.models.Player;
import raffle.utils.AppPaths;
import raffle.utils.Messages;
import raffle.utils.PhoneNumbers;
import raffle.utils.PlayerDataReaderAndWriter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PlayerStatusController {

   @FXML
   private TextField playerInput;

   @FXML
   private TextArea displayPlayerStatus;

   @FXML
   private Button checkPlayerStatus;

   private String itemTitle;

   @FXML
   private void initialize() {
      // tooltip for the checkPlayerStatus button
      addTooltip(checkPlayerStatus, Messages.get("status.tip.check"));
      addTooltip(playerInput, Messages.get("status.tip.input"));
      addTooltip(displayPlayerStatus, Messages.get("status.tip.output"));
   }//end of initialize method

   @FXML
   private void handleCheckPlayer() {
      // Get the player input
      String input = playerInput.getText().trim();

      // Check if the input is empty
      if (input.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.warning"), Messages.get("status.enterQuery"));
         return;
      }// end of if statement

      // Phone numbers are stored without spaces or dashes, so compare the same way
      String phoneInput = PhoneNumbers.normalize(input);

      try {
         List<Player> matchingPlayers = PlayerDataReaderAndWriter.readPlayersFromFile(AppPaths.recordsFile(itemTitle));

         // Check if the player is found
         if (matchingPlayers.isEmpty()) {
            displayPlayerStatus.setText("\n\n\t" + Messages.get("status.notFoundAny"));
            return;
         }// end of if statement

         // Check if the input is numeric
         boolean isNumeric = phoneInput.chars().allMatch(Character::isDigit);

         // a number shorter than a phone number (7 digits) is a ticket ID
         if (isNumeric && phoneInput.length() < 7) {

            // Search by ID
            int inputId = Integer.parseInt(phoneInput);
            Player matchingPlayer = matchingPlayers.stream()
                                                   .filter(player -> player.getId() == inputId)
                                                   .findFirst()
                                                   .orElse(null);

            if (matchingPlayer == null) {
               displayPlayerStatus.setText("\n\n" + Messages.get("status.notFoundId"));
            } else if (! matchingPlayer.isSold()) {
               displayPlayerStatus.setText("\n\n" + Messages.get("status.ticketNotSold", String.valueOf(inputId)));
            } else {
               displayPlayerStatus.setText(Messages.get("status.found",
                                                        matchingPlayer.getName(),
                                                        matchingPlayer.getPhoneNumber(),
                                                        String.valueOf(matchingPlayer.getNumberOfTickets()),
                                                        String.valueOf(matchingPlayer.getId()),
                                                        paymentText(List.of(matchingPlayer))));
            }// end of if/else block
         } else {
            Map<String, List<Player>> playersByPhoneNumber = matchingPlayers.stream()
                                                                            .collect(Collectors.groupingBy(Player::getPhoneNumber));

            // check if the map contains the input key (phone number)
            if (playersByPhoneNumber.containsKey(phoneInput)) {

               // Display players with the given phone number
               List<Player> playersWithPhone = playersByPhoneNumber.get(phoneInput);
               StringBuilder resultText = new StringBuilder();
               resultText.append(Messages.get("status.phoneHeader", phoneInput));

               // Group players by name
               Map<String, List<Player>> playersByName = playersWithPhone.stream()
                                                                         .collect(Collectors.groupingBy(Player::getName));
               // Display the result
               for (Map.Entry<String, List<Player>> entry : playersByName.entrySet()) {
                  String name = entry.getKey();
                  List<Player> players = entry.getValue();
                  int totalTickets = players.size();
                  List<Integer> ids = players.stream().map(Player::getId).collect(Collectors.toList());

                  resultText.append("\n").append(Messages.get("status.block", name, String.valueOf(totalTickets), ids.toString(), paymentText(players)));
               }// end of for loop
               // Display the result
               displayPlayerStatus.setText(resultText.toString());
            } else {
               // Display matching players by name and phone number separately
               StringBuilder resultText = new StringBuilder();
               // Group players by name and phone number
               Map<String, Map<String, List<Player>>> playersByNameAndPhone = matchingPlayers.stream()
                                                                                             .filter(player -> player.getName().equalsIgnoreCase(input))
                                                                                             .collect(Collectors.groupingBy(Player::getName, Collectors.groupingBy(Player::getPhoneNumber)));
               // Display the result
               for (Map.Entry<String, Map<String, List<Player>>> entry : playersByNameAndPhone.entrySet()) {
                  String name = entry.getKey();
                  Map<String, List<Player>> playersByPhone = entry.getValue();

                  for (Map.Entry<String, List<Player>> phoneEntry : playersByPhone.entrySet()) {
                     String phone = phoneEntry.getKey();
                     List<Player> players = phoneEntry.getValue();
                     int totalTickets = players.size();
                     List<Integer> ids = players.stream().map(Player::getId).collect(Collectors.toList());

                     resultText.append(Messages.get("status.blockPhone", name, phone, String.valueOf(totalTickets), ids.toString(), paymentText(players))).append("\n");
                  }// end of inner for loop
               }// end of for loop

               // Display the result
               if (resultText.isEmpty()) {
                  displayPlayerStatus.setText("\n\n\t" + Messages.get("status.notFoundNamePhone"));
               } else {
                  displayPlayerStatus.setText(resultText.toString());
               }// end of if block
            }// end of if block
         }// end of if block
      } catch (IOException | NumberFormatException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("status.err.read"));
      }// end of try catch block
   }// end of handleCheckPlayer method

   // "paid", "not paid" or "2 of 3 tickets paid" for the tickets of one buyer
   private static String paymentText(List<Player> tickets) {
      long paid = tickets.stream().filter(Player::isPaid).count();
      if (paid == tickets.size()) {
         return Messages.get("status.pay.all");
      }// end of if block
      return paid == 0 ? Messages.get("status.pay.none")
                       : Messages.get("status.pay.some", String.valueOf(paid), String.valueOf(tickets.size()));
   }// end of paymentText method

   // Method to add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

   // Set the item in the controller
   public void setItem(String itemTitle) {
      this.itemTitle = itemTitle;
   }// end of setItem method

   // Method to show an alert
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Alert alert = new Alert(alertType);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
   }// end of showAlert method

}// end of PlayerStatusController class
