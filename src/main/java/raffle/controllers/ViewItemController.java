package raffle.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import raffle.models.Item;
import raffle.utils.AppPaths;
import raffle.utils.Messages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ViewItemController {

   @FXML
   private Label itemTitleLabel;
   @FXML
   private Label itemDescriptionLabel;

   @FXML
   private ImageView itemImage;

   @FXML
   private Button prevButton;

   @FXML
   private Button nextButton;

   private List<File> imageFiles;
   private int currentIndex;

   @FXML
   public void initialize() {
      prevButton.setOnAction(event -> handlePreviousImage());
      nextButton.setOnAction(event -> handleNextImage());

      addTooltip(prevButton, Messages.get("viewitem.tip.prev"));
      addTooltip(nextButton, Messages.get("viewitem.tip.next"));
      addTooltip(itemTitleLabel, Messages.get("viewitem.tip.title"));
      addTooltip(itemDescriptionLabel, Messages.get("viewitem.tip.description"));
   }// end of initialize method

   // Method to set the item
   public void setItem(Item item) {

      itemTitleLabel.setText(item.getTitle());
      itemDescriptionLabel.setText(item.getDescription());
      loadImages(item.getTitle());
      if (! imageFiles.isEmpty()) {
         currentIndex = 0;
         showImage(imageFiles.get(currentIndex));
      }// end of if statement
   }// end of setItem method

   // Method to load images from the directory
   private void loadImages(String title) {

      imageFiles = new ArrayList<>();
      File directory = AppPaths.itemDir(title).toFile();
      if (directory.exists() && directory.isDirectory()) {
         File[] files = directory.listFiles((dir, name) -> {
            String lowerCaseName = name.toLowerCase();
            return lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg") ||
                    lowerCaseName.endsWith(".png") || lowerCaseName.endsWith(".gif") ||
                    lowerCaseName.endsWith(".bmp") || lowerCaseName.endsWith(".tiff") ||
                    lowerCaseName.endsWith(".webp");
         });// end of listFiles method

         if (files != null) {
            imageFiles.addAll(Arrays.asList(files));
         }// end of if statement
      }// end of if statement
   }// end of loadImages method

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
         showImage(imageFiles.get(currentIndex));
      }// end of if statement
   }// end of handlePreviousImage method

   @FXML
   private void handleNextImage() {
      if (currentIndex < imageFiles.size() - 1) {
         currentIndex++;
         showImage(imageFiles.get(currentIndex));
      }// end of if statement
   }// end of handleNextImage method

   private void showAlert(Alert.AlertType alertType, String title, String message) {
      Alert alert = new Alert(alertType);
      alert.setTitle(title);
      alert.setHeaderText(null);
      alert.setContentText(message);
      alert.showAndWait();
   }// end of showAlert method

   // Add a tooltip to a control
   private void addTooltip(Control control, String text) {
      Tooltip tooltip = new Tooltip(text);
      control.setTooltip(tooltip);
   }//end of addTooltip method

}// end of ViewItemController class



