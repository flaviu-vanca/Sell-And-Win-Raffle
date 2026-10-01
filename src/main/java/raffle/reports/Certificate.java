package raffle.reports;

import java.time.Instant;

/** A winner's certificate: who won which item with which ticket, and when. The phone number is not on it. */
public record Certificate(String itemTitle, String winnerName, int ticketId, Instant time) {
}
