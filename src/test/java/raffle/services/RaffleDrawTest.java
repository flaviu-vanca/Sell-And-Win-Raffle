package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RaffleDrawTest {

   // 20 tickets like the demo item: only 2, 4 and 17 are sold
   private static List<Player> ledgerWithThreeSold() {
      List<Player> ledger = new ArrayList<>();
      for (int id = 1; id <= 20; id++) {
         boolean sold = id == 2 || id == 4 || id == 17;
         ledger.add(new Player(id, sold ? "Ion Popescu" : "", sold ? "0712345678" : "", sold ? 3 : 0));
      }
      return ledger;
   }

   @Test
   void neverPicksAnUnsoldTicket() {
      RaffleDraw draw = new RaffleDraw(new SecureRandom());
      List<Player> ledger = ledgerWithThreeSold();

      for (int i = 0; i < 2000; i++) {
         Player winner = draw.pickWinner(ledger, Set.of()).orElseThrow();
         assertTrue(winner.isSold(), "ticket " + winner.getId() + " is not sold");
      }
   }

   @Test
   void everySoldTicketCanWin() {
      RaffleDraw draw = new RaffleDraw(new Random(7));
      List<Player> ledger = ledgerWithThreeSold();

      Set<Integer> seen = new java.util.HashSet<>();
      for (int i = 0; i < 500; i++) {
         seen.add(draw.pickWinner(ledger, Set.of()).orElseThrow().getId());
      }
      assertEquals(Set.of(2, 4, 17), seen);
   }

   @Test
   void emptyWhenNothingIsSold() {
      List<Player> ledger = List.of(new Player(1, "", "", 0), new Player(2, "", "", 0));
      assertEquals(Optional.empty(), new RaffleDraw().pickWinner(ledger, Set.of()));
   }

   @Test
   void excludedTicketsCannotWin() {
      RaffleDraw draw = new RaffleDraw(new Random(1));
      List<Player> ledger = ledgerWithThreeSold();

      for (int i = 0; i < 200; i++) {
         assertEquals(17, draw.pickWinner(ledger, Set.of(2, 4)).orElseThrow().getId());
      }
      assertEquals(Optional.empty(), draw.pickWinner(ledger, Set.of(2, 4, 17)));
   }

   @Test
   void chanceIsProportionalToTicketsBought() {
      // Ana holds 3 tickets and Bob 1, so Ana should win about 75% of the time
      List<Player> ledger = List.of(new Player(1, "Ana", "0700000001", 3),
                                    new Player(2, "Ana", "0700000001", 3),
                                    new Player(3, "Ana", "0700000001", 3),
                                    new Player(4, "Bob", "0700000002", 1),
                                    new Player(5, "", "", 0));
      RaffleDraw draw = new RaffleDraw(new Random(42));

      int draws = 40_000;
      int anaWins = 0;
      for (int i = 0; i < draws; i++) {
         if (draw.pickWinner(ledger, Set.of()).orElseThrow().getName().equals("Ana")) {
            anaWins++;
         }
      }
      assertEquals(0.75, anaWins / (double) draws, 0.01);
   }
}
