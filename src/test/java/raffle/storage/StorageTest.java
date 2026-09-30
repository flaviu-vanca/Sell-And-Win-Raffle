package raffle.storage;

import org.junit.jupiter.api.AfterEach;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageTest {

   @TempDir
   Path root;

   @AfterEach
   void closeStorage() {
      Storage.close();
      Storage.takeNotice();
   }

   // Data as an earlier version left it: CSV files only
   private void writeCsvData() throws IOException {
      try (RaffleRepository csv = new CsvRaffleRepository(root)) {
         csv.addItem(new Item("", "Bike", "Red", 3, 12.5), RaffleRepositoryContract.unsoldTickets(3));
         List<Player> ledger = new ArrayList<>(csv.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion Popescu", "0712345678", 1, false, "2026-09-30T18:00:00Z"));
         csv.saveLedger("Bike", ledger);
         csv.addItem(new Item("", "Phone", "Black", 2, 99.5), RaffleRepositoryContract.unsoldTickets(2));
         csv.recordDraw("Bike", new Player(1, "Ion Popescu", "0712345678", 1), Instant.parse("2026-09-30T19:00:00Z"));
      }
   }

   @Test
   void aFreshStartUsesANewDatabaseAndSaysNothing() throws IOException {
      RaffleRepository repository = Storage.open(root);

      assertInstanceOf(SqliteRaffleRepository.class, repository);
      assertTrue(Files.exists(root.resolve("data").resolve("raffle.db")));
      assertTrue(Storage.takeNotice().isEmpty());
   }

   @Test
   void csvFilesOfAnEarlierVersionAreConvertedOnTheFirstStart() throws IOException {
      writeCsvData();

      RaffleRepository repository = Storage.open(root);

      assertInstanceOf(SqliteRaffleRepository.class, repository);
      assertEquals(List.of("Bike", "Phone"), repository.items().stream().map(Item::getTitle).toList());
      assertEquals(12.5, repository.items().getFirst().getPrice());
      List<Player> bike = repository.ledger("Bike");
      assertEquals(3, bike.size());
      assertEquals("Ion Popescu", bike.getFirst().getName());
      assertFalse(bike.getFirst().isPaid());
      assertEquals("2026-09-30T18:00:00Z", bike.getFirst().getSoldAt());
      assertEquals(2, repository.ledger("Phone").size());
      assertEquals(1, repository.draws().size());

      Storage.Notice notice = Storage.takeNotice().orElseThrow();
      assertEquals("storage.imported", notice.messageKey());
      assertFalse(notice.warning());
      assertEquals("2", notice.arguments()[0]);
      assertEquals("5", notice.arguments()[1]);
      assertTrue(Storage.takeNotice().isEmpty(), "the notice is handed out once");
   }

   @Test
   void theCsvFilesAreLeftUntouched() throws IOException {
      writeCsvData();
      String catalog = Files.readString(root.resolve("data").resolve("data.csv"));
      String ledger = Files.readString(root.resolve("records").resolve("Bike.csv"));

      Storage.open(root);

      assertEquals(catalog, Files.readString(root.resolve("data").resolve("data.csv")));
      assertEquals(ledger, Files.readString(root.resolve("records").resolve("Bike.csv")));
   }

   @Test
   void theConversionHappensOnlyOnce() throws IOException {
      writeCsvData();
      Storage.open(root).addItem(new Item("", "Laptop", "Grey", 1, 500), RaffleRepositoryContract.unsoldTickets(1));
      Storage.close();
      Storage.takeNotice();

      RaffleRepository again = Storage.open(root);

      assertEquals(3, again.items().size(), "the item added after the conversion is still there");
      assertTrue(Storage.takeNotice().isEmpty());
   }

   @Test
   void noTemporaryFileIsLeftBehind() throws IOException {
      writeCsvData();
      Storage.open(root);

      assertFalse(Files.exists(root.resolve("data").resolve("raffle.db.importing")));
   }

   @Test
   void unreadableCsvFilesKeepTheCsvInUseInsteadOfStartingEmpty() throws IOException {
      Files.createDirectories(root.resolve("data"));
      Files.writeString(root.resolve("data").resolve("data.csv"),
                        "Image,Title,Description,Available Tickets,Ticket Price\n\"\",\"Bike\",\"Red\",many,25.0\n");

      RaffleRepository repository = Storage.open(root);

      assertInstanceOf(CsvRaffleRepository.class, repository);
      assertFalse(Files.exists(root.resolve("data").resolve("raffle.db")), "no half converted database");
      assertFalse(Files.exists(root.resolve("data").resolve("raffle.db.importing")));
      Storage.Notice notice = Storage.takeNotice().orElseThrow();
      assertEquals("storage.importFailed", notice.messageKey());
      assertTrue(notice.warning());
   }

   @Test
   void dataSavedInTheDatabaseSurvivesARestart() throws IOException {
      Storage.open(root).addItem(new Item("", "Bike", "Red", 2, 10), RaffleRepositoryContract.unsoldTickets(2));
      Storage.close();

      assertEquals(1, Storage.open(root).items().size());
   }
}
