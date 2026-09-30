package raffle.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Item;
import raffle.models.Player;
import raffle.storage.CsvRaffleRepository;
import raffle.storage.RaffleRepository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SalesServiceTest {

   private static final Instant NOW = Instant.parse("2026-09-30T18:00:00Z");

   @TempDir
   Path root;

   RaffleRepository repository;
   SalesService service;

   @BeforeEach
   void setUp() throws IOException {
      repository = new CsvRaffleRepository(root);
      repository.addItem(new Item("", "Bike", "Red", 5, 10.0),
                         List.of(new Player(1, "", "", 0), new Player(2, "", "", 0), new Player(3, "", "", 0),
                                 new Player(4, "", "", 0), new Player(5, "", "", 0)));
      service = new SalesService(repository, new Random(7), Clock.fixed(NOW, ZoneOffset.UTC));
   }

   private String reason(String name, String phone, int tickets) {
      return assertThrows(ValidationException.class, () -> service.sell("Bike", name, phone, tickets, true)).messageKey();
   }

   @Test
   void aSaleIsSavedWithTheBuyersDetailsAndTheTime() throws Exception {
      SalesService.Sale sale = service.sell("Bike", "  Ion Popescu ", "0712 345 678", 2, false);
      List<Integer> ids = sale.ticketIds();

      assertEquals(2, ids.size());
      List<Player> stored = repository.ledger("Bike");
      assertEquals(3, TicketSales.availableIds(stored).size());
      Player ticket = stored.stream().filter(player -> player.getId() == ids.getFirst()).findFirst().orElseThrow();
      assertEquals("Ion Popescu", ticket.getName());
      assertEquals("0712345678", ticket.getPhoneNumber(), "spaces are removed from the phone number");
      assertFalse(ticket.isPaid());
      assertEquals(NOW.toString(), ticket.getSoldAt());
      assertEquals(1000, ticket.getPriceCents(), "the ticket remembers the price of the item at the time of the sale");
      assertEquals(2000, sale.totalCents());
   }

   @Test
   void aLaterPriceChangeDoesNotReachBackToTicketsAlreadySold() throws Exception {
      service.sell("Bike", "Ion", "0712345678", 1, true);
      repository.updateItem("Bike", "Red", 20.0, repository.ledger("Bike"));

      SalesService.Sale later = service.sell("Bike", "Ana", "0700000000", 1, true);

      assertEquals(2000, later.totalCents());
      List<Player> stored = repository.ledger("Bike");
      assertEquals(1000, stored.stream().filter(p -> "Ion".equals(p.getName())).findFirst().orElseThrow().getPriceCents());
      assertEquals(2000, stored.stream().filter(p -> "Ana".equals(p.getName())).findFirst().orElseThrow().getPriceCents());
   }

   @Test
   void badInputIsRefusedWithTheReason() {
      assertEquals("val.nameEmpty", reason("  ", "0712345678", 1));
      assertEquals("val.phoneEmpty", reason("Ion", "", 1));
      assertEquals("phone.invalid", reason("Ion", "12ab", 1));
      assertEquals("val.ticketsPositive", reason("Ion", "0712345678", 0));
      assertEquals("val.ticketsPositive", reason("Ion", "0712345678", -2));
      assertEquals("player.notEnough", reason("Ion", "0712345678", 6));
   }

   @Test
   void aRefusedSaleChangesNothing() throws Exception {
      reason("Ion", "0712345678", 6);

      assertEquals(5, TicketSales.availableIds(repository.ledger("Bike")).size());
   }

   @Test
   void anItemWithoutTicketsCannotSell() {
      assertEquals("player.notEnough", assertThrows(ValidationException.class,
              () -> service.sell("Unknown", "Ion", "0712345678", 1, true)).messageKey());
   }

   @Test
   void releasedTicketsCanBeSoldAgain() throws Exception {
      List<Integer> ids = service.sell("Bike", "Ion", "0712345678", 5, true).ticketIds();

      assertEquals(2, service.release("Bike", ids.subList(0, 2)));

      assertEquals(2, TicketSales.availableIds(repository.ledger("Bike")).size());
      assertEquals(3, repository.ledger("Bike").stream().filter(Player::isSold).findFirst().orElseThrow().getNumberOfTickets());
   }

   @Test
   void releasingNothingSoldSavesNothing() throws Exception {
      assertEquals(0, service.release("Bike", List.of(1, 2)));
   }

   @Test
   void payingMarksAllTicketsOfTheBuyer() throws Exception {
      service.sell("Bike", "Ion", "0712345678", 2, false);
      service.sell("Bike", "Ana", "0700000000", 1, false);
      Player ion = repository.ledger("Bike").stream().filter(p -> p.isSold() && p.getName().equals("Ion")).findFirst().orElseThrow();

      int changed = service.setPaid("Bike", Set.of(ion.buyerKey()), true);

      assertEquals(2, changed);
      List<Player> stored = repository.ledger("Bike");
      assertTrue(stored.stream().filter(p -> "Ion".equals(p.getName())).allMatch(Player::isPaid));
      assertTrue(stored.stream().filter(p -> "Ana".equals(p.getName())).noneMatch(Player::isPaid));
   }
}
