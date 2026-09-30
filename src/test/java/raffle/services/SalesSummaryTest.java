package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SalesSummaryTest {

   private static List<Player> bikeLedger() {
      // 4 tickets, 3 sold (2 paid, 1 owed)
      return List.of(new Player(1, "Ana", "0700000001", 2, true, ""), new Player(2, "Ana", "0700000001", 2, true, ""),
                     new Player(3, "Bob", "0700000002", 1, false, ""), new Player(4, "", "", 0, false, ""));
   }

   private static List<Player> phoneLedger() {
      // 2 tickets, 1 sold and paid
      return List.of(new Player(1, "Cora", "0700000003", 1, true, ""), new Player(2, "", "", 0, false, ""));
   }

   @Test
   void addsUpEveryItem() {
      SalesSummary summary = SalesSummary.of(List.of(ItemSales.of(bikeLedger(), 15.0), ItemSales.of(phoneLedger(), 99.5)));

      assertEquals(2, summary.items());
      assertEquals(6, summary.totalTickets());
      assertEquals(4, summary.soldTickets());
      assertEquals(3, summary.paidTickets());
      assertEquals(2 * 15.0 + 99.5, summary.collected());
      assertEquals(15.0, summary.outstanding());
      assertEquals(4 * 15.0 + 2 * 99.5, summary.potential());
      assertEquals(4 / 6.0, summary.soldFraction(), 1e-9);
   }

   @Test
   void sumsInCentsSoPricesLike0Point1AddUpExactly() {
      List<Player> tenSold = java.util.stream.IntStream.rangeClosed(1, 10).mapToObj(id -> new Player(id, "Buyer", "0700000001", 10, true, "")).toList();
      SalesSummary summary = SalesSummary.of(List.of(ItemSales.of(tenSold, 0.1)));

      assertEquals(1.0, summary.collected(), "ten tickets at 0.10 must be exactly 1.00");
   }

   @Test
   void noItemsMeansAllZero() {
      SalesSummary summary = SalesSummary.of(List.of());

      assertEquals(0, summary.items());
      assertEquals(0.0, summary.soldFraction());
      assertEquals(0.0, summary.collected());
   }
}
