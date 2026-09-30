package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Where the raffle data lives. The rest of the application only talks to this interface, so the data can be kept
 * in CSV files or in a database without the screens knowing the difference.
 * <p>
 * Every method either does all of its work or fails with an {@link IOException} and leaves the data as it was.
 * An item is identified by its title. A ledger has one {@link Player} row per ticket, sold or not.
 */
public interface RaffleRepository extends AutoCloseable {

   /** The catalog in title order. {@link Item#getTickets()} is the number of tickets still available. */
   List<Item> items() throws IOException;

   /** One item by its title. */
   default Optional<Item> item(String title) throws IOException {
      return items().stream().filter(item -> item.getTitle().equals(title)).findFirst();
   }// end of item method

   /** Whether the title is taken: by an item, or by leftover data that would clash with a new item. */
   boolean hasItem(String title) throws IOException;

   /** Adds an item together with its ticket ledger. */
   void addItem(Item item, List<Player> tickets) throws IOException;

   /**
    * Changes the description and the price of an item and replaces its ledger, all or nothing. The title and the
    * picture stay. The ledger is complete: the caller adds or drops unsold tickets to change the number of tickets.
    * A sold ticket without a recorded price is stored at the price the item had <em>before</em> this change.
    */
   void updateItem(String title, String description, double price, List<Player> ledger) throws IOException;

   /**
    * Removes an item from the catalog. Its ledger holds buyers and money owed, so it is kept in
    * {@code backups/deleted/} instead of being destroyed.
    */
   void removeItem(String title) throws IOException;

   /** The picture shown for the item on the main screen; empty text means the logo. */
   void setItemImage(String title, String image) throws IOException;

   /** The tickets of an item in ticket order; empty when the item has no ledger. */
   List<Player> ledger(String title) throws IOException;

   /** Replaces the ledger of an item. */
   void saveLedger(String title, List<Player> ledger) throws IOException;

   /** Adds a line to the draw history. */
   void recordDraw(String itemTitle, Player winner, Instant time) throws IOException;

   /** The draw history, oldest first. */
   List<DrawEntry> draws() throws IOException;

   @Override
   default void close() {
   }

}// end of RaffleRepository interface
