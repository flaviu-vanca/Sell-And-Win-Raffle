package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageFilesTest {

   @TempDir
   Path dir;

   @Test
   void recognisesPicturesByExtensionIgnoringCase() {
      assertTrue(ImageFiles.isImage("bike.PNG"));
      assertTrue(ImageFiles.isImage("holiday.JpEg"));
      assertFalse(ImageFiles.isImage("notes.txt"));
      assertFalse(ImageFiles.isImage("png"));
   }

   @Test
   void copiesIntoAFolderThatDoesNotExistYet() throws IOException {
      Path source = Files.writeString(dir.resolve("bike.png"), "pixels");
      Path itemDir = dir.resolve("items").resolve("Bike");

      Path copy = ImageFiles.copyInto(itemDir, source);

      assertEquals(itemDir.toAbsolutePath().resolve("bike.png"), copy);
      assertEquals("pixels", Files.readString(copy));
      assertTrue(Files.exists(source), "the original stays where it was");
   }

   @Test
   void neverOverwritesAnExistingPicture() throws IOException {
      Path source = Files.writeString(dir.resolve("bike.png"), "new");
      Path itemDir = Files.createDirectories(dir.resolve("Bike"));
      Files.writeString(itemDir.resolve("bike.png"), "old");

      Path first = ImageFiles.copyInto(itemDir, source);
      Path second = ImageFiles.copyInto(itemDir, source);

      assertEquals("bike (2).png", first.getFileName().toString());
      assertEquals("bike (3).png", second.getFileName().toString());
      assertEquals("old", Files.readString(itemDir.resolve("bike.png")));
   }

   @Test
   void aPictureAlreadyInTheFolderIsNotCopiedOntoItself() throws IOException {
      Path itemDir = Files.createDirectories(dir.resolve("Bike"));
      Path inside = Files.writeString(itemDir.resolve("front.jpg"), "x");

      assertEquals(inside.toAbsolutePath().normalize(), ImageFiles.copyInto(itemDir, inside));
      assertEquals(1, ImageFiles.list(itemDir).size());
   }

   @Test
   void listsOnlyPicturesByName() throws IOException {
      Path itemDir = Files.createDirectories(dir.resolve("Bike"));
      Files.writeString(itemDir.resolve("b.png"), "x");
      Files.writeString(itemDir.resolve("A.jpg"), "x");
      Files.writeString(itemDir.resolve("readme.txt"), "x");

      List<String> names = ImageFiles.list(itemDir).stream().map(path -> path.getFileName().toString()).toList();

      assertEquals(List.of("A.jpg", "b.png"), names);
      assertTrue(ImageFiles.list(dir.resolve("missing")).isEmpty());
   }
}
