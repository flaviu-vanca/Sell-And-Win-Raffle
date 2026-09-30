package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import raffle.main.App;
import raffle.models.Item;
import raffle.models.Player;
import raffle.services.ItemService;
import raffle.services.TicketSales;
import raffle.services.ValidationException;
import raffle.storage.Storage;
import raffle.ui.Dialogs;
import raffle.utils.AppPaths;
import raffle.utils.ItemImages;
import raffle.utils.Messages;
import raffle.utils.Money;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/** Changes the description, the price and the number of tickets of an item that already exists. */
public class EditItemController {

   @FXML
   private ImageView itemImageView;
   @FXML
   private Label titleLabel;
   @FXML
   private Label hintLabel;
   @FXML
   private TextField descriptionTextField;
   @FXML
   private TextField numberOfTicketsTextField;
   @FXML
   private TextField ticketPriceTextField;
   @FXML
   private Button saveButton;
   @FXML
   private Button cancelButton;

   private final ItemService items = new ItemService(Storage.repository(), AppPaths.root());
   private App app;
   private Item item;

   @FXML
   private void initialize() {
      addTooltip(titleLabel, Messages.get("edit.tip.title"));
      addTooltip(descriptionTextField, Messages.get("additem.tip.description"));
      addTooltip(numberOfTicketsTextField, Messages.get("edit.tip.tickets"));
      addTooltip(ticketPriceTextField, Messages.get("edit.tip.price"));
      addTooltip(saveButton, Messages.get("edit.tip.save"));
      addTooltip(cancelButton, Messages.get("edit.tip.cancel"));
   }// end of initialize method

   public void setMainApp(App app) {
      this.app = app;
   }// end of setMainApp method

   public void setItem(Item selectedItem) {
      this.item = selectedItem;
      titleLabel.setText(selectedItem.getTitle());
      itemImageView.setImage(ItemImages.load(selectedItem.getImage(), 800, 480));
      descriptionTextField.setText(selectedItem.getDescription());
      ticketPriceTextField.setText(BigDecimal.valueOf(selectedItem.getPrice()).stripTrailingZeros().toPlainString());

      int total = selectedItem.getTickets();
      int highestSold = 0;
      int sold = 0;
      try {
         List<Player> ledger = Storage.repository().ledger(selectedItem.getTitle());
         total = ledger.size();
         sold = (int) ledger.stream().filter(Player::isSold).count();
         highestSold = TicketSales.highestSoldId(ledger);
      } catch (IOException e) {
         Dialogs.showLater(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.loadRecords", selectedItem.getTitle()));
      }// end of try-catch block
      numberOfTicketsTextField.setText(String.valueOf(total));
      hintLabel.setText(Messages.get("edit.hint", String.valueOf(sold), String.valueOf(Math.max(1, highestSold))));
   }// end of setItem method

   @FXML
   private void handleSave() {
      String description = descriptionTextField.getText().trim();
      String ticketsText = numberOfTicketsTextField.getText().trim();
      String priceText = ticketPriceTextField.getText().trim();

      if (description.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.descriptionEmpty"));
         return;
      }
      if (ticketsText.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsEmpty"));
         return;
      }
      if (priceText.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.priceEmpty"));
         return;
      }

      int tickets;
      try {
         tickets = Integer.parseInt(ticketsText);
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsInvalid"));
         return;
      }// end of try-catch block

      double price;
      try {
         price = Money.parse(priceText);
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.priceInvalid"));
         return;
      }// end of try-catch block

      try {
         items.update(item.getTitle(), description, tickets, price);
      } catch (ValidationException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get(e.messageKey(), e.arguments()));
         return;
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("edit.err.save"));
         return;
      }// end of try-catch block

      showAlert(Alert.AlertType.CONFIRMATION, Messages.get("edit.saved.title"), Messages.get("edit.saved", item.getTitle()));
      app.showMainView();
   }// end of handleSave method

   @FXML
   private void handleCancel() {
      app.showMainView();
   }// end of handleCancel method

   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Dialogs.show(alertType, title, message);
   }// end of showAlert method

   private void addTooltip(Control control, String text) {
      control.setTooltip(new Tooltip(text));
   }// end of addTooltip method

}// end of EditItemController class
