package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import raffle.models.Item;
import raffle.services.ItemService;
import raffle.storage.Storage;
import raffle.ui.Dialogs;
import raffle.utils.AppPaths;
import raffle.utils.ImageFiles;
import raffle.utils.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ViewItemController {

   @FXML
   private Label itemTitleLabel;
   @FXML
   private Label itemDescriptionLabel;
   @FXML
   private Label imageCounter;

   @FXML
   private ImageView itemImage;

   @FXML
   private Button prevButton;

   @FXML
   private Button nextButton;
   @FXML
   private Button addPicturesButton;
   @FXML
   private Button mainPictureButton;

   private final ItemService items = new ItemService(Storage.repository(), AppPaths.root());
   private Item item;
   private List<Path> imageFiles = new ArrayList<>();
   private int currentIndex;

   @FXML
   public void initialize() {
      addTooltip(prevButton, Messages.get("viewitem.tip.prev"));
      addTooltip(nextButton, Messages.get("viewitem.tip.next"));
      addTooltip(itemTitleLabel, Messages.get("viewitem.tip.title"));
      addTooltip(itemDescriptionLabel, Messages.get("viewitem.tip.description"));
      addTooltip(addPicturesButton, Messages.get("viewitem.tip.add"));
      addTooltip(mainPictureButton, Messages.get("viewitem.tip.main"));
   }// end of initialize method

   // Method to set the item
   public void setItem(Item item) {
      this.item = item;
      itemTitleLabel.setText(item.getTitle());
      itemDescriptionLabel.setText(item.getDescription());
      loadImages();
      currentIndex = 0;
      showCurrent();
   }// end of setItem method

   // Load the pictures from the item's folder
   private void loadImages() {
      try {
         imageFiles = new ArrayList<>(ImageFiles.list(AppPaths.itemDir(item.getTitle())));
      } catch (IOException e) {
         imageFiles = new ArrayList<>();
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("viewitem.err.image"));
      }// end of try-catch block
   }// end of loadImages method

   // Show the current picture and where it is in the list
   private void showCurrent() {
      boolean any = ! imageFiles.isEmpty();
      if (any) {
         showImage(imageFiles.get(currentIndex).toFile());
      }// end of if block
      imageCounter.setText(any ? Messages.get("viewitem.counter", String.valueOf(currentIndex + 1), String.valueOf(imageFiles.size()))
                               : Messages.get("viewitem.noPictures"));
      prevButton.setDisable(currentIndex <= 0);
      nextButton.setDisable(currentIndex >= imageFiles.size() - 1);
      mainPictureButton.setDisable(! any);
   }// end of showCurrent method

   private void showImage(File file) {
      try (FileInputStream fis = new FileInputStream(file)) {
         Image image = new Image(fis);
         itemImage.setImage(image);
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("viewitem.err.image"));
      }// end of try-catch block
   }// end of showImage method

   @FXML
   private void handlePreviousImage() {
      if (currentIndex > 0) {
         currentIndex--;
         showCurrent();
      }// end of if statement
   }// end of handlePreviousImage method

   @FXML
   private void handleNextImage() {
      if (currentIndex < imageFiles.size() - 1) {
         currentIndex++;
         showCurrent();
      }// end of if statement
   }// end of handleNextImage method

   // Copy pictures from anywhere on the computer into the item's folder and show the first new one
   @FXML
   private void handleAddImages() {
      FileChooser fileChooser = new FileChooser();
      fileChooser.setTitle(Messages.get("viewitem.chooser.title"));
      fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(Messages.get("additem.chooser.filter"), ImageFiles.chooserPatterns()));
      File pictures = new File(System.getProperty("user.home"), "Pictures");
      File start = pictures.isDirectory() ? pictures : new File(System.getProperty("user.home"));
      if (start.isDirectory()) {
         fileChooser.setInitialDirectory(start);
      }// end of if block

      List<File> selected = fileChooser.showOpenMultipleDialog(addPicturesButton.getScene().getWindow());
      if (selected == null || selected.isEmpty()) {
         return;
      }// end of if block

      try {
         Path firstNew = null;
         for (File file : selected) {
            Path copy = ImageFiles.copyInto(AppPaths.itemDir(item.getTitle()), file.toPath());
            if (firstNew == null) {
               firstNew = copy;
            }// end of if block
         }// end of for loop
         loadImages();
         currentIndex = Math.max(0, imageFiles.indexOf(firstNew));
         showCurrent();
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("viewitem.err.image"));
      }// end of try-catch block
   }// end of handleAddImages method

   // The picture on display becomes the item's picture on the main screen
   @FXML
   private void handleSetMainImage() {
      if (imageFiles.isEmpty()) {
         showAlert(Alert.AlertType.INFORMATION, Messages.get("alert.title.noSelection"), Messages.get("viewitem.noPicture"));
         return;
      }// end of if block

      try {
         Path picture = imageFiles.get(currentIndex);
         items.setMainPicture(item.getTitle(), picture);
         item.setImage(picture.toAbsolutePath().toString());
         showAlert(Alert.AlertType.INFORMATION, Messages.get("viewitem.btn.main"), Messages.get("viewitem.mainSet"));
      } catch (IOException e) {
         showAlert(Alert.AlertType.ERROR, Messages.get("alert.title.error"), Messages.get("err.updateCatalog"));
      }// end of try-catch block
   }// end of handleSetMainImage method

   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Dialogs.show(alertType, title, message);
   }// end of showAlert method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }//end of addTooltip method

}// end of ViewItemController class
