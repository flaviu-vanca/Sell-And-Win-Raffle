package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemSalesTest {

   // 6 tickets: 3 sold and paid, 1 sold and unpaid, 2 unsold
   private static List<Player> ledger() {
      return List.of(new Player(1, "Ana", "0700000001", 2, true, ""), new Player(2, "Ana", "0700000001", 2, true, ""),
                     new Player(3, "Bob", "0700000002", 1, true, ""), new Player(4, "Cora", "0700000003", 1, false, ""),
                     new Player(5, "", "", 0, false, ""), new Player(6, "", "", 0, false, ""));
   }

   @Test
   void countsTicketsByState() {
      ItemSales sales = ItemSales.of(ledger(), 15.0);

      assertEquals(6, sales.totalTickets());
      assertEquals(4, sales.soldTickets());
      assertEquals(3, sales.paidTickets());
      assertEquals(1, sales.unpaidTickets());
      assertEquals(2, sales.availableTickets());
   }

   @Test
   void moneyIsCollectedOutstandingAndPotential() {
      ItemSales sales = ItemSales.of(ledger(), 15.0);

      assertEquals(45.0, sales.collected());
      assertEquals(15.0, sales.outstanding());
      assertEquals(90.0, sales.potential());
      assertEquals(4 / 6.0, sales.soldFraction(), 1e-9);
   }

   @Test
   void pricesWithCentsAddUpExactly() {
      // 99.50 * 3 in floating point is 298.5 but 0.1 * 3 is not 0.3: cents keep every sum exact
      assertEquals(298.5, ItemSales.of(List.of(), 99.5).priceOf(3));
      assertEquals(0.3, ItemSales.of(List.of(), 0.1).priceOf(3));
   }

   @Test
   void emptyLedgerHasNothingSold() {
      ItemSales sales = ItemSales.of(List.of(), 10.0);

      assertEquals(0, sales.soldFraction());
      assertEquals(0.0, sales.collected());
   }

   @Test
   void everyTicketCountsAtThePriceItWasSoldFor() {
      // two tickets were sold at 10.00 (one paid), one at 15.00 (paid); the price is 15.00 now; one ticket is unsold
      List<Player> ledger = List.of(new Player(1, "Ana", "0700000001", 2, true, "", 1000), new Player(2, "Ana", "0700000001", 2, false, "", 1000),
                                    new Player(3, "Bob", "0700000002", 1, true, "", 1500), new Player(4, "", "", 0));

      ItemSales sales = ItemSales.of(ledger, 15.0);

      assertEquals(25.0, sales.collected());
      assertEquals(10.0, sales.outstanding());
      assertEquals(50.0, sales.potential(), "what was sold for, plus the unsold ticket at the current price");
   }

   @Test
   void aSoldTicketWithNoRecordedPriceCountsAtTheItemsPrice() {
      List<Player> ledger = List.of(new Player(1, "Ana", "0700000001", 1, true, ""), new Player(2, "Bob", "0700000002", 1, false, ""));

      ItemSales sales = ItemSales.of(ledger, 12.0);

      assertEquals(12.0, sales.collected());
      assertEquals(12.0, sales.outstanding());
   }
}
