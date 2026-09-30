package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketSalesTest {

   private static final Instant NOW = Instant.parse("2026-09-30T18:00:00Z");

   private static List<Player> ledger(int tickets) {
      return new ArrayList<>(IntStream.rangeClosed(1, tickets).mapToObj(id -> new Player(id, "", "", 0)).toList());
   }

   private static Player ticket(List<Player> ledger, int id) {
      return ledger.stream().filter(player -> player.getId() == id).findFirst().orElseThrow();
   }

   @Test
   void aSaleTakesDistinctUnsoldTickets() throws ValidationException {
      List<Player> ledger = ledger(10);

      List<Integer> sold = TicketSales.sell(ledger, "Ion", "0712345678", 4, true, NOW, new Random(1));

      assertEquals(4, sold.size());
      assertEquals(4, new HashSet<>(sold).size(), "no ticket is sold twice");
      assertEquals(6, TicketSales.availableIds(ledger).size());
      for (int id : sold) {
         Player ticket = ticket(ledger, id);
         assertEquals("Ion", ticket.getName());
         assertEquals("0712345678", ticket.getPhoneNumber());
         assertEquals(4, ticket.getNumberOfTickets());
         assertTrue(ticket.isPaid());
         assertEquals(NOW.toString(), ticket.getSoldAt());
      }
   }

   @Test
   void theSameBuyerGetsTheNewTicketsAddedAndEveryRowShowsTheTotal() throws ValidationException {
      List<Player> ledger = ledger(10);
      List<Integer> first = TicketSales.sell(ledger, "Ion Popescu", "0712345678", 2, false, NOW, new Random(2));

      List<Integer> second = TicketSales.sell(ledger, "ion popescu", "0712345678", 3, true, NOW, new Random(3));

      Set<Integer> all = new HashSet<>(first);
      all.addAll(second);
      assertEquals(5, all.size());
      for (int id : all) {
         assertEquals(5, ticket(ledger, id).getNumberOfTickets());
      }
      assertFalse(ticket(ledger, first.getFirst()).isPaid(), "earlier tickets keep their own payment state");
      assertTrue(ticket(ledger, second.getFirst()).isPaid());
   }

   @Test
   void theSameNameWithAnotherPhoneIsAnotherBuyer() throws ValidationException {
      List<Player> ledger = ledger(10);
      List<Integer> first = TicketSales.sell(ledger, "Ion", "0711111111", 2, true, NOW, new Random(1));

      List<Integer> second = TicketSales.sell(ledger, "Ion", "0722222222", 1, true, NOW, new Random(2));

      assertEquals(2, ticket(ledger, first.getFirst()).getNumberOfTickets());
      assertEquals(1, ticket(ledger, second.getFirst()).getNumberOfTickets());
   }

   @Test
   void askingForMoreThanAreLeftChangesNothing() throws ValidationException {
      List<Player> ledger = ledger(3);
      TicketSales.sell(ledger, "Ion", "0712345678", 2, true, NOW, new Random(1));

      ValidationException error = assertThrows(ValidationException.class,
              () -> TicketSales.sell(ledger, "Ana", "0700000000", 2, true, NOW, new Random(1)));

      assertEquals("player.notEnough", error.messageKey());
      assertEquals(1, TicketSales.availableIds(ledger).size());
   }

   @Test
   void theLastTicketCanBeSold() throws ValidationException {
      List<Player> ledger = ledger(2);

      TicketSales.sell(ledger, "Ion", "0712345678", 2, true, NOW, new Random(1));

      assertTrue(TicketSales.availableIds(ledger).isEmpty());
   }

   @Test
   void releasingTicketsMakesThemAvailableAndFixesTheBuyersTotal() throws ValidationException {
      List<Player> ledger = ledger(6);
      List<Integer> sold = TicketSales.sell(ledger, "Ion", "0712345678", 3, true, NOW, new Random(1));

      int released = TicketSales.release(ledger, List.of(sold.get(0)));

      assertEquals(1, released);
      assertFalse(ticket(ledger, sold.get(0)).isSold());
      assertEquals(0, ticket(ledger, sold.get(0)).getNumberOfTickets());
      assertEquals(2, ticket(ledger, sold.get(1)).getNumberOfTickets());
      assertEquals(2, ticket(ledger, sold.get(2)).getNumberOfTickets());
      assertEquals(4, TicketSales.availableIds(ledger).size());
   }

   @Test
   void releasingSeveralTicketsOfOneBuyerLeavesTheRightTotal() throws ValidationException {
      List<Player> ledger = ledger(6);
      List<Integer> sold = TicketSales.sell(ledger, "Ion", "0712345678", 4, true, NOW, new Random(1));

      assertEquals(3, TicketSales.release(ledger, sold.subList(0, 3)));

      assertEquals(1, ticket(ledger, sold.get(3)).getNumberOfTickets());
   }

   @Test
   void releasingUnsoldOrUnknownTicketsDoesNothing() {
      List<Player> ledger = ledger(3);

      assertEquals(0, TicketSales.release(ledger, List.of(1, 2, 99)));
      assertEquals(3, TicketSales.availableIds(ledger).size());
   }
}
