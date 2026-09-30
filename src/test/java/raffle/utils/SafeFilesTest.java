package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SafeFilesTest {

   @TempDir
   Path dir;

   @Test
   void writesAllLinesAndLeavesNoTemporaryFile() throws IOException {
      Path file = dir.resolve("records.csv");
      SafeFiles.writeLinesAtomically(file, List.of("ID,Name", "1,\"Ana\""));

      assertEquals(List.of("ID,Name", "1,\"Ana\""), Files.readAllLines(file));
      try (Stream<Path> files = Files.list(dir)) {
         assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
      }
   }

   @Test
   void overwriteKeepsThePreviousVersionAsBackup() throws IOException {
      Path file = dir.resolve("records.csv");
      SafeFiles.writeLinesAtomically(file, List.of("first"));
      SafeFiles.writeLinesAtomically(file, List.of("second"));

      assertEquals(List.of("second"), Files.readAllLines(file));
      assertEquals(List.of("first"), Files.readAllLines(dir.resolve("records.csv.bak")));
   }

   @Test
   void createsMissingParentDirectories() throws IOException {
      Path file = dir.resolve("a").resolve("b").resolve("data.csv");
      SafeFiles.writeLinesAtomically(file, List.of("x"));

      assertEquals(List.of("x"), Files.readAllLines(file));
   }

   @Test
   void failedWriteLeavesExistingFileUntouched() throws IOException {
      Path file = dir.resolve("records.csv");
      SafeFiles.writeLinesAtomically(file, List.of("keep me"));
      // the parent "directory" of this target is a regular file, so the write cannot succeed
      Path impossible = file.resolve("child.csv");

      assertThrows(IOException.class, () -> SafeFiles.writeLinesAtomically(impossible, List.of("lost")));
      assertEquals(List.of("keep me"), Files.readAllLines(file));
   }
}
