package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentsTest {

   private static List<Player> ledger() {
      return List.of(new Player(1, "Ana", "0700000001", 2, false, ""), new Player(2, "Ana", "0700000001", 2, false, ""),
                     new Player(3, "Bob", "0700000002", 1, false, ""), new Player(4, "", "", 0, false, ""));
   }

   @Test
   void markingABuyerPaidCoversAllTheirTickets() {
      List<Player> ledger = ledger();
      int changed = Payments.setPaid(ledger, Set.of(ledger.get(0).buyerKey()), true);

      assertEquals(2, changed);
      assertTrue(ledger.get(0).isPaid());
      assertTrue(ledger.get(1).isPaid());
      assertFalse(ledger.get(2).isPaid(), "another buyer is not affected");
   }

   @Test
   void unsoldTicketsAreNeverMarked() {
      List<Player> ledger = ledger();
      Payments.setPaid(ledger, Set.of(ledger.get(3).buyerKey()), true);

      assertFalse(ledger.get(3).isPaid());
   }

   @Test
   void onlyRealChangesAreCounted() {
      List<Player> ledger = ledger();
      Set<String> ana = Set.of(ledger.get(0).buyerKey());

      assertEquals(2, Payments.setPaid(ledger, ana, true));
      assertEquals(0, Payments.setPaid(ledger, ana, true));
      assertEquals(2, Payments.setPaid(ledger, ana, false));
   }

   @Test
   void clearingATicketResetsItsPayment() {
      Player ticket = new Player(1, "Ana", "0700000001", 2, true, "2026-09-30T18:00:00Z");
      ticket.clear();

      assertFalse(ticket.isSold());
      assertFalse(ticket.isPaid());
      assertEquals("", ticket.getSoldAt());
   }
}
