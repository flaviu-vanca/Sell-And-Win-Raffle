package raffle.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvTransferTest {

   @TempDir
   Path dir;

   private RaffleRepository filled() throws IOException {
      RaffleRepository repository = new SqliteRaffleRepository(dir.resolve("a"), dir.resolve("a").resolve("raffle.db"));
      repository.addItem(new Item("/pics/bike.png", "Bike", "Red, fast", 3, 12.5), RaffleRepositoryContract.unsoldTickets(3));
      List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
      ledger.set(1, new Player(2, "Ioana Ștefănescu", "+40712345678", 1, true, "2026-09-30T18:00:00Z"));
      repository.saveLedger("Bike", ledger);
      repository.addItem(new Item("", "Phone", "Black", 1, 99.5), RaffleRepositoryContract.unsoldTickets(1));
      repository.recordDraw("Bike", new Player(2, "Ioana Ștefănescu", "+40712345678", 1), Instant.parse("2026-09-30T19:00:00Z"));
      return repository;
   }

   @Test
   void everythingIsCopiedAndCounted() throws IOException {
      try (RaffleRepository from = filled(); RaffleRepository to = new CsvRaffleRepository(dir.resolve("b"))) {
         CsvTransfer.Summary summary = CsvTransfer.copy(from, to);

         assertEquals(new CsvTransfer.Summary(2, 4, 1), summary);
         assertEquals(from.items().stream().map(Item::getTitle).toList(), to.items().stream().map(Item::getTitle).toList());
         assertEquals("/pics/bike.png", to.items().getFirst().getImage());
         assertEquals("Ioana Ștefănescu", to.ledger("Bike").get(1).getName());
         assertEquals(from.draws(), to.draws());
      }
   }

   @Test
   void theExportIsInTheCsvLayoutOfEarlierVersions() throws IOException {
      try (RaffleRepository from = filled()) {
         Path folder = CsvTransfer.exportTo(from, dir.resolve("exports"));

         assertEquals(dir.resolve("exports"), folder.getParent());
         assertTrue(Files.exists(folder.resolve("data").resolve("data.csv")));
         assertTrue(Files.exists(folder.resolve("data").resolve("draws.csv")));
         assertTrue(Files.exists(folder.resolve("records").resolve("Bike.csv")));
         assertTrue(Files.readString(folder.resolve("records").resolve("Bike.csv")).contains("Ioana Ștefănescu"));
         try (java.util.stream.Stream<Path> files = Files.walk(folder)) {
            assertTrue(files.noneMatch(file -> file.toString().endsWith(".bak")), "no backup copies in an export");
         }
      }
   }

   @Test
   void anExportCanBeImportedAgain() throws IOException {
      try (RaffleRepository from = filled()) {
         Path folder = CsvTransfer.exportTo(from, dir.resolve("exports"));

         try (RaffleRepository back = new SqliteRaffleRepository(dir.resolve("c"), dir.resolve("c").resolve("raffle.db"))) {
            CsvTransfer.copy(new CsvRaffleRepository(folder), back);
            assertEquals(from.ledger("Bike").stream().map(Player::getName).toList(), back.ledger("Bike").stream().map(Player::getName).toList());
            assertEquals(from.draws(), back.draws());
         }
      }
   }
}
