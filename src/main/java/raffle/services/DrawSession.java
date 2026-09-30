package raffle.services;

import raffle.models.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * One sitting of drawing winners for an item: how many winners are wanted, who has won so far, and who can
 * still win. Earlier winners never win again, and with "one prize per person" neither do their other tickets.
 */
public final class DrawSession {

   private final RaffleDraw draw;
   private final List<Player> ledger;
   private final int winnersWanted;
   private final boolean onePrizePerPerson;
   private final List<Player> winners = new ArrayList<>();

   public DrawSession(RaffleDraw draw, List<Player> ledger, int winnersWanted, boolean onePrizePerPerson) {
      this.draw = draw;
      this.ledger = List.copyOf(ledger);
      this.winnersWanted = Math.max(1, winnersWanted);
      this.onePrizePerPerson = onePrizePerPerson;
   }

   /** Draws the next winner, or empty when the session is complete or nobody eligible is left. */
   public Optional<Player> drawNext() {
      if (isComplete()) {
         return Optional.empty();
      }// end of if block
      Optional<Player> winner = draw.pickWinner(ledger, winningTicketIds(), excludedBuyers());
      winner.ifPresent(winners::add);
      return winner;
   }// end of drawNext method

   public List<Player> winners() {
      return List.copyOf(winners);
   }// end of winners method

   public int winnersWanted() {
      return winnersWanted;
   }// end of winnersWanted method

   public boolean isComplete() {
      return winners.size() >= winnersWanted;
   }// end of isComplete method

   public int eligibleTickets() {
      return RaffleDraw.eligibleTickets(ledger, winningTicketIds(), excludedBuyers()).size();
   }// end of eligibleTickets method

   public boolean canDrawMore() {
      return ! isComplete() && eligibleTickets() > 0;
   }// end of canDrawMore method

   private Set<Integer> winningTicketIds() {
      Set<Integer> ids = new HashSet<>();
      winners.forEach(winner -> ids.add(winner.getId()));
      return ids;
   }// end of winningTicketIds method

   private Set<String> excludedBuyers() {
      Set<String> buyers = new HashSet<>();
      if (onePrizePerPerson) {
         winners.forEach(winner -> buyers.add(winner.buyerKey()));
      }// end of if block
      return buyers;
   }// end of excludedBuyers method

}// end of DrawSession class
