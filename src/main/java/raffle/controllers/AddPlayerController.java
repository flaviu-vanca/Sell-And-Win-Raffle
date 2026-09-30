package raffle.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import raffle.models.Item;
import raffle.models.Player;
import raffle.services.ItemSales;
import raffle.services.Payments;
import raffle.utils.AppPaths;
import raffle.utils.ItemDataReaderAndWriter;
import raffle.utils.Messages;
import raffle.utils.Money;
import raffle.utils.PhoneNumbers;
import raffle.utils.PlayerDataReaderAndWriter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AddPlayerController {

   @FXML
   private TableView<Player> playerTable;
   @FXML
   private TableColumn<Player, Integer> IDColumn;
   @FXML
   private TableColumn<Player, String> nameColumn;
   @FXML
   private TableColumn<Player, String> phoneNumberColumn;
   @FXML
   private TableColumn<Player, Integer> numberOfTicketsColumn;
   @FXML
   private TableColumn<Player, Boolean> paidColumn;
   @FXML
   private TableColumn<Player, String> soldAtColumn;
   @FXML
   private Label itemHeader;
   @FXML
   private Label salesSummary;
   @FXML
   private Label totalLabel;
   @FXML
   private CheckBox paidCheck;
   @FXML
   private Button markPaidButton;
   @FXML
   private Button markUnpaidButton;
   @FXML
   private TextField playerName;
   @FXML
   private TextField phoneNumber;
   @FXML
   private TextField numberOfTickets;
   @FXML
   private Label ticketsLeft;
   @FXML
   private Button addPlayerButton;
   @FXML
   private Button removePlayerButton;
   @FXML
   private Button clearFieldsButton;
   @FXML
   private Button refreshButton;

   private ObservableList<Player> playerList;
   private List<Integer> availableIDs;
   private String itemTitle;
   private double itemPrice;
   private Path recordsDirectory;

   @FXML
   private void initialize() {
      // Add tooltips to the controls
      addTooltip(playerName, Messages.get("player.tip.name"));
      addTooltip(phoneNumber, Messages.get("player.tip.phone"));
      addTooltip(numberOfTickets, Messages.get("player.tip.tickets"));
      addTooltip(ticketsLeft, Messages.get("player.tip.left"));
      addTooltip(addPlayerButton, Messages.get("player.tip.add"));
      addTooltip(clearFieldsButton, Messages.get("player.tip.clear"));
      addTooltip(refreshButton, Messages.get("player.tip.refresh"));
      addTooltip(removePlayerButton, Messages.get("player.tip.deleteOne"));
      addTooltip(markPaidButton, Messages.get("player.tip.markPaid"));
      addTooltip(markUnpaidButton, Messages.get("player.tip.markUnpaid"));
      addTooltip(totalLabel, Messages.get("player.tip.total"));
      addTooltip(paidCheck, Messages.get("player.tip.paidCheck"));

      // The amount to pay follows the number of tickets being typed
      numberOfTickets.textProperty().addListener((observable, oldText, newText) -> updateTotal());
      updateTotal();

      // Initialize the player table
      IDColumn.setCellValueFactory(cellData -> cellData.getValue().idProperty().asObject());
      nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
      phoneNumberColumn.setCellValueFactory(cellData -> cellData.getValue().phoneNumberProperty());
      numberOfTicketsColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfTicketsProperty().asObject());
      paidColumn.setCellValueFactory(cellData -> cellData.getValue().paidProperty());
      soldAtColumn.setCellValueFactory(cellData -> cellData.getValue().soldAtProperty());

      // Paid or unpaid, only for tickets that have a buyer; the colour comes from the stylesheet
      paidColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(Boolean paid, boolean empty) {
            super.updateItem(paid, empty);
            getStyleClass().removeAll("paid-yes", "paid-no");
            Player row = getTableRow() == null ? null : getTableRow().getItem();
            if (empty || paid == null || row == null || ! row.isSold()) {
               setText(null);
            } else {
               setText(Messages.get(paid ? "player.paid.yes" : "player.paid.no"));
               getStyleClass().add(paid ? "paid-yes" : "paid-no");
               setStyle("-fx-alignment: CENTER; -fx-font-size: 14px; -fx-font-weight: bold;");
            }// end of if-else block
         }
      });

      // When the ticket was sold, in the local format of the active language
      soldAtColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(String soldAt, boolean empty) {
            super.updateItem(soldAt, empty);
            setText(empty || soldAt == null || soldAt.isBlank() ? null : formatSoldAt(soldAt));
            setStyle("-fx-alignment: CENTER; -fx-font-size: 14px;");
         }
      });

      // Add a listener to the selection model of the playerTable
      playerTable.getSelectionModel().getSelectedItems().addListener((ListChangeListener.Change<? extends Player> change) -> {
         if (playerTable.getSelectionModel().getSelectedItems().size() > 1) {
            removePlayerButton.setText(Messages.get("player.btn.deleteMany"));
            addTooltip(removePlayerButton, Messages.get("player.tip.deleteMany"));
         } else {
            removePlayerButton.setText(Messages.get("player.btn.deleteOne"));
            addTooltip(removePlayerButton, Messages.get("player.tip.deleteOne"));
         }// end of if-else block
      });

      // Customize the display of the IDColumn
      IDColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(item.toString());
               setStyle("-fx-alignment: CENTER; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
         }
      });// end of setCellFactory method

      // Customize the display of the nameColumn
      nameColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(item);
               setStyle("-fx-alignment: CENTER; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
         }
      });

      // Customize the display of the phoneNumberColumn
      phoneNumberColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(item);
               setStyle("-fx-alignment: CENTER; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
         }
      });

      // Customize the display of the numberOfTicketsColumn
      numberOfTicketsColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(item.toString());
               setStyle("-fx-alignment: CENTER; -fx-font-size: 14px; -fx-font-weight: bold;");
            }
         }
      });

      // Initialize the player list
      playerList = FXCollections.observableArrayList();
      playerTable.setItems(playerList);

      // Enable multiple row selection
      playerTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

      // Adjust columns dynamically with the table's width (also applied once right away, not only after a resize)
      playerTable.widthProperty().addListener((obs, oldWidth, newWidth) -> applyColumnWidths(newWidth.doubleValue()));

      Platform.runLater(() -> {
         Stage stage = (Stage) playerTable.getScene().getWindow();
         applyColumnWidths(playerTable.getWidth());

         // Ensure the table resizes with the window
         stage.widthProperty().addListener((obs, oldVal, newVal) -> playerTable.setPrefWidth(newVal.doubleValue()));
         stage.heightProperty().addListener((obs, oldVal, newVal) -> playerTable.setPrefHeight(newVal.doubleValue()));
      });// end of Platform.runLater method
   }// end of initialize method

   // Columns share the table width; 4% is left for the vertical scrollbar so no horizontal scrollbar appears
   private void applyColumnWidths(double tableWidth) {
      if (tableWidth <= 0) {
         return;
      }// end of if block
      IDColumn.setPrefWidth(tableWidth * 0.06);
      nameColumn.setPrefWidth(tableWidth * 0.23);
      phoneNumberColumn.setPrefWidth(tableWidth * 0.17);
      numberOfTicketsColumn.setPrefWidth(tableWidth * 0.14);
      paidColumn.setPrefWidth(tableWidth * 0.13);
      soldAtColumn.setPrefWidth(tableWidth * 0.23);
   }// end of applyColumnWidths method

   // Load players from the records directory based on item title
   private void loadPlayersFromCSV() {
      recordsDirectory = AppPaths.recordsDir();
      Path dataFilePath = AppPaths.recordsFile(itemTitle);

      if (Files.exists(dataFilePath)) {
         try {
            List<Player> players = PlayerDataReaderAndWriter.readPlayersFromFile(dataFilePath);
            playerList.setAll(players);
            calculateAvailableIDs(players);
            updateSalesHeader();
         } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.loadRecords", itemTitle));
         } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.parse"));
         }// end of try-catch block
      } else {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.initNeeded"), Messages.get("player.initNeeded"));
      }// end of if-else block
   }// end of loadPlayersFromCSV method

   // Calculate the available IDs for new players
   private void calculateAvailableIDs(List<Player> players) {
      Set<Integer> usedIDs = new HashSet<>();
      for (Player player : players) {
         if (! player.getName().isEmpty() && ! player.getPhoneNumber().isEmpty()) {
            usedIDs.add(player.getId());
         }// end of if block
      }// end of for loop

      int maxID = players.stream().mapToInt(Player::getId).max().orElse(0);
      availableIDs = IntStream.rangeClosed(1, maxID)
                              .boxed()
                              .filter(id -> ! usedIDs.contains(id))
                              .collect(Collectors.toList());

      updateTicketsLeftLabel();
   }// end of calculateAvailableIDs method

   // Update the tickets left label
   private void updateTicketsLeftLabel() {
      ticketsLeft.setText(String.valueOf(availableIDs.size()));
      updateTicketsInCSV();
   }// end of updateTicketsLeftLabel method

   // Check if the file is accessible for writing
   public boolean isFileAccessibleForWriting(Path filePath) {
      try (FileOutputStream ignored = new FileOutputStream(filePath.toFile(), true)) {
         return false;
      } catch (IOException e) {
         return true;
      }// end of try-catch block
   }// end of isFileAccessibleForWriting method

   // Update the number of tickets in the CSV file
   private void updateTicketsInCSV() {
      Path dataFilePath = AppPaths.catalogFile();

      if (isFileAccessibleForWriting(dataFilePath)) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.fileAccess"), Messages.get("err.fileOpen", "data.csv"));
         return;
      }// end of if block

      try {
         List<Item> items = ItemDataReaderAndWriter.readItemsFromFile(dataFilePath);
         Optional<Item> catalogItem = items.stream().filter(item -> item.getTitle().equals(itemTitle)).findFirst();
         if (catalogItem.isPresent() && catalogItem.get().getTickets() != availableIDs.size()) {
            catalogItem.get().setTickets(availableIDs.size());
            ItemDataReaderAndWriter.writeItemsToCSV(items, dataFilePath);
         }// end of if block
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.err.updateTickets"));
      }// end of try-catch block
   }// end of updateTicketsInCSV method

   // Add a new player to the table
   @FXML
   private void handleAddPlayer() throws IOException {
      Path recordsFilePath = AppPaths.recordsFile(itemTitle);

      // Check if the records.csv file is accessible
      if (isFileAccessibleForWriting(recordsFilePath)) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.fileAccess"), Messages.get("err.fileOpen", itemTitle + ".csv"));
         return; // Return early, do not proceed with adding the player
      }

      // Make sure fields are not empty
      if (playerName.getText().isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.nameEmpty"));
         return;
      }

      if (phoneNumber.getText().isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.phoneEmpty"));
         return;
      }

      if (numberOfTickets.getText().isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsEmpty"));
         return;
      }

      // Validate the phone number input field
      if (! PhoneNumbers.isValid(phoneNumber.getText())) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("phone.invalid"));
         return;
      }// end of if block

      // Validate the number of tickets input field
      try {
         int numberOfTicketsValue = Integer.parseInt(numberOfTickets.getText());
         if (numberOfTicketsValue <= 0) {
            showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsPositive"));
            return;
         }
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsDigits"));
         return;
      }// end of try-catch block

      String name = playerName.getText().trim();
      String phone = PhoneNumbers.normalize(phoneNumber.getText());
      int tickets;

      // Validate the input fields
      try {
         tickets = Integer.parseInt(numberOfTickets.getText().trim());
         if (tickets <= 0) {
            showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsPositive"));
            return;
         }// end of if block
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsInvalid"));
         return;
      }// end of try-catch block

      // Check if there are enough tickets available
      if (availableIDs.size() < tickets) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.inputError"), Messages.get("player.notEnough"));
         return;
      }// end of if block

      boolean paidNow = paidCheck.isSelected();
      String soldAt = Instant.now().toString();

      // Assign random IDs to the new tickets
      Random random = new Random();
      List<Integer> availableBeforeSale = new ArrayList<>(availableIDs);
      List<Integer> assignedIDs = new ArrayList<>();

      for (int i = 0; i < tickets; i++) {
         int randomIndex = random.nextInt(availableIDs.size());
         assignedIDs.add(availableIDs.remove(randomIndex));
      }// end of for loop

      // Calculate total tickets including the new ones
      int additionalTickets = tickets;
      List<Player> existingPlayers = playerList.stream()
                                               .filter(player -> player.getName().equalsIgnoreCase(name) && player.getPhoneNumber().equals(phone))
                                               .toList();

      // Add the number of tickets for existing players with the same name and phone number
      if (! existingPlayers.isEmpty()) {
         Player existingPlayer = existingPlayers.getFirst();
         additionalTickets += existingPlayer.getNumberOfTickets();
      }// end of if block

      // Update all existing entries with the same name and phone number
      int finalAdditionalTickets = additionalTickets;
      playerList.stream()
                .filter(player -> player.getName().equalsIgnoreCase(name) && player.getPhoneNumber().equals(phone))
                .forEach(player -> player.setNumberOfTickets(finalAdditionalTickets));

      // Add new entries with the same name and phone number
      assignedIDs.forEach(id -> {
         Optional<Player> existingPlayer = playerList.stream().filter(player -> player.getId() == id).findFirst();
         if (existingPlayer.isPresent()) {
            existingPlayer.get().setName(name);
            existingPlayer.get().setPhoneNumber(phone);
            existingPlayer.get().setNumberOfTickets(finalAdditionalTickets);
            existingPlayer.get().setPaid(paidNow);
            existingPlayer.get().setSoldAt(soldAt);
         } else {
            Player newPlayer = new Player(id, name, phone, finalAdditionalTickets, paidNow, soldAt);
            playerList.add(newPlayer);
         }// end of if-else block
      });// end of forEach loop

      // Write the updated player list to the CSV file
      try {
         PlayerDataReaderAndWriter.writePlayersToCSV(new ArrayList<>(playerList), AppPaths.recordsFile(itemTitle));
      } catch (IOException e) {
         // Nothing was saved: put the tickets back and reload what is really on disk
         availableIDs = availableBeforeSale;
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.saveFailed", e.getMessage()));
         handleRefresh();
         return;
      }// end of try-catch block

      // Format the assigned IDs to display 10 IDs per line in the alert dialog
      String IDs = IntStream.range(0, assignedIDs.size())
                            .mapToObj(i -> (i > 0 && i % 10 == 0) ? "\n" + assignedIDs.get(i) : assignedIDs.get(i).toString())
                            .collect(Collectors.joining(", "));

      // Show a confirmation message
      String total = Money.format(ItemSales.of(List.of(), itemPrice).priceOf(tickets));
      showAlert(Alert.AlertType.CONFIRMATION, Messages.get("player.added.title"),
                Messages.get("player.addedBody", name, IDs, total, Messages.get(paidNow ? "player.sale.paid" : "player.sale.unpaid")));

      updateTicketsLeftLabel();
      clearFields();
      handleRefresh();

   }// end of handleAddPlayer method

   // Remove the selected player from the table
   @FXML
   private void handleRemovePlayer() throws IOException {
      Path recordsFilePath = AppPaths.recordsFile(itemTitle);

      // Check if the records.csv file is accessible
      if (isFileAccessibleForWriting(recordsFilePath)) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.fileAccess"), Messages.get("err.fileOpen", itemTitle + ".csv"));
         return; // Return early, do not proceed with removing the player
      }

      // Get the selected players
      ObservableList<Player> selectedPlayers = playerTable.getSelectionModel().getSelectedItems();
      if (! selectedPlayers.isEmpty()) {

         // Show a confirmation dialog
         Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
         alert.setTitle(Messages.get("player.delete.title"));
         alert.setHeaderText(Messages.get("player.delete.header"));
         alert.setContentText(Messages.get("dialog.chooseOption"));

         ButtonType buttonTypeOne = new ButtonType(Messages.get("dialog.yes"));
         ButtonType buttonTypeTwo = new ButtonType(Messages.get("dialog.no"));

         alert.getButtonTypes().setAll(buttonTypeOne, buttonTypeTwo);

         // Process the user's choice
         Optional<ButtonType> result = alert.showAndWait();
         if (result.isPresent() && result.get() == buttonTypeOne) {

            for (Player selectedPlayer : selectedPlayers) {
               int removedID = selectedPlayer.getId();
               String name = selectedPlayer.getName();
               String phone = selectedPlayer.getPhoneNumber();

               // Find all entries with the same name and phone number
               List<Player> relatedPlayers = playerList.stream()
                                                       .filter(player -> player.getName().equalsIgnoreCase(name) && player.getPhoneNumber().equals(phone))
                                                       .toList();

               // Calculate the total number of tickets remaining after removing the selected player
               int remainingTickets = relatedPlayers.size() - 1;

               // Update the number of tickets for other entries with the same name and phone number
               relatedPlayers.forEach(player -> player.setNumberOfTickets(remainingTickets));

               // Update the selected player's entry
               selectedPlayer.clear();

               availableIDs.add(removedID);
            }// end of for loop

            // Sort the available IDs
            Collections.sort(availableIDs);
            // Write the updated player list to the CSV file
            try {
               PlayerDataReaderAndWriter.writePlayersToCSV(new ArrayList<>(playerList), AppPaths.recordsFile(itemTitle));
            } catch (IOException e) {
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.removeFailed", e.getMessage()));
               handleRefresh();
               return;
            }// end of try-catch block

            showAlert(Alert.AlertType.CONFIRMATION, Messages.get("player.removed.title"), Messages.get("player.removed"));
            handleRefresh();
         }// end of if block
      } else {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("player.noneSelected"));
      }// end of if-else block
   }// end of handleRemovePlayer method

   // Clear all input fields
   @FXML
   private void clearFields() {
      playerName.clear();
      phoneNumber.clear();
      numberOfTickets.clear();
      paidCheck.setSelected(true);
   }// end of clearFields method

   // Record that the selected buyers have paid (or not): all tickets of a buyer are paid together
   @FXML
   private void handleMarkPaid() {
      markSelected(true);
   }// end of handleMarkPaid method

   @FXML
   private void handleMarkUnpaid() {
      markSelected(false);
   }// end of handleMarkUnpaid method

   private void markSelected(boolean paid) {
      Path recordsFilePath = AppPaths.recordsFile(itemTitle);
      if (isFileAccessibleForWriting(recordsFilePath)) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.fileAccess"), Messages.get("err.fileOpen", itemTitle + ".csv"));
         return;
      }// end of if block

      Set<String> buyers = new HashSet<>();
      for (Player selected : playerTable.getSelectionModel().getSelectedItems()) {
         if (selected.isSold()) {
            buyers.add(selected.buyerKey());
         }// end of if block
      }// end of for loop
      if (buyers.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("player.selectBuyer"));
         return;
      }// end of if block

      if (Payments.setPaid(playerList, buyers, paid) > 0) {
         try {
            PlayerDataReaderAndWriter.writePlayersToCSV(new ArrayList<>(playerList), recordsFilePath);
         } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.removeFailed", e.getMessage()));
         }// end of try-catch block
      }// end of if block
      handleRefresh();// shows what is really on disk, and the new totals
   }// end of markSelected method

   // Amount to pay for the number of tickets being typed
   private void updateTotal() {
      try {
         int tickets = Integer.parseInt(numberOfTickets.getText().trim());
         totalLabel.setText(tickets > 0 ? Messages.get("player.label.total", Money.format(ItemSales.of(List.of(), itemPrice).priceOf(tickets))) : "");
      } catch (NumberFormatException e) {
         totalLabel.setText("");
      }// end of try-catch block
   }// end of updateTotal method

   // Item name and price above the list, money collected and still owed at the right
   private void updateSalesHeader() {
      ItemSales sales = ItemSales.of(playerList, itemPrice);
      itemHeader.setText(Messages.get("player.itemHeader", itemTitle, Money.format(itemPrice)));
      salesSummary.setText(Messages.get("player.summary", Money.format(sales.collected()), Money.format(sales.outstanding())));
      updateTotal();
   }// end of updateSalesHeader method

   private static String formatSoldAt(String soldAt) {
      try {
         return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                                 .withLocale(Messages.locale())
                                 .withZone(ZoneId.systemDefault())
                                 .format(Instant.parse(soldAt));
      } catch (DateTimeParseException e) {
         return soldAt;
      }// end of try-catch block
   }// end of formatSoldAt method

   // Refresh the player list
   @FXML
   private void handleRefresh() {
      loadPlayersFromCSV();
      playerTable.refresh();
   }// end of handleRefresh method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

   // Show an alert dialog
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Alert alert = new Alert(alertType);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
   }// end of showAlert method

   // Set the selected item
   public void setItem(Item selectedItem) {
      if (selectedItem != null) {
         this.itemTitle = selectedItem.getTitle();
         this.itemPrice = selectedItem.getPrice();
         loadPlayersFromCSV();
      }// end of if block
   }// end of setItem method

}// end of AddPlayerController class
