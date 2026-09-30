package raffle.services;

import raffle.models.Player;
import raffle.storage.RaffleRepository;
import raffle.utils.PhoneNumbers;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Selling tickets, taking them back and recording who has paid. Each call reads the ledger, changes it and saves it. */
public final class SalesService {

   private final RaffleRepository repository;
   private final Random random;
   private final Clock clock;

   public SalesService(RaffleRepository repository) {
      this(repository, new Random(), Clock.systemUTC());
   }

   public SalesService(RaffleRepository repository, Random random, Clock clock) {
      this.repository = repository;
      this.random = random;
      this.clock = clock;
   }

   public List<Player> ledger(String itemTitle) throws IOException {
      return repository.ledger(itemTitle);
   }// end of ledger method

   /**
    * Sells tickets to a buyer and saves the ledger.
    *
    * @return the numbers of the tickets the buyer got
    * @throws ValidationException when the buyer details or the number of tickets are not acceptable
    */
   public List<Integer> sell(String itemTitle, String name, String phone, int tickets, boolean paid)
           throws ValidationException, IOException {
      if (name == null || name.isBlank()) {
         throw new ValidationException("val.nameEmpty");
      }// end of if block
      if (phone == null || phone.isBlank()) {
         throw new ValidationException("val.phoneEmpty");
      }// end of if block
      if (! PhoneNumbers.isValid(phone)) {
         throw new ValidationException("phone.invalid");
      }// end of if block
      if (tickets <= 0) {
         throw new ValidationException("val.ticketsPositive");
      }// end of if block

      List<Player> ledger = repository.ledger(itemTitle);
      List<Integer> sold = TicketSales.sell(ledger, name.trim(), PhoneNumbers.normalize(phone), tickets, paid,
                                            Instant.now(clock), random);
      repository.saveLedger(itemTitle, ledger);
      return sold;
   }// end of sell method

   /**
    * Takes tickets back (the buyer's record is removed, the tickets can be sold again) and saves the ledger.
    *
    * @return how many tickets were taken back
    */
   public int release(String itemTitle, Collection<Integer> ticketIds) throws IOException {
      List<Player> ledger = repository.ledger(itemTitle);
      int released = TicketSales.release(ledger, ticketIds);
      if (released > 0) {
         repository.saveLedger(itemTitle, ledger);
      }// end of if block
      return released;
   }// end of release method

   /**
    * Records that buyers have paid (or not) for all of their tickets, and saves the ledger.
    *
    * @param buyerKeys see {@link Player#buyerKey()}
    * @return how many tickets changed
    */
   public int setPaid(String itemTitle, Set<String> buyerKeys, boolean paid) throws IOException {
      List<Player> ledger = repository.ledger(itemTitle);
      int changed = Payments.setPaid(ledger, buyerKeys, paid);
      if (changed > 0) {
         repository.saveLedger(itemTitle, ledger);
      }// end of if block
      return changed;
   }// end of setPaid method

}// end of SalesService class
