package raffle.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What every {@link RaffleRepository} must do, whatever it stores the data in. Each implementation has a test
 * class that extends this one and says how to create its repository.
 */
abstract class RaffleRepositoryContract {

   @TempDir
   Path root;

   abstract RaffleRepository create(Path root) throws IOException;

   static List<Player> unsoldTickets(int count) {
      return IntStream.rangeClosed(1, count).mapToObj(id -> new Player(id, "", "", 0)).toList();
   }

   static Item item(String title, int tickets, double price) {
      return new Item("", title, "About " + title, tickets, price);
   }

   @Test
   void aNewRepositoryIsEmpty() throws IOException {
      try (RaffleRepository repository = create(root)) {
         assertTrue(repository.items().isEmpty());
         assertTrue(repository.draws().isEmpty());
         assertTrue(repository.ledger("Nothing").isEmpty());
         assertFalse(repository.hasItem("Nothing"));
      }
   }

   @Test
   void anAddedItemIsListedWithItsTickets() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(new Item("/pics/bike.png", "Bike", "A red bike", 3, 12.5), unsoldTickets(3));

         List<Item> items = repository.items();
         assertEquals(1, items.size());
         Item bike = items.getFirst();
         assertEquals("Bike", bike.getTitle());
         assertEquals("A red bike", bike.getDescription());
         assertEquals("/pics/bike.png", bike.getImage());
         assertEquals(12.5, bike.getPrice());
         assertEquals(3, bike.getTickets());
         assertTrue(repository.hasItem("Bike"));
         assertEquals(List.of(1, 2, 3), repository.ledger("Bike").stream().map(Player::getId).toList());
      }
   }

   @Test
   void itemsComeInTitleOrder() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Phone", 2, 5), unsoldTickets(2));
         repository.addItem(item("Bike", 2, 5), unsoldTickets(2));
         repository.addItem(item("Laptop", 2, 5), unsoldTickets(2));

         assertEquals(List.of("Bike", "Laptop", "Phone"), repository.items().stream().map(Item::getTitle).toList());
      }
   }

   @Test
   void theLedgerKeepsEveryDetailOfASale() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 3, 10), unsoldTickets(3));

         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(1, new Player(2, "Ioana Ștefănescu", "+40712345678", 2, false, "2026-09-30T18:00:00Z"));
         ledger.set(2, new Player(3, "Ioana Ștefănescu", "+40712345678", 2, true, "2026-09-30T18:05:00Z"));
         repository.saveLedger("Bike", ledger);

         List<Player> stored = repository.ledger("Bike");
         assertEquals(3, stored.size());
         assertFalse(stored.get(0).isSold());
         assertEquals("Ioana Ștefănescu", stored.get(1).getName());
         assertEquals("+40712345678", stored.get(1).getPhoneNumber());
         assertEquals(2, stored.get(1).getNumberOfTickets());
         assertFalse(stored.get(1).isPaid());
         assertEquals("2026-09-30T18:00:00Z", stored.get(1).getSoldAt());
         assertTrue(stored.get(2).isPaid());
      }
   }

   @Test
   void savingALedgerUpdatesTheAvailableTicketsOfTheItem() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 3, 10), unsoldTickets(3));

         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 1, true, ""));
         repository.saveLedger("Bike", ledger);

         assertEquals(2, repository.items().getFirst().getTickets());
      }
   }

   @Test
   void ledgersOfDifferentItemsAreKeptApart() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.addItem(item("Phone", 4, 10), unsoldTickets(4));

         assertEquals(2, repository.ledger("Bike").size());
         assertEquals(4, repository.ledger("Phone").size());
      }
   }

   @Test
   void textWithCommasQuotesAndAccentsWorks() throws IOException {
      try (RaffleRepository repository = create(root)) {
         // the title is also a file name in the CSV layout: no characters a file name cannot have on Windows or
         // in a POSIX locale (quotes, accents); the comma is what matters for CSV
         String title = "Telefon Pro, 128GB";
         repository.addItem(new Item("", title, "Descriere, cu virgulă și \"ghilimele\"", 2, 99.5), unsoldTickets(2));

         Item stored = repository.items().getFirst();
         assertEquals(title, stored.getTitle());
         assertEquals("Descriere, cu virgulă și \"ghilimele\"", stored.getDescription());
         assertEquals(99.5, stored.getPrice());
         assertEquals(2, repository.ledger(title).size());
      }
   }

   @Test
   void theMainPictureCanBeChanged() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 1, 10), unsoldTickets(1));
         repository.addItem(item("Phone", 1, 10), unsoldTickets(1));

         repository.setItemImage("Bike", "/pics/new.png");

         assertEquals("/pics/new.png", repository.items().get(0).getImage());
         assertEquals("", repository.items().get(1).getImage());
      }
   }

   @Test
   void aRemovedItemDisappearsButItsLedgerIsKept() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.addItem(item("Phone", 2, 10), unsoldTickets(2));
         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 1, true, ""));
         repository.saveLedger("Bike", ledger);

         repository.removeItem("Bike");

         assertEquals(List.of("Phone"), repository.items().stream().map(Item::getTitle).toList());
         assertFalse(repository.hasItem("Bike"));
         assertTrue(repository.ledger("Bike").isEmpty());
         try (Stream<Path> archived = Files.list(root.resolve("backups").resolve("deleted"))) {
            List<Path> files = archived.toList();
            assertEquals(1, files.size());
            assertTrue(Files.readString(files.getFirst()).contains("Ion"), "the archived ledger keeps the buyer");
         }
      }
   }

   @Test
   void aRemovedTitleCanBeUsedAgain() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.removeItem("Bike");

         repository.addItem(item("Bike", 5, 20), unsoldTickets(5));

         assertEquals(5, repository.ledger("Bike").size());
         assertEquals(20.0, repository.items().getFirst().getPrice());
      }
   }

   @Test
   void drawsAreKeptInOrder() throws IOException {
      try (RaffleRepository repository = create(root)) {
         Instant first = Instant.parse("2026-09-30T18:00:00Z");
         Instant second = Instant.parse("2026-09-30T18:05:00Z");
         repository.recordDraw("Bike", new Player(4, "Ion Popescu", "0712345678", 3), first);
         repository.recordDraw("Phone, 128GB", new Player(9, "O\"Brien", "+40700000000", 1), second);

         assertEquals(List.of(new DrawEntry(first, "Bike", 4, "Ion Popescu", "0712345678"),
                              new DrawEntry(second, "Phone, 128GB", 9, "O\"Brien", "+40700000000")),
                      repository.draws());
      }
   }

   @Test
   void dataSurvivesReopening() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.recordDraw("Bike", new Player(1, "Ion", "0712345678", 1), Instant.parse("2026-09-30T18:00:00Z"));
      }
      try (RaffleRepository reopened = create(root)) {
         assertEquals(1, reopened.items().size());
         assertEquals(2, reopened.ledger("Bike").size());
         assertEquals(1, reopened.draws().size());
      }
   }

   @Test
   void aTicketKeepsThePriceItWasSoldFor() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 1, true, "", 1250));
         repository.saveLedger("Bike", ledger);

         List<Player> stored = repository.ledger("Bike");
         assertEquals(1250, stored.get(0).getPriceCents());
         assertEquals(0, stored.get(1).getPriceCents());
      }
   }

   @Test
   void oneItemCanBeLookedUpByTitle() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         repository.addItem(item("Phone", 2, 99.5), unsoldTickets(2));

         assertEquals(99.5, repository.item("Phone").orElseThrow().getPrice());
         assertTrue(repository.item("Nothing").isEmpty());
      }
   }

   @Test
   void anUpdateChangesTheDescriptionThePriceAndTheTickets() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 1, true, "", 1000));
         ledger.add(new Player(3, "", "", 0));
         ledger.add(new Player(4, "", "", 0));

         repository.updateItem("Bike", "New description", 12.5, ledger);

         Item bike = repository.items().getFirst();
         assertEquals("New description", bike.getDescription());
         assertEquals(12.5, bike.getPrice());
         assertEquals(3, bike.getTickets());
         assertEquals(4, repository.ledger("Bike").size());
         assertEquals("Ion", repository.ledger("Bike").getFirst().getName());
         assertEquals(1000, repository.ledger("Bike").getFirst().getPriceCents());
      }
   }

   @Test
   void anUpdateKeepsTheTitleAndThePicture() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(new Item("/pics/bike.png", "Bike", "Red", 2, 10), unsoldTickets(2));

         repository.updateItem("Bike", "Blue", 11, repository.ledger("Bike"));

         assertEquals("Bike", repository.items().getFirst().getTitle());
         assertEquals("/pics/bike.png", repository.items().getFirst().getImage());
      }
   }

   @Test
   void soldTicketsWithoutARecordedPriceKeepTheOldPriceWhenThePriceChanges() throws IOException {
      try (RaffleRepository repository = create(root)) {
         repository.addItem(item("Bike", 2, 10), unsoldTickets(2));
         List<Player> ledger = new ArrayList<>(repository.ledger("Bike"));
         ledger.set(0, new Player(1, "Ion", "0712345678", 1, true, "", 0));// sold, price not recorded

         repository.updateItem("Bike", "Red", 20, ledger);

         assertEquals(1000, repository.ledger("Bike").getFirst().getPriceCents());
      }
   }

   @Test
   void updatingAnUnknownItemFails() throws IOException {
      try (RaffleRepository repository = create(root)) {
         org.junit.jupiter.api.Assertions.assertThrows(IOException.class,
                 () -> repository.updateItem("Nothing", "x", 1, unsoldTickets(1)));
      }
   }
}
