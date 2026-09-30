package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import raffle.services.ItemService;
import raffle.services.ValidationException;
import raffle.storage.Storage;
import raffle.utils.AppPaths;
import raffle.utils.ImageFiles;
import raffle.utils.ItemImages;
import raffle.utils.Messages;
import raffle.utils.Money;
import raffle.ui.Dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AddItemController {

   @FXML
   private TextField titleTextField;
   @FXML
   private TextField descriptionTextField;
   @FXML
   private TextField numberOfTicketsTextField;
   @FXML
   private TextField ticketPriceTextField;
   @FXML
   private Button addDDefaultImageButton;
   @FXML
   private Button addItemButton;
   @FXML
   private Button clearFieldsButton;
   @FXML
   private ImageView itemImageView;

   private final ItemService items = new ItemService(Storage.repository(), AppPaths.root());
   private Path chosenImage;

   @FXML
   public void initialize() {
      // Add tooltips to the controls
      addTooltip(titleTextField, Messages.get("additem.tip.title"));
      addTooltip(descriptionTextField, Messages.get("additem.tip.description"));
      addTooltip(numberOfTicketsTextField, Messages.get("additem.tip.tickets"));
      addTooltip(ticketPriceTextField, Messages.get("additem.tip.price"));
      addTooltip(addDDefaultImageButton, Messages.get("additem.tip.image"));
      addTooltip(addItemButton, Messages.get("additem.tip.add"));
      addTooltip(clearFieldsButton, Messages.get("additem.tip.clear"));

   }// end of initialize method

   // Pick the item's picture from anywhere on the computer. It is copied into the item's folder when the item is
   // added, so nothing has to be copied by hand.
   @FXML
   private void handleAddImage() {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(Messages.get("additem.chooser.title"));
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(Messages.get("additem.chooser.filter"), ImageFiles.chooserPatterns()));

      File pictures = new File(System.getProperty("user.home"), "Pictures");
      File start = pictures.isDirectory() ? pictures : new File(System.getProperty("user.home"));
      if (start.isDirectory()) {
         fileChooser.setInitialDirectory(start);
      }// end of if block

      File selectedFile = fileChooser.showOpenDialog(addDDefaultImageButton.getScene().getWindow());
      if (selectedFile != null) {
         chosenImage = selectedFile.toPath();
         itemImageView.setImage(ItemImages.load(selectedFile.getAbsolutePath(), 800, 600));
      }// end of if block
   }// end of handleAddImage method

   @FXML
   private void handleAddItem() {
      String title = titleTextField.getText();
      String description = descriptionTextField.getText();
      String numberOfTicketsText = numberOfTicketsTextField.getText().trim();
      String ticketPriceText = ticketPriceTextField.getText().trim();

      // What was typed: is everything filled in, and are the numbers numbers?
      if (ItemService.cleanTitle(title).isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.titleEmpty"));
         return;
      }
      if (description.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.descriptionEmpty"));
         return;
      }
      if (numberOfTicketsText.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsEmpty"));
         return;
      }
      if (ticketPriceText.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.priceEmpty"));
         return;
      }

      int numberOfTickets;
      try {
         numberOfTickets = Integer.parseInt(numberOfTicketsText);
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsInvalid"));
         return;
      }// end of try-catch block

      double ticketPrice;
      try {
         ticketPrice = Money.parse(ticketPriceText);
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.priceInvalid"));
         return;
      }// end of try-catch block

      // The rules (positive numbers, title not taken) and the saving are the item service's job
      try {
         title = items.create(title, description, numberOfTickets, ticketPrice, chosenImage).getTitle();
      } catch (ValidationException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get(e.messageKey(), e.arguments()));
         return;
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("additem.err.create"));
         return;
      }// end of try-catch block

      // Show a confirmation message
      showAlert(Alert.AlertType.CONFIRMATION, Messages.get("additem.added.title"), Messages.get("additem.added", title));
      handleClearFields();

   }// end of handleAddItem method

   // Show an alert dialog
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Dialogs.show(alertType, title, message);
   }// end of showAlert method

   // Clear all the fields
   @FXML
   private void handleClearFields() {
      titleTextField.clear();
      descriptionTextField.clear();
      numberOfTicketsTextField.clear();
      ticketPriceTextField.clear();
      itemImageView.setImage(ItemImages.load(null, 800, 600));// back to the logo
      chosenImage = null;
   }// end of handleClearFields method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

}// end of AddItemController class
