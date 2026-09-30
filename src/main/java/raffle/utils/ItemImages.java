package raffle.utils;

import javafx.scene.image.Image;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/** Loads the picture of an item, or the bundled logo when the item has none (or the file is gone). */
public final class ItemImages {

   private ItemImages() {
   }

   public static Image load(String imagePath, double width, double height) {
      try {
         if (imagePath != null && ! imagePath.isBlank()) {
            Path file = Paths.get(imagePath);
            if (Files.isRegularFile(file)) {
               return new Image(file.toUri().toString(), width, height, true, true);
            }// end of if block
         }// end of if block
      } catch (InvalidPathException e) {
         // not a usable path: fall through to the logo
      }// end of try-catch block
      return new Image(Objects.requireNonNull(ItemImages.class.getResourceAsStream("/icons/logo.png")), width, height, true, true);
   }// end of load method

}// end of ItemImages class
