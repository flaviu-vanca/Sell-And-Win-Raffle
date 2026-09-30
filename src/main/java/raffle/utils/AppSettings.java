package raffle.utils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/** Small key/value settings file kept next to the data (for now only the chosen language). */
public final class AppSettings {

   private AppSettings() {
   }

   public static Optional<String> get(String key) {
      return get(AppPaths.settingsFile(), key);
   }// end of get method

   public static void put(String key, String value) throws IOException {
      put(AppPaths.settingsFile(), key, value);
   }// end of put method

   static Optional<String> get(Path file, String key) {
      return Optional.ofNullable(load(file).getProperty(key));
   }// end of get method

   static void put(Path file, String key, String value) throws IOException {
      Properties properties = load(file);
      properties.setProperty(key, value);

      StringWriter text = new StringWriter();
      properties.store(text, null);
      List<String> lines = Arrays.stream(text.toString().split("\\R"))
                                 .filter(line -> ! line.isBlank() && ! line.startsWith("#"))// drop the timestamp comment
                                 .sorted()
                                 .toList();
      SafeFiles.writeLinesAtomically(file, lines);
   }// end of put method

   // A missing or unreadable settings file just means "defaults"
   private static Properties load(Path file) {
      Properties properties = new Properties();
      if (Files.isRegularFile(file)) {
         try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            properties.load(reader);
         } catch (IOException | IllegalArgumentException e) {
            System.err.println("Could not read settings: " + e.getMessage());
         }// end of try-catch block
      }// end of if block
      return properties;
   }// end of load method

}// end of AppSettings class
