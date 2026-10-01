package raffle.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import raffle.main.App;
import raffle.models.Item;
import raffle.services.ItemOverview;
import raffle.services.ItemSales;
import raffle.services.ItemService;
import raffle.services.SalesSummary;
import raffle.reports.HtmlReports;
import raffle.reports.ReportFiles;
import raffle.reports.ReportService;
import raffle.reports.SalesReport;
import raffle.storage.CsvTransfer;
import raffle.storage.Storage;
import raffle.ui.Documents;
import raffle.ui.Theme;
import raffle.utils.AppPaths;
import raffle.utils.AppSettings;
import raffle.utils.ItemImages;
import raffle.utils.Messages;
import raffle.utils.Money;
import raffle.ui.Dialogs;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
   private TableColumn<Item, String> collectedColumn;
   @FXML
   private Label soldValue;
   @FXML
   private ProgressBar soldBar;
   @FXML
   private Label collectedValue;
   @FXML
   private Label outstandingValue;
   @FXML
   private Label potentialValue;
   @FXML
   private Button addItemButton;
   @FXML
   private Button editItemButton;
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
   private Button reportButton;
   @FXML
   private Button exportButton;
   @FXML
   private Button languageButton;
   @FXML
   private Button themeButton;

   private App app;
   private ObservableList<Item> itemList;
   // Sales figures per item title, read from each item's ticket ledger
   private final Map<String, ItemSales> sales = new HashMap<>();
   private final ItemService itemService = new ItemService(Storage.repository(), AppPaths.root());

   @FXML
   private void initialize() {
      itemTable.setPlaceholder(new Label(Messages.get("main.empty")));

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
                        Image image = ItemImages.load(imagePath, 180, 180);
                        imageView.setImage(image);
                        imageView.setFitWidth(180);
                        imageView.setFitHeight(180);
                        imageView.setPreserveRatio(true);
                        StackPane imagePane = new StackPane(imageView);
                        imagePane.setPrefSize(180, 180);
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
               setStyle("-fx-alignment: CENTER; -fx-font-size: 26px; -fx-font-weight: bold;");
            }// end of if-else block
         }// end of updateItem method
      });// end of titleColumn.setCellFactory method

      ticketsColumn.setCellValueFactory(cellData -> cellData.getValue().ticketsProperty().asObject());
      // How much of the item is sold: "8 / 20 (40%)" above a progress bar
      ticketsColumn.setCellFactory(tc -> new TableCell<>() {
         private final ProgressBar bar = new ProgressBar(0);
         private final Label text = new Label();
         private final VBox box = new VBox(6, text, bar);

         {
            box.setAlignment(Pos.CENTER);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(14);
            text.getStyleClass().add("sold-text");
         }

         @Override
         protected void updateItem(Integer ignored, boolean empty) {
            super.updateItem(ignored, empty);
            Item row = getTableRow() == null ? null : getTableRow().getItem();
            if (empty || row == null) {
               setGraphic(null);
               return;
            }// end of if block
            ItemSales figures = salesOf(row);
            text.setText(Messages.get("main.soldPercent", String.valueOf(figures.soldTickets()), String.valueOf(figures.totalTickets()),
                                      String.valueOf(Math.round(figures.soldFraction() * 100))));
            bar.setProgress(figures.soldFraction());
            setGraphic(box);
         }// end of updateItem method
      });// end of ticketsColumn.setCellFactory method

      // Money collected for the item, with what is still owed underneath
      collectedColumn.setCellFactory(tc -> new TableCell<>() {
         private final Label money = new Label();
         private final Label owed = new Label();
         private final VBox box = new VBox(2, money, owed);

         {
            box.setAlignment(Pos.CENTER);
            money.getStyleClass().add("cell-money");
            owed.getStyleClass().add("cell-owed");
         }

         @Override
         protected void updateItem(String ignored, boolean empty) {
            super.updateItem(ignored, empty);
            Item row = getTableRow() == null ? null : getTableRow().getItem();
            if (empty || row == null) {
               setGraphic(null);
               return;
            }// end of if block
            ItemSales figures = salesOf(row);
            money.setText(Money.format(figures.collected()));
            boolean someOwed = figures.outstandingCents() > 0;
            owed.setText(someOwed ? Messages.get("main.owed", Money.format(figures.outstanding())) : "");
            owed.setVisible(someOwed);
            owed.setManaged(someOwed);
            setGraphic(box);
         }// end of updateItem method
      });// end of collectedColumn.setCellFactory method

      priceColumn.setCellValueFactory(cellData -> cellData.getValue().priceProperty().asObject());
      priceColumn.setCellFactory(tc -> new TableCell<>() {
         @Override
         protected void updateItem(Double item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
               setText(null);
            } else {
               setText(Money.format(item));
               setStyle("-fx-alignment: CENTER; -fx-font-size: 26px; -fx-font-weight: bold;");
            }//end of if-else block
         }//end of updateItem method
      });// end of priceColumn.setCellFactory method

      // Add tooltips to the buttons
      addTooltip(addItemButton, Messages.get("main.tip.add"));
      addTooltip(editItemButton, Messages.get("main.tip.edit"));
      addTooltip(deleteItemButton, Messages.get("main.tip.delete"));
      addTooltip(drawButton, Messages.get("main.tip.draw"));
      addTooltip(refreshListButton, Messages.get("main.tip.refresh"));
      addTooltip(buyTicketsButton, Messages.get("main.tip.buy"));
      addTooltip(viewItemButton, Messages.get("main.tip.view"));
      addTooltip(reportButton, Messages.get("main.tip.report"));
      addTooltip(exportButton, Messages.get("main.tip.export"));
      addTooltip(languageButton, Messages.get("main.tip.language"));
      languageButton.setText(Messages.other().getLanguage().toUpperCase(Locale.ROOT));// shows the language it switches to
      addTooltip(themeButton, Messages.get("main.tip.theme"));
      themeButton.setText(Messages.get(Theme.mode() == Theme.Mode.DARK ? "main.btn.themeLight" : "main.btn.themeDark"));// shows the theme it switches to

      loadItems();

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
      imageColumn.setPrefWidth(tableWidth * 0.20);
      titleColumn.setPrefWidth(tableWidth * 0.24);
      ticketsColumn.setPrefWidth(tableWidth * 0.22);
      priceColumn.setPrefWidth(tableWidth * 0.14);
      collectedColumn.setPrefWidth(tableWidth * 0.16);
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

   // Load the items and each item's sales figures from the repository
   private void loadItems() {
      try {
         List<ItemOverview> overview = itemService.overview();
         itemList = FXCollections.observableArrayList(overview.stream().map(ItemOverview::item).toList());
         sales.clear();
         overview.forEach(entry -> sales.put(entry.item().getTitle(), entry.sales()));
         updateDashboard();
         itemTable.setItems(itemList);
      } catch (IOException e) {
         if (itemList == null) {
            itemList = FXCollections.observableArrayList();
            itemTable.setItems(itemList);
         }// end of if block
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.loadCatalog"));
      }// end of try-catch block
   }// end of loadItems method

   private ItemSales salesOf(Item item) {
      return sales.getOrDefault(item.getTitle(), ItemSales.of(List.of(), item.getPrice()));
   }// end of salesOf method

   // The cards above the table: the whole raffle in four numbers
   private void updateDashboard() {
      SalesSummary summary = SalesSummary.of(sales.values());
      soldValue.setText(Messages.get("main.soldPercent", String.valueOf(summary.soldTickets()), String.valueOf(summary.totalTickets()),
                                     String.valueOf(Math.round(summary.soldFraction() * 100))));
      soldBar.setProgress(summary.soldFraction());
      collectedValue.setText(Money.format(summary.collected()));
      outstandingValue.setText(Money.format(summary.outstanding()));
      potentialValue.setText(Money.format(summary.potential()));
   }// end of updateDashboard method

   // Handle the Add Item button
   @FXML
   private void handleAddItem() {
      app.showAddItemView();
   }// end of handleAddItem method

   // Handle the Edit Item button
   @FXML
   private void handleEditItem() {
      if (itemList == null || itemList.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.initNeeded"), Messages.get("main.initNeeded"));
         return;
      }//end of if block

      Item selectedItem = itemTable.getSelectionModel().getSelectedItem();
      if (selectedItem == null) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("main.selectToEdit"));
         return;
      }//end of if block
      app.showEditItemView(selectedItem);
   }//end of handleEditItem method

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
               itemService.delete(selectedItem.getTitle());
               itemList.remove(selectedItem);
               sales.remove(selectedItem.getTitle());
               updateDashboard();
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

   // The sales report as a page the browser can print or save as PDF
   @FXML
   private void handleReport() {
      try {
         SalesReport report = new ReportService(Storage.repository()).salesReport(Instant.now());
         String organizer = AppSettings.get("organizer").orElse("");
         Documents.open(ReportFiles.write(AppPaths.reportsDir(), "sales-report", HtmlReports.salesReport(report, organizer)));
      } catch (IOException e) {
         Dialogs.show(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("report.err", String.valueOf(e.getMessage())));
      }// end of try-catch block
   }//end of handleReport method

   // Write all the data as CSV files (a spreadsheet can open them) into a new folder
   @FXML
   private void handleExport() {
      try {
         Path folder = CsvTransfer.exportTo(Storage.repository(), AppPaths.exportsDir());
         Dialogs.show(Alert.AlertType.INFORMATION, Messages.get("main.btn.export"), Messages.get("storage.exported", folder.toString()));
      } catch (IOException e) {
         Dialogs.show(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("storage.exportFailed", String.valueOf(e.getMessage())));
      }// end of try-catch block
   }//end of handleExport method

   @FXML
   private void handleRefreshList() {
      loadItems();
   }//end of handleRefreshList method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }//end of addTooltip method

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
