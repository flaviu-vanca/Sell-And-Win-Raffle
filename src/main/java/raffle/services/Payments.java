package raffle.services;

import raffle.models.Player;

import java.util.List;
import java.util.Set;

/** Changes to who has paid. A buyer pays for all of their tickets together. */
public final class Payments {

   private Payments() {
   }

   /**
    * Sets the paid state of every sold ticket that belongs to one of the given buyers.
    *
    * @return how many tickets changed
    */
   public static int setPaid(List<Player> ledger, Set<String> buyerKeys, boolean paid) {
      int changed = 0;
      for (Player ticket : ledger) {
         if (ticket.isSold() && buyerKeys.contains(ticket.buyerKey()) && ticket.isPaid() != paid) {
            ticket.setPaid(paid);
            changed++;
         }// end of if block
      }// end of for loop
      return changed;
   }// end of setPaid method

}// end of Payments class
