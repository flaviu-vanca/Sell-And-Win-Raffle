package raffle.reports;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.services.ItemSales;
import raffle.services.SalesSummary;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Everything the sales report shows: each item with its figures and its buyers, the figures of the whole raffle,
 * and the draws. Money is in cents.
 */
public record SalesReport(Instant generatedAt, List<ItemReport> items, SalesSummary summary, List<DrawEntry> draws) {

   /** All the tickets of one buyer for one item. {@code paidCents} and {@code owedCents} add up to what the buyer's tickets cost. */
   public record BuyerLine(String name, String phone, List<Integer> ticketIds, long paidCents, long owedCents) {

      public long totalCents() {
         return paidCents + owedCents;
      }// end of totalCents method
   }

   public record ItemReport(Item item, ItemSales sales, List<BuyerLine> buyers) {
   }

   /** A buyer who still has to pay for an item. */
   public record OwedLine(String itemTitle, BuyerLine buyer) {
   }

   /** The buyers who still owe money, those who owe most first. */
   public List<OwedLine> owedLines() {
      List<OwedLine> owed = new ArrayList<>();
      for (ItemReport item : items) {
         for (BuyerLine buyer : item.buyers()) {
            if (buyer.owedCents() > 0) {
               owed.add(new OwedLine(item.item().getTitle(), buyer));
            }// end of if block
         }// end of for loop
      }// end of for loop
      owed.sort(Comparator.<OwedLine>comparingLong(line -> - line.buyer().owedCents())
                          .thenComparing(line -> line.buyer().name().toLowerCase(java.util.Locale.ROOT)));
      return owed;
   }// end of owedLines method

}// end of SalesReport record
