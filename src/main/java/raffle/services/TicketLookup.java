package raffle.services;

import raffle.models.Player;
import raffle.utils.PhoneNumbers;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Finds tickets for the "check player status" screen. The query is a ticket number (fewer than 7 digits), a phone
 * number or a buyer's name. The result says what was found; wording it is up to the screen.
 */
public final class TicketLookup {

   /** All tickets of one buyer (same name and phone number). */
   public record Buyer(String name, String phone, List<Player> tickets) {

      public List<Integer> ticketIds() {
         return tickets.stream().map(Player::getId).toList();
      }// end of ticketIds method
   }

   public sealed interface Result {
      /** The item has no tickets at all. */
      record NoTickets() implements Result {
      }

      record UnknownTicket(int id) implements Result {
      }

      record TicketNotSold(int id) implements Result {
      }

      record TicketFound(Player ticket) implements Result {
      }

      /** The buyers that use this phone number. */
      record PhoneFound(String phone, List<Buyer> buyers) implements Result {
      }

      /** The buyers with this name, one entry per phone number. */
      record NameFound(List<Buyer> buyers) implements Result {
      }

      record NotFound() implements Result {
      }
   }

   private static final int PHONE_MIN_DIGITS = 7;

   private TicketLookup() {
   }

   public static Result find(List<Player> ledger, String query) {
      if (ledger.isEmpty()) {
         return new Result.NoTickets();
      }// end of if block

      String phone = PhoneNumbers.normalize(query.trim());
      boolean digitsOnly = ! phone.isEmpty() && phone.chars().allMatch(Character::isDigit);

      // A number shorter than a phone number is a ticket number
      if (digitsOnly && phone.length() < PHONE_MIN_DIGITS) {
         int id = Integer.parseInt(phone);
         return ledger.stream().filter(ticket -> ticket.getId() == id).findFirst()
                      .<Result>map(ticket -> ticket.isSold() ? new Result.TicketFound(ticket) : new Result.TicketNotSold(id))
                      .orElseGet(() -> new Result.UnknownTicket(id));
      }// end of if block

      List<Player> sold = ledger.stream().filter(Player::isSold).toList();

      List<Buyer> byPhone = buyers(sold.stream().filter(ticket -> ticket.getPhoneNumber().equals(phone)).toList());
      if (! byPhone.isEmpty()) {
         return new Result.PhoneFound(phone, byPhone);
      }// end of if block

      String name = query.trim();
      List<Buyer> byName = buyers(sold.stream().filter(ticket -> ticket.getName().equalsIgnoreCase(name)).toList());
      return byName.isEmpty() ? new Result.NotFound() : new Result.NameFound(byName);
   }// end of find method

   // Groups tickets by buyer, in name and phone order
   private static List<Buyer> buyers(List<Player> tickets) {
      Map<String, List<Player>> groups = new TreeMap<>();
      for (Player ticket : tickets) {
         groups.computeIfAbsent(ticket.getName().toLowerCase(Locale.ROOT) + "|" + ticket.getPhoneNumber(), key -> new ArrayList<>()).add(ticket);
      }// end of for loop
      List<Buyer> buyers = new ArrayList<>();
      for (List<Player> group : groups.values()) {
         group.sort(Comparator.comparingInt(Player::getId));
         buyers.add(new Buyer(group.getFirst().getName(), group.getFirst().getPhoneNumber(), group));
      }// end of for loop
      return buyers;
   }// end of buyers method

}// end of TicketLookup class
