package raffle.services;

import raffle.models.Player;

import java.util.List;

/**
 * Sales figures of one item, worked out from its ticket ledger. Money is counted in cents so that sums of
 * prices like 99.50 do not pick up floating point noise.
 *
 * @param totalTickets tickets the item started with
 * @param soldTickets  tickets that have a buyer
 * @param paidTickets  sold tickets that are paid for
 * @param priceCents   price of one ticket in cents
 */
public record ItemSales(int totalTickets, int soldTickets, int paidTickets, long priceCents) {

   public static ItemSales of(List<Player> ledger, double ticketPrice) {
      int sold = 0;
      int paid = 0;
      for (Player ticket : ledger) {
         if (ticket.isSold()) {
            sold++;
            if (ticket.isPaid()) {
               paid++;
            }// end of if block
         }// end of if block
      }// end of for loop
      return new ItemSales(ledger.size(), sold, paid, Math.round(ticketPrice * 100));
   }// end of of method

   public int availableTickets() {
      return totalTickets - soldTickets;
   }// end of availableTickets method

   public int unpaidTickets() {
      return soldTickets - paidTickets;
   }// end of unpaidTickets method

   /** Money received for paid tickets. */
   public double collected() {
      return collectedCents() / 100.0;
   }// end of collected method

   /** Money still owed for sold tickets that are not paid yet. */
   public double outstanding() {
      return outstandingCents() / 100.0;
   }// end of outstanding method

   /** What selling every ticket would bring in. */
   public double potential() {
      return potentialCents() / 100.0;
   }// end of potential method

   public long collectedCents() {
      return paidTickets * priceCents;
   }// end of collectedCents method

   public long outstandingCents() {
      return unpaidTickets() * priceCents;
   }// end of outstandingCents method

   public long potentialCents() {
      return totalTickets * priceCents;
   }// end of potentialCents method

   /** Share of tickets sold, 0 to 1. */
   public double soldFraction() {
      return totalTickets == 0 ? 0 : soldTickets / (double) totalTickets;
   }// end of soldFraction method

   /** What the given number of tickets costs. */
   public double priceOf(int tickets) {
      return tickets * priceCents / 100.0;
   }// end of priceOf method

}// end of ItemSales record
