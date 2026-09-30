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
import raffle.services.SalesService;
import raffle.services.TicketSales;
import raffle.services.ValidationException;
import raffle.storage.Storage;
import raffle.utils.Messages;
import raffle.utils.Money;
import raffle.ui.Dialogs;

import java.io.IOException;
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

   private final SalesService sales = new SalesService(Storage.repository());
   private ObservableList<Player> playerList;
   private String itemTitle;
   private double itemPrice;

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

   // Load the tickets of the item from the repository
   private void loadTickets() {
      try {
         List<Player> ledger = sales.ledger(itemTitle);
         if (ledger.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.initNeeded"), Messages.get("player.initNeeded"));
            return;
         }// end of if block
         playerList.setAll(ledger);
         ticketsLeft.setText(String.valueOf(TicketSales.availableIds(ledger).size()));
         updateSalesHeader();
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.loadRecords", itemTitle));
      }// end of try-catch block
   }// end of loadTickets method

   // Sell tickets to the buyer typed in
   @FXML
   private void handleAddPlayer() {
      String name = playerName.getText().trim();
      String phone = phoneNumber.getText();
      String ticketsText = numberOfTickets.getText().trim();

      // What was typed: is everything filled in, and is the number of tickets a number?
      if (name.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.nameEmpty"));
         return;
      }
      if (phone.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.phoneEmpty"));
         return;
      }
      if (ticketsText.isEmpty()) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsEmpty"));
         return;
      }

      int tickets;
      try {
         tickets = Integer.parseInt(ticketsText);
      } catch (NumberFormatException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get("val.ticketsDigits"));
         return;
      }// end of try-catch block

      // The rules (valid phone, tickets available) and the saving are the sales service's job
      boolean paidNow = paidCheck.isSelected();
      List<Integer> assignedIDs;
      try {
         assignedIDs = sales.sell(itemTitle, name, phone, tickets, paidNow);
      } catch (ValidationException e) {
         showAlert(Alert.AlertType.WARNING, Messages.get("alert.title.inputError"), Messages.get(e.messageKey(), e.arguments()));
         return;
      } catch (IOException e) {
         // Nothing was saved: show what is really stored
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

      clearFields();
      handleRefresh();

   }// end of handleAddPlayer method

   // Take the selected buyers' tickets back
   @FXML
   private void handleRemovePlayer() {
      ObservableList<Player> selectedPlayers = playerTable.getSelectionModel().getSelectedItems();
      if (selectedPlayers.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("player.noneSelected"));
         return;
      }// end of if block

      // Rows of tickets nobody has bought have nothing to remove
      List<Integer> soldIds = selectedPlayers.stream().filter(Player::isSold).map(Player::getId).toList();
      if (soldIds.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("player.selectBuyer"));
         return;
      }// end of if block

      // Ask before deleting
      if (Dialogs.confirm(Messages.get("player.delete.title"), Messages.get("player.delete.header"))) {
         try {
            sales.release(itemTitle, soldIds);
         } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.removeFailed", e.getMessage()));
            handleRefresh();
            return;
         }// end of try-catch block

         showAlert(Alert.AlertType.CONFIRMATION, Messages.get("player.removed.title"), Messages.get("player.removed"));
         handleRefresh();
      }// end of if block
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

      try {
         sales.setPaid(itemTitle, buyers, paid);
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("player.saveFailed", e.getMessage()));
      }// end of try-catch block
      handleRefresh();// shows what is really stored, and the new totals
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
      loadTickets();
      playerTable.refresh();
   }// end of handleRefresh method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }// end of addTooltip method

   // Show an alert dialog
   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Dialogs.show(alertType, title, message);
   }// end of showAlert method

   // Set the selected item
   public void setItem(Item selectedItem) {
      if (selectedItem != null) {
         this.itemTitle = selectedItem.getTitle();
         this.itemPrice = selectedItem.getPrice();
         loadTickets();
      }// end of if block
   }// end of setItem method

}// end of AddPlayerController class
