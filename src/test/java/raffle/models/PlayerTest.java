package raffle.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTest {

   @Test
   void ticketWithABuyerIsSold() {
      assertTrue(new Player(1, "Ion Popescu", "0712345678", 2).isSold());
   }

   @Test
   void emptyOrBlankNameMeansUnsold() {
      assertFalse(new Player(1, "", "", 0).isSold());
      assertFalse(new Player(1, "   ", "", 0).isSold());
   }

   @Test
   void clearingTheBuyerMakesTheTicketAvailableAgain() {
      Player player = new Player(3, "Ana", "0700000001", 1);
      player.setName("");
      player.setPhoneNumber("");
      player.setNumberOfTickets(0);

      assertFalse(player.isSold());
      assertEquals(3, player.getId());
   }

   @Test
   void clearingATicketAlsoForgetsWhatItWasSoldFor() {
      Player player = new Player(3, "Ana", "0700000001", 1, true, "2026-09-30T18:00:00Z", 1250);
      assertEquals(1250, player.getPriceCents());

      player.clear();

      assertEquals(0, player.getPriceCents());
      assertFalse(player.isSold());
   }
}
