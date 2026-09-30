package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import raffle.models.Item;
import raffle.models.Player;
import raffle.utils.AppPaths;
import raffle.utils.ItemDataReaderAndWriter;
import raffle.utils.PlayerDataReaderAndWriter;
import raffle.utils.Messages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
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

   private String imagePath;
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

   @FXML
   private void handleAddImage() {
      // Get the title from the title text field
      Path appDirectoryPath = AppPaths.root();
      Path dataFilePath = AppPaths.catalogFile();

      // Create a file chooser dialog
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(Messages.get("additem.chooser.title"));
      fileChooser.getExtensionFilters().addAll(
              new FileChooser.ExtensionFilter(Messages.get("additem.chooser.filter"), "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.tiff", "*.webp")
      );

      // Set initial directory to the app's directory
      fileChooser.setInitialDirectory(appDirectoryPath.toFile());

      // Show the file chooser dialog
      Stage stage = (Stage) addDDefaultImageButton.getScene().getWindow();
      File selectedFile = fileChooser.showOpenDialog(stage);

      if (selectedFile != null) {
         // Extract the title from the path
         Path relativePath = appDirectoryPath.relativize(selectedFile.toPath());
         String[] pathParts = relativePath.toString().split(Pattern.quote(File.separator));
         if (pathParts.length > 1) {
            title = pathParts[0];
         }// end of if statement

         // Check if the selected directory matches the title
         if (! selectedFile.getParentFile().getName().equals(title)) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("additem.err.wrongDir"));
            return;
         }// end of if statement

         imagePath = selectedFile.getAbsolutePath();
         Image image = new Image(selectedFile.toURI().toString());
         itemImageView.setImage(image);

         // Read the data.csv and match titles
         try {
            List<Item> existingItems = ItemDataReaderAndWriter.readItemsFromFile(dataFilePath);
            final String itemTitle = title;
            boolean updated = existingItems.stream()
                                           .filter(item -> item.getTitle().equals(itemTitle))
                                           .findFirst()
                                           .map(item -> {
                                              item.setImage(imagePath);// Update the image path
                                              return true;
                                           })
                                           .orElse(false);
            if (updated) {
               ItemDataReaderAndWriter.writeItemsToCSV(existingItems, dataFilePath);
               showAlert(Alert.AlertType.CONFIRMATION, Messages.get("additem.imageUpdated.title"), Messages.get("additem.imageUpdated"));
            } else {
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("additem.err.imagePath"));
            }// end of if-else block
         } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.updateCatalog"));
         }// end of try-catch block
      }// end of if statement
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
      try {
         Files.createDirectories(path);
         Files.createDirectories(recordsPath);

         // One empty ledger row per ticket
         PlayerDataReaderAndWriter.writePlayersToCSV(generateEmptyTickets(numberOfTickets), csvFilePath);

         // Create a new item (no image yet: stored as empty text, the main view shows the default logo)
         Item newItem = new Item(imagePath == null ? "" : imagePath, title, description, numberOfTickets, ticketPrice);

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
            Files.deleteIfExists(path);
         } catch (IOException ignored) {
            // nothing more can be done here
         }// end of try-catch block
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("additem.err.create"));
         return;
      }// end of try-catch block

      // Show a confirmation message
      showAlert(Alert.AlertType.CONFIRMATION, Messages.get("additem.added.title"), Messages.get("additem.added", title, path));

   }// end of handleAddItem method

   // Generate the ledger rows for the item: every ticket starts unsold (empty name and phone)
   private List<Player> generateEmptyTickets(int numberOfTickets) {
      return IntStream.rangeClosed(1, numberOfTickets)
                      .mapToObj(id -> new Player(id, "", "", 0))
                      .collect(Collectors.toList());
   }// end of generateEmptyTickets method

   // Show an alert dialog
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Alert alert = new Alert(alertType);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
   }// end of showAlert method

   // Clear all the fields
   @FXML
   private void handleClearFields() {
      titleTextField.clear();
      descriptionTextField.clear();
      numberOfTicketsTextField.clear();
      ticketPriceTextField.clear();
      itemImageView.setImage(null);
      imagePath = null;
   }// end of handleClearFields method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

}// end of AddItemController class
