package raffle.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Callback;
import raffle.main.App;
import raffle.models.Item;
import raffle.ui.Theme;
import raffle.utils.AppPaths;
import raffle.utils.BackupService;
import raffle.utils.ItemDataReaderAndWriter;
import raffle.utils.ItemImages;
import raffle.utils.Messages;
import raffle.utils.Money;
import raffle.ui.Dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class MainViewController {

   @FXML
   private TableView<Item> itemTable;
   @FXML
   private TableColumn<Item, String> imageColumn;
   @FXML
   private TableColumn<Item, String> titleColumn;
   @FXML
   private TableColumn<Item, Integer> ticketsColumn;
   @FXML
   private TableColumn<Item, Double> priceColumn;
   @FXML
   private Button addItemButton;
   @FXML
   private Button deleteItemButton;
   @FXML
   private Button drawButton;
   @FXML
   private Button buyTicketsButton;
   @FXML
   private Button viewItemButton;
   @FXML
   private Button refreshListButton;
   @FXML
   private Button languageButton;
   @FXML
   private Button themeButton;

   private App app;
   private ObservableList<Item> itemList;

   @FXML
   private void initialize() {
      imageColumn.setCellValueFactory(cellData -> cellData.getValue().imageProperty());
      imageColumn.setCellFactory(new Callback<>() {
         @Override
         public TableCell<Item, String> call(TableColumn<Item, String> param) {
            return new TableCell<>() {
               private final ImageView imageView = new ImageView();

               @Override
               protected void updateItem(String imagePath, boolean empty) {
                  super.updateItem(imagePath, empty);
                  if (empty) {
                     setGraphic(null);
                  } else {
                     try {
                        Image image = ItemImages.load(imagePath, 250, 250);
                        imageView.setImage(image);
                        imageView.setFitWidth(250);
                        imageView.setFitHeight(250);
                        imageView.setPreserveRatio(true);
                        StackPane imagePane = new StackPane(imageView);
                        imagePane.setPrefSize(250, 250);
                        setGraphic(imagePane);
                     } catch (Exception e) {
                        setGraphic(null);
                        System.err.println("Invalid image path: " + imagePath);
                     }// end of try-catch block
                  }// end of if-else block
               }// end of updateItem method
            };// end of TableCell method
         }// end of call method
      });// end of imageColumn.setCellFactory method

      titleColumn.setCellValueFactory(cellData -> cellData.getValue().titleProperty());
      titleColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(item);
               setStyle("-fx-alignment: CENTER; -fx-font-size: 30px; -fx-font-weight: bold;");
            }// end of if-else block
         }// end of updateItem method
      });// end of titleColumn.setCellFactory method

      ticketsColumn.setCellValueFactory(cellData -> cellData.getValue().ticketsProperty().asObject());
      ticketsColumn.setCellFactory(tc -> {
         return new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
               super.updateItem(item, empty);
               if (empty) {
                  setText(null);
               } else {
                  setText(item.toString());
                  setStyle("-fx-alignment: CENTER; -fx-font-size: 30px; -fx-font-weight: bold;");
               }// end of if-else block
            }// end of updateItem method
         };// end of updateItem method
      });// end of ticketsColumn.setCellFactory method

      priceColumn.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
      priceColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(Money.format(item));
               setStyle("-fx-alignment: CENTER; -fx-font-size: 30px; -fx-font-weight: bold;");
            }//end of if-else block
         }//end of updateItem method
      });// end of priceColumn.setCellFactory method

      // Add tooltips to the buttons
      addTooltip(addItemButton, Messages.get("main.tip.add"));
      addTooltip(deleteItemButton, Messages.get("main.tip.delete"));
      addTooltip(drawButton, Messages.get("main.tip.draw"));
      addTooltip(refreshListButton, Messages.get("main.tip.refresh"));
      addTooltip(buyTicketsButton, Messages.get("main.tip.buy"));
      addTooltip(viewItemButton, Messages.get("main.tip.view"));
      addTooltip(languageButton, Messages.get("main.tip.language"));
      languageButton.setText(Messages.other().getLanguage().toUpperCase(Locale.ROOT));// shows the language it switches to
      addTooltip(themeButton, Messages.get("main.tip.theme"));
      themeButton.setText(Messages.get(Theme.mode() == Theme.Mode.DARK ? "main.btn.themeLight" : "main.btn.themeDark"));// shows the theme it switches to

      loadItemsFromCSV();

      // Bind column widths to the table's width (also applied once right away, not only after a resize)
      itemTable.widthProperty().addListener((obs, oldWidth, newWidth) -> applyColumnWidths(newWidth.doubleValue()));

      Platform.runLater(() -> {
         Stage stage = (Stage) itemTable.getScene().getWindow();

         applyColumnWidths(itemTable.getWidth());

         // Ensure the table resizes with the window
         stage.widthProperty().addListener((obs, oldVal, newVal) -> itemTable.setPrefWidth(newVal.doubleValue()));
         stage.heightProperty().addListener((obs, oldVal, newVal) -> itemTable.setPrefHeight(newVal.doubleValue()));
      });
   }// end of initialize method

   // Columns share the table width; 4% is left for the vertical scrollbar so no horizontal scrollbar appears
   private void applyColumnWidths(double tableWidth) {
      if (tableWidth <= 0) {
         return;
      }//end of if block
      imageColumn.setPrefWidth(tableWidth * 0.29);
      titleColumn.setPrefWidth(tableWidth * 0.33);
      ticketsColumn.setPrefWidth(tableWidth * 0.17);
      priceColumn.setPrefWidth(tableWidth * 0.17);
   }//end of applyColumnWidths method

   @FXML
   private void handleBuyTickets() {
      // Check if the list is null or empty for initialization
      if (itemList == null || itemList.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
         return;
      }//end of if block

      // Check if there is a selected item
      Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
      if (selectedItem == null) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("main.selectToBuy"));
         return;
      }

      try {
         app.showAddPlayerView(selectedItem);
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("main.err.openBuy"));
      }//end of try-catch block
   }// end of handleBuyTickets method

   // Load items from the data.csv file
   private void loadItemsFromCSV() {
      Path dataFilePath = AppPaths.catalogFile();

      if (Files.exists(dataFilePath)) {
         try {
            List<Item> items = ItemDataReaderAndWriter.readItemsFromFile(dataFilePath);
            itemList = FXCollections.observableArrayList(items);
            itemTable.setItems(itemList);
         } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.loadCatalog"));
         } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.parse"));
         }
      } else {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
      }
   }// end of loadItemsFromCSV method

   // Handle the Add Item button
   @FXML
   private void handleAddItem() {
      app.showAddItemView();
   }// end of handleAddItem method

   // Handle the Delete Item button
   @FXML
   private void handleDeleteItem() {
      // Check if the list is null or empty for initialization
      if (itemList == null || itemList.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
         return;
      }//end of if block

      Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
      if (selectedItem != null) {
         if (Dialogs.confirm(Messages.get("main.delete.title"), Messages.get("main.delete.header"))) {
            try {
               ItemDataReaderAndWriter.writeItemsToCSV(itemList.filtered(item -> item != selectedItem), AppPaths.catalogFile());
               itemList.remove(selectedItem);
               archiveRecords(selectedItem.getTitle());
               deleteDirectory(selectedItem.getTitle());
               showAlert(Alert.AlertType.CONFIRMATION, Messages.get("main.deleted.title"), Messages.get("main.deleted") + "\n\n" + Messages.get("item.recordsArchived"));
            } catch (IOException e) {
               showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.updateCatalog"));
            }// end of try-catch block
         }//end of if block
      } else {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("main.noneSelectedDelete"));
      }//end of if-else block
   }//end of handleDeleteItem method

   // Handle the Draw button
   @FXML
   private void handleDraw() {
      // Check if the list is null or empty for initialization
      if (itemList == null || itemList.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
         return;
      }

      // Get the selected item
      Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
      if (selectedItem == null) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("main.selectToDraw"));
         return;
      }

      // Show the draw view for the selected item
      try {
         app.showDrawView(selectedItem);
      } catch (Exception e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("main.err.openDraw"));
      }
   }//end of handleDraw method

   // Switch between English and Romanian. Every screen is built from the bundle when it is opened,
   // so rebuilding the main view is enough to apply the new language.
   @FXML
   private void handleToggleLanguage() {
      Messages.switchLanguage();
      app.showMainView();
   }//end of handleToggleLanguage method

   // Switch between the dark and the light theme; the main view is rebuilt, every other screen is built
   // with the new theme when it is opened.
   @FXML
   private void handleToggleTheme() {
      Theme.toggle();
      app.showMainView();
   }//end of handleToggleTheme method

   @FXML
   private void handleRefreshList() {
      loadItemsFromCSV();
   }//end of handleRefreshList method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }//end of addTooltip method

   // Delete the image directory with the given title
   private void deleteDirectory(String title) {
      File directory = AppPaths.itemDir(title).toFile();
      File[] files = directory.listFiles();
      if (files != null) {
         for (File file : files) {
            file.delete();
         }
         directory.delete();
      }//end of if block
   }//end of deleteDirectory method

   // The ticket ledger holds buyer data and money owed, so it is moved to backups instead of being destroyed.
   // It also frees the title, which previously could not be reused until the ledger was deleted by hand.
   private void archiveRecords(String title) {
      Path records = AppPaths.recordsFile(title);
      if (Files.exists(records)) {
         try {
            BackupService.archive(AppPaths.root(), records);
         } catch (IOException e) {
            System.err.println("Could not archive " + records + ": " + e.getMessage());
         }//end of try-catch block
      }//end of if block
   }//end of archiveRecords method

   @FXML
   private void handleViewItem(){
      // Check if the list is null or empty for initialization
      if (itemList == null || itemList.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
         return;
      }//end of if block

      Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
      if (selectedItem != null) {
         app.showViewItemView(selectedItem);
      } else {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("main.noneSelected"));
      }
   }// end of handleViewItem method

   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Dialogs.showLater(alertType, title, message);
   }// end of showAlert method

   public void setMainApp(App app) {
      this.app = app;
   }// end of setMainApp method

}// end of MainViewController class
