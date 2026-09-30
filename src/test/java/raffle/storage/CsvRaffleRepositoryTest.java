package raffle.storage;

import org.junit.jupiter.api.Test;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvRaffleRepositoryTest extends RaffleRepositoryContract {

   @Override
   RaffleRepository create(Path root) {
      return new CsvRaffleRepository(root);
   }

   @Test
   void usesTheFileLayoutOfEarlierVersions() throws IOException {
      RaffleRepository repository = create(root);
      repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
      repository.recordDraw("Bike", new Player(1, "Ion", "0712345678", 1), java.time.Instant.now());

      assertTrue(Files.exists(root.resolve("data").resolve("data.csv")));
      assertTrue(Files.exists(root.resolve("records").resolve("Bike.csv")));
      assertTrue(Files.exists(root.resolve("data").resolve("draws.csv")));
   }

   @Test
   void readsFilesWrittenByEarlierVersions() throws IOException {
      Files.createDirectories(root.resolve("data"));
      Files.createDirectories(root.resolve("records"));
      Files.writeString(root.resolve("data").resolve("data.csv"),
                        "Image,Title,Description,Available Tickets,Ticket Price\n\"null\",\"Bike\",\"Red\",1,25.0\n");
      Files.writeString(root.resolve("records").resolve("Bike.csv"),
                        "ID,Name,Phone Number,Number of Tickets\n1,\"Ion\",\"0712345678\",1\n2,\"\",\"\",0\n");

      RaffleRepository repository = create(root);
      Item bike = repository.items().getFirst();
      List<Player> ledger = repository.ledger("Bike");

      assertEquals("", bike.getImage());
      assertEquals(2, ledger.size());
      assertTrue(ledger.get(0).isSold());
      assertTrue(ledger.get(0).isPaid(), "tickets sold before payments were tracked count as paid");
      assertFalse(ledger.get(1).isSold());
   }

   @Test
   void aBrokenNumberIsReportedAsAnIoError() throws IOException {
      Files.createDirectories(root.resolve("data"));
      Files.writeString(root.resolve("data").resolve("data.csv"),
                        "Image,Title,Description,Available Tickets,Ticket Price\n\"\",\"Bike\",\"Red\",many,25.0\n");

      RaffleRepository repository = create(root);
      try {
         repository.items();
         org.junit.jupiter.api.Assertions.fail("expected an IOException");
      } catch (IOException expected) {
         assertTrue(expected.getMessage().contains("data.csv"), expected.getMessage());
      }
   }

   @Test
   void aLedgerWithoutACatalogEntryBlocksItsTitle() throws IOException {
      Files.createDirectories(root.resolve("records"));
      Files.writeString(root.resolve("records").resolve("Bike.csv"), "ID,Name,Phone Number,Number of Tickets\n");

      assertTrue(create(root).hasItem("Bike"));
   }
}
