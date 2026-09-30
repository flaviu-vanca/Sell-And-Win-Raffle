package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import raffle.models.Player;
import raffle.services.TicketLookup;
import raffle.storage.Storage;
import raffle.utils.Messages;
import raffle.ui.Dialogs;

import java.io.IOException;
import java.util.List;

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
      String input = playerInput.getText().trim();
      if (input.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.warning"), Messages.get("status.enterQuery"));
         return;
      }// end of if statement

      TicketLookup.Result result;
      try {
         result = TicketLookup.find(Storage.repository().ledger(itemTitle), input);
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("status.err.read"));
         return;
      }// end of try-catch block

      displayPlayerStatus.setText(describe(result));
   }// end of handleCheckPlayer method

   // The text shown for what was found
   private static String describe(TicketLookup.Result result) {
      return switch (result) {
         case TicketLookup.Result.NoTickets ignored -> "\n\n\t" + Messages.get("status.notFoundAny");
         case TicketLookup.Result.UnknownTicket unknown -> "\n\n" + Messages.get("status.notFoundId");
         case TicketLookup.Result.TicketNotSold notSold -> "\n\n" + Messages.get("status.ticketNotSold", String.valueOf(notSold.id()));
         case TicketLookup.Result.TicketFound found -> Messages.get("status.found",
                                                                    found.ticket().getName(),
                                                                    found.ticket().getPhoneNumber(),
                                                                    String.valueOf(found.ticket().getNumberOfTickets()),
                                                                    String.valueOf(found.ticket().getId()),
                                                                    paymentText(List.of(found.ticket())));
         case TicketLookup.Result.PhoneFound found -> {
            StringBuilder text = new StringBuilder(Messages.get("status.phoneHeader", found.phone()));
            for (TicketLookup.Buyer buyer : found.buyers()) {
               text.append("\n").append(Messages.get("status.block", buyer.name(), String.valueOf(buyer.tickets().size()),
                                                      buyer.ticketIds().toString(), paymentText(buyer.tickets())));
            }// end of for loop
            yield text.toString();
         }
         case TicketLookup.Result.NameFound found -> {
            StringBuilder text = new StringBuilder();
            for (TicketLookup.Buyer buyer : found.buyers()) {
               text.append(Messages.get("status.blockPhone", buyer.name(), buyer.phone(), String.valueOf(buyer.tickets().size()),
                                        buyer.ticketIds().toString(), paymentText(buyer.tickets()))).append("\n");
            }// end of for loop
            yield text.toString();
         }
         case TicketLookup.Result.NotFound ignored -> "\n\n\t" + Messages.get("status.notFoundNamePhone");
      };
   }// end of describe method

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
      Dialogs.show(alertType, title, message);
   }// end of showAlert method

}// end of PlayerStatusController class
