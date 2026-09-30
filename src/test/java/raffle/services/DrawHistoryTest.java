package raffle.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawHistoryTest {

   @TempDir
   Path dir;

   @Test
   void missingFileMeansNoDrawsYet() throws IOException {
      assertTrue(new DrawHistory(dir.resolve("draws.csv")).readAll().isEmpty());
   }

   @Test
   void recordsAreAppendedAndReadBackInOrder() throws IOException {
      DrawHistory history = new DrawHistory(dir.resolve("data").resolve("draws.csv"));
      Instant first = Instant.parse("2026-09-30T18:00:00Z");
      Instant second = Instant.parse("2026-09-30T18:05:00Z");

      history.record("Bike", new Player(4, "Ion Popescu", "0712345678", 3), first);
      history.record("Phone, 128GB", new Player(9, "O\"Brien", "+40700000000", 1), second);

      List<DrawHistory.Entry> entries = history.readAll();
      assertEquals(2, entries.size());
      assertEquals(new DrawHistory.Entry(first, "Bike", 4, "Ion Popescu", "0712345678"), entries.get(0));
      assertEquals(new DrawHistory.Entry(second, "Phone, 128GB", 9, "O\"Brien", "+40700000000"), entries.get(1));
   }

   @Test
   void headerIsWrittenOnlyOnce() throws IOException {
      Path file = dir.resolve("draws.csv");
      DrawHistory history = new DrawHistory(file);
      history.record("Bike", new Player(1, "A", "0700000001", 1), Instant.now());
      history.record("Bike", new Player(2, "B", "0700000002", 1), Instant.now());

      List<String> lines = Files.readAllLines(file);
      assertEquals(3, lines.size());
      assertEquals(DrawHistory.HEADER, lines.getFirst());
   }

   @Test
   void winningTicketsAreListedPerItem() throws IOException {
      DrawHistory history = new DrawHistory(dir.resolve("draws.csv"));
      history.record("Bike", new Player(4, "A", "0700000001", 1), Instant.now());
      history.record("Phone", new Player(8, "B", "0700000002", 1), Instant.now());
      history.record("Bike", new Player(17, "C", "0700000003", 1), Instant.now());

      assertEquals(List.of(4, 17), history.winningTicketIds("Bike"));
      assertEquals(List.of(8), history.winningTicketIds("Phone"));
      assertTrue(history.winningTicketIds("Unknown").isEmpty());
   }
}
