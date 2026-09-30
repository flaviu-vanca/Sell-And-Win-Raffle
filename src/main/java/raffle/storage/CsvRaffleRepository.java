package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;
import raffle.utils.BackupService;
import raffle.utils.ItemDataReaderAndWriter;
import raffle.utils.PlayerDataReaderAndWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * The CSV file layout of the application: {@code data/data.csv} for the catalog, {@code data/draws.csv} for the
 * draw history and one {@code records/<title>.csv} ledger per item.
 */
public final class CsvRaffleRepository implements RaffleRepository {

   private final Path root;
   private final CsvDrawLog drawLog;

   public CsvRaffleRepository(Path root) {
      this.root = root;
      this.drawLog = new CsvDrawLog(root.resolve("data").resolve("draws.csv"));
   }

   private Path catalogFile() {
      return root.resolve("data").resolve("data.csv");
   }// end of catalogFile method

   private Path ledgerFile(String title) {
      return root.resolve("records").resolve(title + ".csv");
   }// end of ledgerFile method

   @Override
   public List<Item> items() throws IOException {
      if (! Files.exists(catalogFile())) {
         return new ArrayList<>();
      }// end of if block
      try {
         return ItemDataReaderAndWriter.readItemsFromFile(catalogFile());
      } catch (NumberFormatException e) {
         throw new IOException("Malformed number in " + catalogFile().getFileName() + ": " + e.getMessage(), e);
      }// end of try-catch block
   }// end of items method

   @Override
   public boolean hasItem(String title) throws IOException {
      return Files.exists(ledgerFile(title)) || items().stream().anyMatch(item -> item.getTitle().equals(title));
   }// end of hasItem method

   @Override
   public void addItem(Item item, List<Player> tickets) throws IOException {
      Path ledger = ledgerFile(item.getTitle());
      Files.createDirectories(ledger.getParent());
      PlayerDataReaderAndWriter.writePlayersToCSV(tickets, ledger);
      try {
         List<Item> items = items();
         items.add(item);
         Files.createDirectories(catalogFile().getParent());
         ItemDataReaderAndWriter.writeItemsToCSV(items, catalogFile());
      } catch (IOException | RuntimeException e) {
         Files.deleteIfExists(ledger);// do not leave a ledger without a catalog entry: it would block the title
         throw e;
      }// end of try-catch block
   }// end of addItem method

   @Override
   public void updateItem(String title, String description, double price, List<Player> ledger) throws IOException {
      List<Item> items = items();
      Item item = items.stream().filter(candidate -> candidate.getTitle().equals(title)).findFirst()
                       .orElseThrow(() -> new IOException("There is no item named " + title));

      // Sold tickets without a recorded price were sold at the old price: pin it before the price changes
      long oldPriceCents = Math.round(item.getPrice() * 100);
      ledger.stream().filter(ticket -> ticket.isSold() && ticket.getPriceCents() <= 0).forEach(ticket -> ticket.setPriceCents(oldPriceCents));

      // The ledger first: if the catalog cannot be written afterwards, the old price stays and nothing is misread
      PlayerDataReaderAndWriter.writePlayersToCSV(ledger, ledgerFile(title));
      item.setDescription(description);
      item.setPrice(price);
      item.setTickets((int) ledger.stream().filter(ticket -> ! ticket.isSold()).count());
      ItemDataReaderAndWriter.writeItemsToCSV(items, catalogFile());
   }// end of updateItem method

   @Override
   public void removeItem(String title) throws IOException {
      List<Item> remaining = items().stream().filter(item -> ! item.getTitle().equals(title)).toList();
      ItemDataReaderAndWriter.writeItemsToCSV(remaining, catalogFile());
      Path ledger = ledgerFile(title);
      if (Files.exists(ledger)) {
         BackupService.archive(root, ledger);
      }// end of if block
   }// end of removeItem method

   @Override
   public void setItemImage(String title, String image) throws IOException {
      List<Item> items = items();
      items.stream().filter(item -> item.getTitle().equals(title)).forEach(item -> item.setImage(image));
      ItemDataReaderAndWriter.writeItemsToCSV(items, catalogFile());
   }// end of setItemImage method

   @Override
   public List<Player> ledger(String title) throws IOException {
      Path file = ledgerFile(title);
      if (! Files.exists(file)) {
         return new ArrayList<>();
      }// end of if block
      try {
         return PlayerDataReaderAndWriter.readPlayersFromFile(file);
      } catch (NumberFormatException e) {
         throw new IOException("Malformed number in " + file.getFileName() + ": " + e.getMessage(), e);
      }// end of try-catch block
   }// end of ledger method

   @Override
   public void saveLedger(String title, List<Player> ledger) throws IOException {
      PlayerDataReaderAndWriter.writePlayersToCSV(ledger, ledgerFile(title));
      keepCatalogCountCurrent(title, ledger);
   }// end of saveLedger method

   // data.csv also shows how many tickets are still available. Nothing in the application reads that number
   // (it comes from the ledger), so failing to update it must not fail the save.
   private void keepCatalogCountCurrent(String title, List<Player> ledger) {
      long available = ledger.stream().filter(ticket -> ! ticket.isSold()).count();
      try {
         List<Item> items = items();
         boolean changed = false;
         for (Item item : items) {
            if (item.getTitle().equals(title) && item.getTickets() != available) {
               item.setTickets((int) available);
               changed = true;
            }// end of if block
         }// end of for loop
         if (changed) {
            ItemDataReaderAndWriter.writeItemsToCSV(items, catalogFile());
         }// end of if block
      } catch (IOException e) {
         System.err.println("Could not update the ticket count in " + catalogFile() + ": " + e.getMessage());
      }// end of try-catch block
   }// end of keepCatalogCountCurrent method

   @Override
   public void recordDraw(String itemTitle, Player winner, Instant time) throws IOException {
      drawLog.record(itemTitle, winner, time);
   }// end of recordDraw method

   @Override
   public List<DrawEntry> draws() throws IOException {
      return drawLog.readAll();
   }// end of draws method

}// end of CsvRaffleRepository class
