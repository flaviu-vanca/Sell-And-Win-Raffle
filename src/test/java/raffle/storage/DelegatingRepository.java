package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/** A repository that passes everything on to another one; tests override the calls they want to break. */
public class DelegatingRepository implements RaffleRepository {

   private final RaffleRepository real;

   public DelegatingRepository(RaffleRepository real) {
      this.real = real;
   }

   @Override
   public List<Item> items() throws IOException {
      return real.items();
   }

   @Override
   public boolean hasItem(String title) throws IOException {
      return real.hasItem(title);
   }

   @Override
   public void addItem(Item item, List<Player> tickets) throws IOException {
      real.addItem(item, tickets);
   }

   @Override
   public void updateItem(String title, String description, double price, List<Player> ledger) throws IOException {
      real.updateItem(title, description, price, ledger);
   }

   @Override
   public void removeItem(String title) throws IOException {
      real.removeItem(title);
   }

   @Override
   public void setItemImage(String title, String image) throws IOException {
      real.setItemImage(title, image);
   }

   @Override
   public List<Player> ledger(String title) throws IOException {
      return real.ledger(title);
   }

   @Override
   public void saveLedger(String title, List<Player> ledger) throws IOException {
      real.saveLedger(title, ledger);
   }

   @Override
   public void recordDraw(String itemTitle, Player winner, Instant time) throws IOException {
      real.recordDraw(itemTitle, winner, time);
   }

   @Override
   public List<DrawEntry> draws() throws IOException {
      return real.draws();
   }

   @Override
   public void close() {
      real.close();
   }
}
