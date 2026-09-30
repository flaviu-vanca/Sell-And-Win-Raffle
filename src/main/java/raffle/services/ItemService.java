package raffle.services;

import raffle.models.Item;
import raffle.models.Player;
import raffle.storage.RaffleRepository;
import raffle.utils.ImageFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Adding and removing items. The data goes to the repository; the pictures of an item stay as ordinary files in
 * a folder named after the item, directly below the application folder.
 */
public final class ItemService {

   private final RaffleRepository repository;
   private final Path root;

   public ItemService(RaffleRepository repository, Path root) {
      this.repository = repository;
      this.root = root;
   }

   /** A title usable as a folder name: trimmed, and characters Windows does not allow in file names become "_". */
   public static String cleanTitle(String title) {
      return title == null ? "" : title.trim().replaceAll("[<>:\"/\\\\|?*]", "_");
   }// end of cleanTitle method

   /** Every item with its sales figures. A ledger that cannot be read counts as having no sales. */
   public List<ItemOverview> overview() throws IOException {
      List<ItemOverview> overview = new ArrayList<>();
      for (Item item : repository.items()) {
         List<Player> ledger = List.of();
         try {
            ledger = repository.ledger(item.getTitle());
         } catch (IOException e) {
            System.err.println("Could not read the tickets of " + item.getTitle() + ": " + e.getMessage());
         }// end of try-catch block
         overview.add(new ItemOverview(item, ItemSales.of(ledger, item.getPrice())));
      }// end of for loop
      return overview;
   }// end of overview method

   /**
    * Adds an item with one unsold ticket per number. The picture, if any, is copied into the item's folder.
    *
    * @param picture a picture file to copy in, or null
    * @throws ValidationException when something is missing or not acceptable, or the title is taken
    */
   public Item create(String rawTitle, String description, int tickets, double price, Path picture)
           throws ValidationException, IOException {
      String title = cleanTitle(rawTitle);
      if (title.isEmpty()) {
         throw new ValidationException("val.titleEmpty");
      }// end of if block
      if (description == null || description.isBlank()) {
         throw new ValidationException("val.descriptionEmpty");
      }// end of if block
      if (tickets <= 0) {
         throw new ValidationException("val.ticketsPositiveInt");
      }// end of if block
      if (! (price > 0) || Double.isInfinite(price)) {
         throw new ValidationException("val.pricePositive");
      }// end of if block

      Path folder = root.resolve(title);
      if (Files.exists(folder) || repository.hasItem(title)) {
         throw new ValidationException("additem.exists", title);
      }// end of if block

      Path copiedPicture = null;
      try {
         Files.createDirectories(folder);
         if (picture != null) {
            copiedPicture = ImageFiles.copyInto(folder, picture);
         }// end of if block

         // Without a picture the image is stored as empty text and the screens show the logo
         Item item = new Item(copiedPicture == null ? "" : copiedPicture.toString(), title, description.trim(), tickets, price);
         repository.addItem(item, emptyTickets(tickets));
         return item;
      } catch (IOException | RuntimeException e) {
         // Do not leave a half created item behind: its folder would block adding the same title again
         deleteFolder(folder);
         throw e;
      }// end of try-catch block
   }// end of create method

   /**
    * Changes what can change about an item: its description, its price and its number of tickets. Tickets that
    * were sold keep the price they were sold at. More tickets add unsold ones at the end; fewer tickets drop the
    * last ones, which is only possible while they are unsold.
    *
    * @throws ValidationException when something is not acceptable, or tickets that are already sold would be dropped
    */
   public void update(String title, String description, int totalTickets, double price) throws ValidationException, IOException {
      if (description == null || description.isBlank()) {
         throw new ValidationException("val.descriptionEmpty");
      }// end of if block
      if (totalTickets <= 0) {
         throw new ValidationException("val.ticketsPositiveInt");
      }// end of if block
      if (! (price > 0) || Double.isInfinite(price)) {
         throw new ValidationException("val.pricePositive");
      }// end of if block

      Item item = repository.item(title).orElseThrow(() -> new IOException("There is no item named " + title));
      List<Player> ledger = repository.ledger(title);
      TicketSales.freezePrices(ledger, Math.round(item.getPrice() * 100));
      repository.updateItem(title, description.trim(), price, TicketSales.resized(ledger, totalTickets));
   }// end of update method

   /** Removes an item. Its ticket ledger is kept in the backups; its pictures are deleted. */
   public void delete(String title) throws IOException {
      repository.removeItem(title);
      deleteFolder(root.resolve(title));
   }// end of delete method

   /** The picture shown for the item on the main screen. */
   public void setMainPicture(String title, Path picture) throws IOException {
      repository.setItemImage(title, picture.toAbsolutePath().toString());
   }// end of setMainPicture method

   // One unsold ticket per number
   private static List<Player> emptyTickets(int count) {
      List<Player> tickets = new ArrayList<>(count);
      for (int id = 1; id <= count; id++) {
         tickets.add(new Player(id, "", "", 0));
      }// end of for loop
      return tickets;
   }// end of emptyTickets method

   // The folder only holds pictures; anything that cannot be deleted is left behind
   private static void deleteFolder(Path folder) {
      if (! Files.isDirectory(folder)) {
         return;
      }// end of if block
      try (Stream<Path> files = Files.list(folder)) {
         for (Path file : files.toList()) {
            Files.deleteIfExists(file);
         }// end of for loop
         Files.deleteIfExists(folder);
      } catch (IOException e) {
         System.err.println("Could not delete " + folder + ": " + e.getMessage());
      }// end of try-catch block
   }// end of deleteFolder method

}// end of ItemService class
