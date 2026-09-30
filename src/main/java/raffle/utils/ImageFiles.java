package raffle.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/** Picture files of an item: which files count as pictures, listing them, and copying new ones in. */
public final class ImageFiles {

   private static final List<String> EXTENSIONS = List.of("png", "jpg", "jpeg", "gif", "bmp", "tiff", "webp");

   private ImageFiles() {
   }

   /** Patterns for a file chooser, for example {@code *.png}. */
   public static String[] chooserPatterns() {
      return EXTENSIONS.stream().map(extension -> "*." + extension).toArray(String[]::new);
   }// end of chooserPatterns method

   public static boolean isImage(String fileName) {
      String lower = fileName.toLowerCase(Locale.ROOT);
      return EXTENSIONS.stream().anyMatch(extension -> lower.endsWith("." + extension));
   }// end of isImage method

   /** The pictures in a folder, by name. A folder that does not exist has none. */
   public static List<Path> list(Path directory) throws IOException {
      if (! Files.isDirectory(directory)) {
         return List.of();
      }// end of if block
      try (Stream<Path> files = Files.list(directory)) {
         return files.filter(Files::isRegularFile)
                     .filter(file -> isImage(file.getFileName().toString()))
                     .sorted(Comparator.comparing(file -> file.getFileName().toString().toLowerCase(Locale.ROOT)))
                     .toList();
      }// end of try-with-resources
   }// end of list method

   /**
    * Copies a picture into the folder, creating the folder if needed. An existing file is never overwritten:
    * {@code bike.png} becomes {@code bike (2).png}. A file that is already in the folder is left where it is.
    *
    * @return the path of the copy
    */
   public static Path copyInto(Path directory, Path source) throws IOException {
      Files.createDirectories(directory);
      Path absoluteDirectory = directory.toAbsolutePath().normalize();
      if (source.toAbsolutePath().normalize().getParent().equals(absoluteDirectory)) {
         return source.toAbsolutePath().normalize();
      }// end of if block

      String name = source.getFileName().toString();
      int dot = name.lastIndexOf('.');
      String base = dot > 0 ? name.substring(0, dot) : name;
      String extension = dot > 0 ? name.substring(dot) : "";

      Path target = absoluteDirectory.resolve(name);
      for (int copy = 2; Files.exists(target); copy++) {
         target = absoluteDirectory.resolve(base + " (" + copy + ")" + extension);
      }// end of for loop
      return Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
   }// end of copyInto method

}// end of ImageFiles class
