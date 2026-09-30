package raffle.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Item;
import raffle.models.Player;
import raffle.storage.CsvRaffleRepository;
import raffle.storage.DelegatingRepository;
import raffle.storage.RaffleRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {

   @TempDir
   Path root;

   RaffleRepository repository;
   ItemService service;

   @BeforeEach
   void setUp() {
      repository = new CsvRaffleRepository(root);
      service = new ItemService(repository, root);
   }

   private String reason(String title, String description, int tickets, double price) {
      return assertThrows(ValidationException.class, () -> service.create(title, description, tickets, price, null)).messageKey();
   }

   @Test
   void titlesAreMadeSafeForFolderNames() {
      assertEquals("Bike", ItemService.cleanTitle("  Bike  "));
      assertEquals("a_b_c_d_e_f_g_h_i", ItemService.cleanTitle("a<b>c:d\"e/f\\g|h?i"));
      assertEquals("", ItemService.cleanTitle("   "));
      assertEquals("", ItemService.cleanTitle(null));
   }

   @Test
   void anItemGetsOneUnsoldTicketPerNumber() throws Exception {
      Item item = service.create("Bike", "A red bike", 4, 12.5, null);

      assertEquals("Bike", item.getTitle());
      assertEquals("", item.getImage(), "no picture means the logo is shown");
      List<Player> ledger = repository.ledger("Bike");
      assertEquals(List.of(1, 2, 3, 4), ledger.stream().map(Player::getId).toList());
      assertTrue(ledger.stream().noneMatch(Player::isSold));
      assertEquals(12.5, repository.items().getFirst().getPrice());
      assertTrue(Files.isDirectory(root.resolve("Bike")), "the folder for the item's pictures exists");
   }

   @Test
   void theChosenPictureIsCopiedIntoTheItemsFolder() throws Exception {
      Path picture = Files.writeString(root.resolve("elsewhere.png"), "not really a picture");

      Item item = service.create("Bike", "Red", 2, 10, picture);

      Path copy = root.resolve("Bike").resolve("elsewhere.png");
      assertTrue(Files.exists(copy));
      assertEquals(copy.toAbsolutePath().normalize().toString(), Path.of(item.getImage()).toAbsolutePath().normalize().toString());
      assertEquals(item.getImage(), repository.items().getFirst().getImage());
      assertTrue(Files.exists(picture), "the original stays where it was");
   }

   @Test
   void theTitleIsCleanedBeforeItIsUsed() throws Exception {
      Item item = service.create("  Phone: 128GB/Pro ", "Black", 2, 10, null);

      assertEquals("Phone_ 128GB_Pro", item.getTitle());
      assertTrue(repository.hasItem("Phone_ 128GB_Pro"));
   }

   @Test
   void badInputIsRefusedWithTheReason() {
      assertEquals("val.titleEmpty", reason("   ", "Red", 2, 10));
      assertEquals("val.descriptionEmpty", reason("Bike", " ", 2, 10));
      assertEquals("val.ticketsPositiveInt", reason("Bike", "Red", 0, 10));
      assertEquals("val.pricePositive", reason("Bike", "Red", 2, 0));
      assertEquals("val.pricePositive", reason("Bike", "Red", 2, -1));
      assertEquals("val.pricePositive", reason("Bike", "Red", 2, Double.NaN));
      assertEquals("val.pricePositive", reason("Bike", "Red", 2, Double.POSITIVE_INFINITY));
   }

   @Test
   void aRefusedItemLeavesNothingBehind() throws Exception {
      reason("Bike", "Red", 0, 10);

      assertTrue(repository.items().isEmpty());
      assertFalse(Files.exists(root.resolve("Bike")));
   }

   @Test
   void aTitleCannotBeUsedTwice() throws Exception {
      service.create("Bike", "Red", 2, 10, null);

      ValidationException error = assertThrows(ValidationException.class, () -> service.create("Bike", "Blue", 3, 20, null));

      assertEquals("additem.exists", error.messageKey());
      assertEquals("Bike", error.arguments()[0]);
      assertEquals(1, repository.items().size());
   }

   @Test
   void aTitleThatIsAnApplicationFolderIsRefused() throws Exception {
      Files.createDirectories(root.resolve("data"));

      assertEquals("additem.exists", reason("data", "Red", 2, 10));
   }

   // A repository that cannot save new items
   private static final class FullDisk extends DelegatingRepository {
      FullDisk(RaffleRepository real) {
         super(real);
      }

      @Override
      public void addItem(Item item, List<Player> tickets) throws IOException {
         throw new IOException("disk full");
      }
   }

   @Test
   void aFailedSaveLeavesNoFolderBehind() {
      assertThrows(IOException.class, () -> new ItemService(new FullDisk(repository), root).create("Bike", "Red", 2, 10, null));

      assertFalse(Files.exists(root.resolve("Bike")));
   }

   @Test
   void overviewShowsEachItemWithItsSalesFigures() throws Exception {
      service.create("Bike", "Red", 4, 10, null);
      new SalesService(repository).sell("Bike", "Ion", "0712345678", 2, true);
      new SalesService(repository).sell("Bike", "Ana", "0700000000", 1, false);

      List<ItemOverview> overview = service.overview();

      assertEquals(1, overview.size());
      ItemSales sales = overview.getFirst().sales();
      assertEquals(4, sales.totalTickets());
      assertEquals(3, sales.soldTickets());
      assertEquals(2, sales.paidTickets());
      assertEquals(20.0, sales.collected());
      assertEquals(10.0, sales.outstanding());
   }

   @Test
   void deletingRemovesTheItemAndItsPicturesButKeepsTheLedger() throws Exception {
      Path picture = Files.writeString(root.resolve("x.png"), "x");
      service.create("Bike", "Red", 2, 10, picture);
      new SalesService(repository).sell("Bike", "Ion", "0712345678", 1, true);

      service.delete("Bike");

      assertTrue(repository.items().isEmpty());
      assertFalse(Files.exists(root.resolve("Bike")));
      try (Stream<Path> archived = Files.list(root.resolve("backups").resolve("deleted"))) {
         assertEquals(1, archived.count());
      }
   }

   @Test
   void theMainPictureCanBeChosen() throws Exception {
      service.create("Bike", "Red", 2, 10, null);
      Path picture = root.resolve("Bike").resolve("side.png");
      Files.writeString(picture, "x");

      service.setMainPicture("Bike", picture);

      assertEquals(picture.toAbsolutePath().toString(), repository.items().getFirst().getImage());
   }

   private String updateReason(String title, String description, int tickets, double price) {
      return assertThrows(ValidationException.class, () -> service.update(title, description, tickets, price)).messageKey();
   }

   @Test
   void anItemCanBeEdited() throws Exception {
      service.create("Bike", "Red", 4, 10, null);

      service.update("Bike", "  Blue  ", 6, 12.5);

      Item bike = repository.items().getFirst();
      assertEquals("Blue", bike.getDescription());
      assertEquals(12.5, bike.getPrice());
      assertEquals(6, repository.ledger("Bike").size());
      assertEquals(6, bike.getTickets());
   }

   @Test
   void editingThePriceDoesNotChangeWhatSoldTicketsWentFor() throws Exception {
      service.create("Bike", "Red", 4, 10, null);
      new SalesService(repository).sell("Bike", "Ion", "0712345678", 2, true);

      service.update("Bike", "Red", 4, 20);
      new SalesService(repository).sell("Bike", "Ana", "0700000000", 1, false);

      ItemSales sales = service.overview().getFirst().sales();
      assertEquals(20.0, sales.collected(), "two tickets sold at 10.00");
      assertEquals(20.0, sales.outstanding(), "one ticket sold at 20.00 and not paid yet");
      assertEquals(20.0 + 20.0 + 20.0, sales.potential(), "sold ones as sold, the one unsold ticket at 20.00");
   }

   @Test
   void theTotalOfTicketsCanGrowAndShrinkWhileTheEndIsUnsold() throws Exception {
      service.create("Bike", "Red", 5, 10, null);
      new SalesService(repository, new java.util.Random(1), java.time.Clock.systemUTC()).sell("Bike", "Ion", "0712345678", 1, true);
      int highestSold = TicketSales.highestSoldId(repository.ledger("Bike"));

      service.update("Bike", "Red", highestSold, 10);
      assertEquals(highestSold, repository.ledger("Bike").size());

      service.update("Bike", "Red", 8, 10);
      assertEquals(8, repository.ledger("Bike").size());
      assertEquals(7, TicketSales.availableIds(repository.ledger("Bike")).size());
   }

   @Test
   void theTotalCannotDropBelowASoldTicket() throws Exception {
      service.create("Bike", "Red", 5, 10, null);
      new SalesService(repository).sell("Bike", "Ion", "0712345678", 5, true);

      assertEquals("edit.ticketsSold", updateReason("Bike", "Red", 4, 10));
      assertEquals(5, repository.ledger("Bike").size());
   }

   @Test
   void badEditsAreRefusedWithTheReason() throws Exception {
      service.create("Bike", "Red", 5, 10, null);

      assertEquals("val.descriptionEmpty", updateReason("Bike", " ", 5, 10));
      assertEquals("val.ticketsPositiveInt", updateReason("Bike", "Red", 0, 10));
      assertEquals("val.pricePositive", updateReason("Bike", "Red", 5, 0));
      assertEquals("val.pricePositive", updateReason("Bike", "Red", 5, Double.NaN));
   }

   @Test
   void editingAnItemThatDoesNotExistFails() {
      assertThrows(IOException.class, () -> service.update("Nothing", "x", 1, 1));
   }
}
