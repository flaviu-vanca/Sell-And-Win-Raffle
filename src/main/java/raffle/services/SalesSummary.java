package raffle.services;

import java.util.Collection;

/**
 * The figures of the whole raffle: every item added together. Money is summed in cents.
 *
 * @param items           number of items
 * @param totalTickets    tickets the items started with
 * @param soldTickets     tickets that have a buyer
 * @param paidTickets     sold tickets that are paid for
 * @param collectedCents  money received
 * @param outstandingCents money still owed
 * @param potentialCents  what selling every ticket would bring in
 */
public record SalesSummary(int items, int totalTickets, int soldTickets, int paidTickets,
                           long collectedCents, long outstandingCents, long potentialCents) {

   public static SalesSummary of(Collection<ItemSales> sales) {
      int total = 0;
      int sold = 0;
      int paid = 0;
      long collected = 0;
      long outstanding = 0;
      long potential = 0;
      for (ItemSales item : sales) {
         total += item.totalTickets();
         sold += item.soldTickets();
         paid += item.paidTickets();
         collected += item.collectedCents();
         outstanding += item.outstandingCents();
         potential += item.potentialCents();
      }// end of for loop
      return new SalesSummary(sales.size(), total, sold, paid, collected, outstanding, potential);
   }// end of of method

   public double collected() {
      return collectedCents / 100.0;
   }// end of collected method

   public double outstanding() {
      return outstandingCents / 100.0;
   }// end of outstanding method

   public double potential() {
      return potentialCents / 100.0;
   }// end of potential method

   /** Share of tickets sold, 0 to 1. */
   public double soldFraction() {
      return totalTickets == 0 ? 0 : soldTickets / (double) totalTickets;
   }// end of soldFraction method

}// end of SalesSummary record
