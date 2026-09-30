package raffle.storage;

import org.junit.jupiter.api.Test;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqliteRaffleRepositoryTest extends RaffleRepositoryContract {

   private Path db() {
      return root.resolve("data").resolve("raffle.db");
   }

   @Override
   RaffleRepository create(Path root) throws IOException {
      return new SqliteRaffleRepository(root, root.resolve("data").resolve("raffle.db"));
   }

   @Test
   void theDatabaseLivesInOneFile() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
      }
      assertTrue(Files.size(db()) > 0);
   }

   @Test
   void aNewDatabaseGetsTheCurrentLayout() throws Exception {
      create(root).close();

      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db())) {
         assertEquals(SqliteSchema.currentVersion(), SqliteSchema.version(connection));
      }
   }

   @Test
   void aDatabaseFromANewerVersionIsNotTouched() throws Exception {
      create(root).close();
      try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + db());
           Statement statement = connection.createStatement()) {
         statement.execute("PRAGMA user_version = " + (SqliteSchema.currentVersion() + 1));
      }

      IOException error = assertThrows(IOException.class, () -> create(root));

      assertTrue(error.getMessage().contains("newer version"), error.getMessage());
   }

   @Test
   void aBuyersTicketCountIsWorkedOutFromTheTicketsNotTrustedFromTheRow() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 4, 10), unsoldTickets(4));
         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 99, true, ""));
         ledger.set(2, new Player(3, "ion", "0712345678", 99, true, ""));
         repository.saveLedger("Bike", ledger);

         List<Player> stored = repository.ledger("Bike");
         assertEquals(2, stored.get(0).getNumberOfTickets());
         assertEquals(2, stored.get(2).getNumberOfTickets());
         assertEquals(0, stored.get(1).getNumberOfTickets());
      }
   }

   @Test
   void aFailedSaveLeavesTheLedgerAsItWas() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 3, 10), unsoldTickets(3));
         List<Player> good = new ArrayList<>(repository.ledger("Bike"));
         good.set(0, new Player(1, "Ion", "0712345678", 1, true, ""));
         repository.saveLedger("Bike", good);

         // two rows with the same ticket number break the primary key half way through the save
         List<Player> broken = List.of(new Player(1, "Ana", "0700000000", 1, true, ""), new Player(1, "Ana", "0700000000", 1, true, ""));
         assertThrows(IOException.class, () -> repository.saveLedger("Bike", broken));

         List<Player> stored = repository.ledger("Bike");
         assertEquals(3, stored.size());
         assertEquals("Ion", stored.getFirst().getName());
      }
   }

   @Test
   void aTitleCannotBeAddedTwice() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));

         assertThrows(IOException.class, () -> repository.addItem(item("Bike", 5, 20), unsoldTickets(5)));

         assertEquals(2, repository.ledger("Bike").size());
         assertEquals(1, repository.items().size());
      }
   }

   @Test
   void savingTheLedgerOfAnUnknownItemFails() throws IOException {
      try (RaffleRepository repository = create(root)) {
         assertThrows(IOException.class, () -> repository.saveLedger("Nothing", unsoldTickets(2)));
      }
   }

   @Test
   void theFolderNameMayContainSpacesAndAnAmpersand() throws IOException {
      Path odd = root.resolve("Sell & Win Raffle");
      try (RaffleRepository repository = new SqliteRaffleRepository(odd, odd.resolve("data").resolve("raffle.db"))) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         assertEquals(2, repository.ledger("Bike").size());
      }
   }

   @Test
   void aLedgerWithABlankNameCountsAsUnsold() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.saveLedger("Bike", List.of(new Player(1, "   ", "0712345678", 1, true, ""), new Player(2, "", "", 0)));

         assertEquals(2, repository.items().getFirst().getTickets());
      }
   }
}
