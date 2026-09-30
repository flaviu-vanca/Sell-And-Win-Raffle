package raffle.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.DrawEntry;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvDrawLogTest {

   @TempDir
   Path dir;

   @Test
   void missingFileMeansNoDrawsYet() throws IOException {
      assertTrue(new CsvDrawLog(dir.resolve("draws.csv")).readAll().isEmpty());
   }

   @Test
   void recordsAreAppendedAndReadBackInOrder() throws IOException {
      CsvDrawLog history = new CsvDrawLog(dir.resolve("data").resolve("draws.csv"));
      Instant first = Instant.parse("2026-09-30T18:00:00Z");
      Instant second = Instant.parse("2026-09-30T18:05:00Z");

      history.record("Bike", new Player(4, "Ion Popescu", "0712345678", 3), first);
      history.record("Phone, 128GB", new Player(9, "O\"Brien", "+40700000000", 1), second);

      List<DrawEntry> entries = history.readAll();
      assertEquals(2, entries.size());
      assertEquals(new DrawEntry(first, "Bike", 4, "Ion Popescu", "0712345678"), entries.get(0));
      assertEquals(new DrawEntry(second, "Phone, 128GB", 9, "O\"Brien", "+40700000000"), entries.get(1));
   }

   @Test
   void headerIsWrittenOnlyOnce() throws IOException {
      Path file = dir.resolve("draws.csv");
      CsvDrawLog history = new CsvDrawLog(file);
      history.record("Bike", new Player(1, "A", "0700000001", 1), Instant.now());
      history.record("Bike", new Player(2, "B", "0700000002", 1), Instant.now());

      List<String> lines = Files.readAllLines(file);
      assertEquals(3, lines.size());
      assertEquals(CsvDrawLog.HEADER, lines.getFirst());
   }
}
