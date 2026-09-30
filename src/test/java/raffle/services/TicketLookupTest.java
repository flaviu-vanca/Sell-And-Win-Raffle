package raffle.services;

import org.junit.jupiter.api.Test;
import raffle.models.Player;
import raffle.services.TicketLookup.Result;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TicketLookupTest {

   private final List<Player> ledger = List.of(
           new Player(1, "Ion Popescu", "0712345678", 2, true, ""),
           new Player(2, "", "", 0),
           new Player(3, "Ion Popescu", "0712345678", 2, false, ""),
           new Player(4, "Ana Ionescu", "0712345678", 1, true, ""),
           new Player(5, "Ion Popescu", "0799999999", 1, true, ""),
           new Player(10, "", "", 0));

   @Test
   void aShortNumberIsATicketNumber() {
      Result.TicketFound found = assertInstanceOf(Result.TicketFound.class, TicketLookup.find(ledger, " 3 "));
      assertEquals("Ion Popescu", found.ticket().getName());
   }

   @Test
   void anUnsoldTicketIsReportedAsNotSold() {
      assertEquals(new Result.TicketNotSold(2), TicketLookup.find(ledger, "2"));
   }

   @Test
   void aTicketThatDoesNotExistIsReported() {
      assertEquals(new Result.UnknownTicket(99), TicketLookup.find(ledger, "99"));
   }

   @Test
   void aPhoneNumberFindsEveryBuyerUsingIt() {
      Result.PhoneFound found = assertInstanceOf(Result.PhoneFound.class, TicketLookup.find(ledger, "0712 345 678"));

      assertEquals("0712345678", found.phone());
      assertEquals(2, found.buyers().size());
      assertEquals("Ana Ionescu", found.buyers().get(0).name());
      assertEquals(List.of(4), found.buyers().get(0).ticketIds());
      assertEquals("Ion Popescu", found.buyers().get(1).name());
      assertEquals(List.of(1, 3), found.buyers().get(1).ticketIds());
   }

   @Test
   void aNameFindsTheBuyerForEachPhoneNumber() {
      Result.NameFound found = assertInstanceOf(Result.NameFound.class, TicketLookup.find(ledger, "ion popescu"));

      assertEquals(2, found.buyers().size());
      assertEquals("0712345678", found.buyers().get(0).phone());
      assertEquals(List.of(1, 3), found.buyers().get(0).ticketIds());
      assertEquals("0799999999", found.buyers().get(1).phone());
   }

   @Test
   void unknownNamesAndNumbersAreNotFound() {
      assertEquals(new Result.NotFound(), TicketLookup.find(ledger, "Maria"));
      assertEquals(new Result.NotFound(), TicketLookup.find(ledger, "0700000000"));
      assertEquals(new Result.NotFound(), TicketLookup.find(ledger, "+"));
   }

   @Test
   void anItemWithoutTicketsHasNothingToFind() {
      assertEquals(new Result.NoTickets(), TicketLookup.find(List.of(), "Ion"));
   }
}
