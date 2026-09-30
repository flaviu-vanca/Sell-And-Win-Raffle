package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import raffle.models.Item;
import raffle.models.Player;
import raffle.utils.AppPaths;
import raffle.utils.ImageFiles;
import raffle.utils.ItemImages;
import raffle.utils.ItemDataReaderAndWriter;
import raffle.utils.PlayerDataReaderAndWriter;
import raffle.utils.Messages;
import raffle.ui.Dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

   private Path chosenImage;
   private String title;

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
      title = titleTextField.getText();// get the text from the titleTextField
      title = titleTextField.getText().trim();// remove leading and trailing whitespaces
      title = title.replaceAll("[<>:\"/\\\\|?*]", "_");// replace invalid characters with underscore
      String description = descriptionTextField.getText();
      String numberOfTicketsText = numberOfTicketsTextField.getText();
      String ticketPriceText = ticketPriceTextField.getText();

      // Validate inputs
      if (title.isEmpty()) {
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

      // Validate the number of tickets
      int numberOfTickets;
      try {
         numberOfTickets = Integer.parseInt(numberOfTicketsText);
         if (numberOfTickets <= 0) {
            showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsPositiveInt"));
            return;
         }
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsInvalid"));
         return;
      }// end of try-catch block

      // Validate the ticket price
      double ticketPrice;
      try {
         ticketPrice = Double.parseDouble(ticketPriceText);
         if (ticketPrice <= 0) {
            showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.pricePositive"));
            return;
         }
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.priceInvalid"));
         return;
      }// end of try-catch block

      Path recordsPath = AppPaths.recordsDir();
      Path csvFilePath = AppPaths.recordsFile(title);

      // Check if directory already exists
      Path path = AppPaths.itemDir(title);
      if (Files.exists(path)) {
         showAlert(Alert.AlertType.WARNING, Messages.get("additem.dirExists.title"), Messages.get("additem.dirExists", title));
         return;
      }

      // Check if CSV file already exists
      if (Files.exists(csvFilePath)) {
         showAlert(Alert.AlertType.WARNING, Messages.get("additem.recordExists.title"), Messages.get("additem.recordExists", title, recordsPath));
         return;
      }

      // Create the directory and the records directory
      Path copiedImage = null;
      try {
         Files.createDirectories(path);
         Files.createDirectories(recordsPath);

         // The chosen picture goes into the item's own folder
         if (chosenImage != null) {
            copiedImage = ImageFiles.copyInto(path, chosenImage);
         }// end of if block

         // One empty ledger row per ticket
         PlayerDataReaderAndWriter.writePlayersToCSV(generateEmptyTickets(numberOfTickets), csvFilePath);

         // Create a new item (without a picture it is stored as empty text and the screens show the logo)
         Item newItem = new Item(copiedImage == null ? "" : copiedImage.toString(), title, description, numberOfTickets, ticketPrice);

         // Add it to the catalog, creating the data directory and data.csv on first use
         Path dataFilePath = AppPaths.catalogFile();
         Files.createDirectories(dataFilePath.getParent());
         List<Item> items = Files.exists(dataFilePath) ? ItemDataReaderAndWriter.readItemsFromFile(dataFilePath) : new ArrayList<>();
         items.add(newItem);
         ItemDataReaderAndWriter.writeItemsToCSV(items, dataFilePath);
      } catch (IOException e) {
         // Do not leave a half created item behind: it would block adding the same title again
         try {
            Files.deleteIfExists(csvFilePath);
            if (copiedImage != null) {
               Files.deleteIfExists(copiedImage);
            }// end of if block
            Files.deleteIfExists(path);
         } catch (IOException ignored) {
            // nothing more can be done here
         }// end of try-catch block
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("additem.err.create"));
         return;
      }// end of try-catch block

      // Show a confirmation message
      showAlert(Alert.AlertType.CONFIRMATION, Messages.get("additem.added.title"), Messages.get("additem.added", title));
      handleClearFields();

   }// end of handleAddItem method

   // Generate the ledger rows for the item: every ticket starts unsold (empty name and phone)
   private List<Player> generateEmptyTickets(int numberOfTickets) {
      return IntStream.rangeClosed(1, numberOfTickets)
                      .mapToObj(id -> new Player(id, "", "", 0))
                      .collect(Collectors.toList());
   }// end of generateEmptyTickets method

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
