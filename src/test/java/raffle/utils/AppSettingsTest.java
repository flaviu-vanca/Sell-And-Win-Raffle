package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppSettingsTest {

   @TempDir
   Path dir;

   @Test
   void missingFileMeansNoValue() {
      assertEquals(Optional.empty(), AppSettings.get(dir.resolve("settings.properties"), "language"));
   }

   @Test
   void valuesSurviveAWriteAndRead() throws IOException {
      Path file = dir.resolve("settings.properties");
      AppSettings.put(file, "language", "ro");

      assertEquals(Optional.of("ro"), AppSettings.get(file, "language"));
   }

   @Test
   void settingOneKeyKeepsTheOthers() throws IOException {
      Path file = dir.resolve("settings.properties");
      AppSettings.put(file, "language", "ro");
      AppSettings.put(file, "folder", "D:\\Raffle Data");
      AppSettings.put(file, "language", "en");

      assertEquals(Optional.of("en"), AppSettings.get(file, "language"));
      assertEquals(Optional.of("D:\\Raffle Data"), AppSettings.get(file, "folder"));
   }

   @Test
   void fileHasNoTimestampComment() throws IOException {
      Path file = dir.resolve("settings.properties");
      AppSettings.put(file, "language", "ro");

      assertEquals(java.util.List.of("language=ro"), Files.readAllLines(file));
   }

   @Test
   void unreadableContentFallsBackToDefaults() throws IOException {
      Path file = dir.resolve("settings.properties");
      Files.writeString(file, "language=\\uZZZZ");// malformed unicode escape

      assertEquals(Optional.empty(), AppSettings.get(file, "language"));
   }
}
