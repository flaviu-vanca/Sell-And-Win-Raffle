package raffle.reports;

import java.time.Instant;
import java.util.List;

/**
 * What a buyer gets for one purchase: the tickets bought together, what they cost and whether they are paid.
 *
 * @param soldAt when the tickets were sold, or null when the ledger does not say
 */
public record Receipt(String itemTitle, String itemDescription, String buyerName, String phone, List<Integer> ticketIds,
                      long priceCents, long totalCents, int paidTickets, Instant soldAt) {

   public boolean fullyPaid() {
      return paidTickets == ticketIds.size();
   }// end of fullyPaid method

}// end of Receipt record
