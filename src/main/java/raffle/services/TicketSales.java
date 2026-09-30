package raffle.services;

import raffle.models.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Selling and taking back tickets in a ledger. The ledger has a row for every ticket; a sold ticket has the
 * buyer's name, phone number, payment state and time, and every row of a buyer shows how many tickets that buyer
 * holds in total. Works on the list in memory: saving it is up to the caller.
 */
public final class TicketSales {

   private TicketSales() {
   }

   /** The numbers of the tickets nobody has bought. */
   public static List<Integer> availableIds(List<Player> ledger) {
      return ledger.stream().filter(ticket -> ! ticket.isSold()).map(Player::getId).toList();
   }// end of availableIds method

   /**
    * Sells randomly chosen unsold tickets to a buyer. A buyer who already holds tickets (same name, ignoring case,
    * and same phone number) gets the new ones added to them.
    *
    * @return the numbers of the tickets sold, in the order they were picked
    * @throws ValidationException when fewer tickets are left than were asked for
    */
   public static List<Integer> sell(List<Player> ledger, String name, String phone, int tickets, boolean paid,
                                    Instant soldAt, Random random) throws ValidationException {
      List<Player> available = new ArrayList<>(ledger.stream().filter(ticket -> ! ticket.isSold()).toList());
      if (tickets > available.size()) {
         throw new ValidationException("player.notEnough");
      }// end of if block

      List<Integer> sold = new ArrayList<>();
      for (int i = 0; i < tickets; i++) {
         Player ticket = available.remove(random.nextInt(available.size()));
         ticket.setName(name);
         ticket.setPhoneNumber(phone);
         ticket.setPaid(paid);
         ticket.setSoldAt(soldAt.toString());
         sold.add(ticket.getId());
      }// end of for loop

      refreshCounts(ledger);
      return sold;
   }// end of sell method

   /**
    * Makes the given tickets available again. Numbers that are not sold tickets of this ledger are ignored.
    *
    * @return how many tickets were taken back
    */
   public static int release(List<Player> ledger, Collection<Integer> ticketIds) {
      Set<Integer> wanted = new HashSet<>(ticketIds);
      int released = 0;
      for (Player ticket : ledger) {
         if (ticket.isSold() && wanted.contains(ticket.getId())) {
            ticket.clear();
            released++;
         }// end of if block
      }// end of for loop
      if (released > 0) {
         refreshCounts(ledger);
      }// end of if block
      return released;
   }// end of release method

   // Every row of a buyer shows the buyer's total; unsold rows show none
   private static void refreshCounts(List<Player> ledger) {
      Map<String, Integer> totals = new TreeMap<>();
      for (Player ticket : ledger) {
         if (ticket.isSold()) {
            totals.merge(ticket.buyerKey(), 1, Integer::sum);
         }// end of if block
      }// end of for loop
      for (Player ticket : ledger) {
         ticket.setNumberOfTickets(ticket.isSold() ? totals.get(ticket.buyerKey()) : 0);
      }// end of for loop
   }// end of refreshCounts method

}// end of TicketSales class
