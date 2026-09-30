package raffle.models;

import java.time.Instant;

/** One line of the draw history: who won which item, with which ticket, and when. */
public record DrawEntry(Instant time, String itemTitle, int ticketId, String winnerName, String phoneNumber) {
}
