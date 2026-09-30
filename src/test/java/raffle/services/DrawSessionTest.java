package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrawSessionTest {

   // Ana holds tickets 1-3, Bob 4-5, Cora 6, tickets 7-8 are unsold
   private static List<Player> ledger() {
      return List.of(new Player(1, "Ana", "0700000001", 3), new Player(2, "Ana", "0700000001", 3), new Player(3, "Ana", "0700000001", 3),
                     new Player(4, "Bob", "0700000002", 2), new Player(5, "Bob", "0700000002", 2),
                     new Player(6, "Cora", "0700000003", 1),
                     new Player(7, "", "", 0), new Player(8, "", "", 0));
   }

   @Test
   void drawsTheWantedNumberOfDifferentTickets() {
      DrawSession session = new DrawSession(new RaffleDraw(new Random(1)), ledger(), 4, false);

      Set<Integer> seen = new HashSet<>();
      for (int i = 0; i < 4; i++) {
         assertTrue(seen.add(session.drawNext().orElseThrow().getId()), "a ticket must not win twice");
      }
      assertTrue(session.isComplete());
      assertEquals(Optional.empty(), session.drawNext(), "no more winners once the wanted number is reached");
   }

   @Test
   void onePrizePerPersonKeepsAllTicketsOfEarlierWinnersOut() {
      for (long seed = 0; seed < 200; seed++) {
         DrawSession session = new DrawSession(new RaffleDraw(new Random(seed)), ledger(), 3, true);

         Set<String> people = new HashSet<>();
         for (int i = 0; i < 3; i++) {
            assertTrue(people.add(session.drawNext().orElseThrow().getName()), "the same person won twice (seed " + seed + ")");
         }
         assertEquals(Set.of("Ana", "Bob", "Cora"), people);
      }
   }

   @Test
   void sessionEndsWhenEveryBuyerHasWon() {
      DrawSession session = new DrawSession(new RaffleDraw(new Random(3)), ledger(), 10, true);

      for (int i = 0; i < 3; i++) {
         assertTrue(session.canDrawMore());
         session.drawNext().orElseThrow();
      }
      assertFalse(session.isComplete(), "10 winners were wanted, only 3 people can win");
      assertFalse(session.canDrawMore());
      assertEquals(0, session.eligibleTickets());
      assertEquals(Optional.empty(), session.drawNext());
   }

   @Test
   void withoutThePersonRuleOnePersonCanWinWithDifferentTickets() {
      List<Player> onlyAna = List.of(new Player(1, "Ana", "0700000001", 3), new Player(2, "Ana", "0700000001", 3), new Player(3, "Ana", "0700000001", 3));
      DrawSession session = new DrawSession(new RaffleDraw(new Random(5)), onlyAna, 3, false);

      assertEquals(3, session.eligibleTickets());
      for (int i = 0; i < 3; i++) {
         session.drawNext().orElseThrow();
      }
      assertTrue(session.isComplete());
      assertEquals(Set.of(1, 2, 3), new HashSet<>(session.winners().stream().map(Player::getId).toList()));
   }

   @Test
   void sameNameWithADifferentPhoneIsADifferentPerson() {
      List<Player> twoAnas = List.of(new Player(1, "Ana", "0700000001", 1), new Player(2, "ana", "0700000009", 1));
      DrawSession session = new DrawSession(new RaffleDraw(new Random(2)), twoAnas, 2, true);

      session.drawNext().orElseThrow();
      assertTrue(session.canDrawMore(), "a different phone number means a different buyer");
   }

   @Test
   void sameBuyerIsRecognisedDespiteCaseAndSpaces() {
      assertEquals(new Player(1, "Ana ", "0700000001", 1).buyerKey(), new Player(2, "ANA", "0700000001", 1).buyerKey());
   }

   @Test
   void winnersAreListedInDrawOrderAndAnUnsoldLedgerCannotBeDrawn() {
      DrawSession session = new DrawSession(new RaffleDraw(new Random(9)), ledger(), 2, true);
      Player first = session.drawNext().orElseThrow();
      Player second = session.drawNext().orElseThrow();
      assertEquals(List.of(first, second), session.winners());

      DrawSession empty = new DrawSession(new RaffleDraw(), List.of(new Player(1, "", "", 0)), 1, true);
      assertFalse(empty.canDrawMore());
      assertEquals(Optional.empty(), empty.drawNext());
   }
}
