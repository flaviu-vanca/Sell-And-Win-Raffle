package raffle.services;

import raffle.models.Player;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.random.RandomGenerator;

/**
 * Picks a raffle winner. No JavaFX, no files, so it can be tested and (later) reused unchanged by a database
 * backed version.
 * <p>
 * The winner is drawn uniformly from the tickets that were actually sold, using a cryptographically strong
 * random source. The outcome therefore cannot be influenced by when the operator presses Stop, and a draw can
 * no longer land on an unsold ticket.
 */
public final class RaffleDraw {

   private final RandomGenerator random;

   public RaffleDraw() {
      this(new SecureRandom());
   }

   public RaffleDraw(RandomGenerator random) {
      this.random = random;
   }

   /** Ticket rows that have a buyer. A buyer with several tickets appears once per ticket. */
   public static List<Player> soldTickets(List<Player> ledger) {
      return ledger.stream().filter(Player::isSold).toList();
   }// end of soldTickets method

   /**
    * @param ledger            every ticket row of the item
    * @param excludedTicketIds tickets that must not win (for example earlier winners of the same item)
    * @return the winning ticket row, or empty when no eligible ticket exists
    */
   public Optional<Player> pickWinner(List<Player> ledger, Set<Integer> excludedTicketIds) {
      List<Player> eligible = soldTickets(ledger).stream()
                                                 .filter(ticket -> ! excludedTicketIds.contains(ticket.getId()))
                                                 .toList();
      if (eligible.isEmpty()) {
         return Optional.empty();
      }// end of if block
      return Optional.of(eligible.get(random.nextInt(eligible.size())));
   }// end of pickWinner method

}// end of RaffleDraw class
