package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;
import raffle.utils.BackupService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps everything in one SQLite file. Each change is one transaction, so a crash or a full disk leaves the data
 * as it was before the change. The application is the only user of the file; one connection is kept open.
 */
public final class SqliteRaffleRepository implements RaffleRepository {

   private final Path root;
   private final Connection connection;

   /**
    * @param root   the application folder, where ledgers of deleted items are archived
    * @param dbFile the database file; created, with its folder, when it does not exist
    */
   public SqliteRaffleRepository(Path root, Path dbFile) throws IOException {
      this.root = root;
      Connection opened = null;
      try {
         Files.createDirectories(dbFile.toAbsolutePath().getParent());
         opened = DriverManager.getConnection("jdbc:sqlite:" + dbFile.toAbsolutePath());
         try (Statement statement = opened.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
         }// end of try-with-resources
         SqliteSchema.migrate(opened);
         this.connection = opened;
      } catch (SQLException e) {
         closeQuietly(opened);
         throw new IOException("Could not open the database " + dbFile.getFileName() + ": " + e.getMessage(), e);
      }// end of try-catch block
   }

   @FunctionalInterface
   private interface Work<T> {
      T run() throws SQLException, IOException;
   }

   // Runs the work as one transaction: all of it is saved, or none of it
   private synchronized <T> T inTransaction(Work<T> work) throws IOException {
      try {
         connection.setAutoCommit(false);
         try {
            T result = work.run();
            connection.commit();
            return result;
         } catch (SQLException | IOException | RuntimeException e) {
            try {
               connection.rollback();
            } catch (SQLException rollbackFailure) {
               e.addSuppressed(rollbackFailure);
            }// end of try-catch block
            throw e;
         } finally {
            connection.setAutoCommit(true);
         }// end of try-catch-finally block
      } catch (SQLException e) {
         throw new IOException("Database error: " + e.getMessage(), e);
      }// end of try-catch block
   }// end of inTransaction method

   @Override
   public List<Item> items() throws IOException {
      return inTransaction(() -> {
         List<Item> items = new ArrayList<>();
         try (Statement statement = connection.createStatement();
              ResultSet result = statement.executeQuery("""
                      SELECT i.title, i.description, i.image, i.price,
                             (SELECT COUNT(*) FROM tickets t WHERE t.item_id = i.id AND t.buyer_name = '') AS available
                      FROM items i ORDER BY i.title""")) {
            while (result.next()) {
               items.add(new Item(result.getString("image"), result.getString("title"), result.getString("description"),
                                  result.getInt("available"), result.getDouble("price")));
            }// end of while loop
         }// end of try-with-resources
         return items;
      });
   }// end of items method

   @Override
   public boolean hasItem(String title) throws IOException {
      return inTransaction(() -> itemId(title) != null);
   }// end of hasItem method

   @Override
   public void addItem(Item item, List<Player> tickets) throws IOException {
      inTransaction(() -> {
         long id;
         try (PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO items (title, description, image, price, created_at) VALUES (?, ?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            insert.setString(1, item.getTitle());
            insert.setString(2, item.getDescription());
            insert.setString(3, item.getImage() == null ? "" : item.getImage());
            insert.setDouble(4, item.getPrice());
            insert.setString(5, Instant.now().toString());
            insert.executeUpdate();
            try (ResultSet keys = insert.getGeneratedKeys()) {
               keys.next();
               id = keys.getLong(1);
            }// end of try-with-resources
         }// end of try-with-resources
         insertTickets(id, tickets);
         return null;
      });
   }// end of addItem method

   @Override
   public void removeItem(String title) throws IOException {
      inTransaction(() -> {
         Long id = itemId(title);
         if (id == null) {
            return null;
         }// end of if block
         // The ledger holds buyers and money owed: keep a copy as a CSV file before it is deleted
         BackupService.archiveLedger(root, title, readLedger(id));
         try (PreparedStatement delete = connection.prepareStatement("DELETE FROM items WHERE id = ?")) {
            delete.setLong(1, id);
            delete.executeUpdate();// the tickets go with it (ON DELETE CASCADE)
         }// end of try-with-resources
         return null;
      });
   }// end of removeItem method

   @Override
   public void setItemImage(String title, String image) throws IOException {
      inTransaction(() -> {
         try (PreparedStatement update = connection.prepareStatement("UPDATE items SET image = ? WHERE title = ?")) {
            update.setString(1, image == null ? "" : image);
            update.setString(2, title);
            update.executeUpdate();
         }// end of try-with-resources
         return null;
      });
   }// end of setItemImage method

   @Override
   public List<Player> ledger(String title) throws IOException {
      return inTransaction(() -> {
         Long id = itemId(title);
         return id == null ? new ArrayList<>() : readLedger(id);
      });
   }// end of ledger method

   @Override
   public void saveLedger(String title, List<Player> ledger) throws IOException {
      inTransaction(() -> {
         Long id = itemId(title);
         if (id == null) {
            throw new IOException("There is no item named " + title);
         }// end of if block
         try (PreparedStatement delete = connection.prepareStatement("DELETE FROM tickets WHERE item_id = ?")) {
            delete.setLong(1, id);
            delete.executeUpdate();
         }// end of try-with-resources
         insertTickets(id, ledger);
         return null;
      });
   }// end of saveLedger method

   @Override
   public void recordDraw(String itemTitle, Player winner, Instant time) throws IOException {
      inTransaction(() -> {
         try (PreparedStatement insert = connection.prepareStatement(
                 "INSERT INTO draws (drawn_at, item_title, ticket_number, winner_name, winner_phone) VALUES (?, ?, ?, ?, ?)")) {
            insert.setString(1, time.toString());
            insert.setString(2, itemTitle);
            insert.setInt(3, winner.getId());
            insert.setString(4, winner.getName());
            insert.setString(5, winner.getPhoneNumber());
            insert.executeUpdate();
         }// end of try-with-resources
         return null;
      });
   }// end of recordDraw method

   @Override
   public List<DrawEntry> draws() throws IOException {
      return inTransaction(() -> {
         List<DrawEntry> draws = new ArrayList<>();
         try (Statement statement = connection.createStatement();
              ResultSet result = statement.executeQuery(
                      "SELECT drawn_at, item_title, ticket_number, winner_name, winner_phone FROM draws ORDER BY id")) {
            while (result.next()) {
               draws.add(new DrawEntry(Instant.parse(result.getString(1)), result.getString(2), result.getInt(3),
                                       result.getString(4), result.getString(5)));
            }// end of while loop
         }// end of try-with-resources
         return draws;
      });
   }// end of draws method

   private Long itemId(String title) throws SQLException {
      try (PreparedStatement select = connection.prepareStatement("SELECT id FROM items WHERE title = ?")) {
         select.setString(1, title);
         try (ResultSet result = select.executeQuery()) {
            return result.next() ? result.getLong(1) : null;
         }// end of try-with-resources
      }// end of try-with-resources
   }// end of itemId method

   private void insertTickets(long itemId, List<Player> tickets) throws SQLException {
      try (PreparedStatement insert = connection.prepareStatement(
              "INSERT INTO tickets (item_id, number, buyer_name, buyer_phone, paid, sold_at) VALUES (?, ?, ?, ?, ?, ?)")) {
         for (Player ticket : tickets) {
            boolean sold = ticket.isSold();// an unsold ticket has no buyer details, whatever is left in the row
            insert.setLong(1, itemId);
            insert.setInt(2, ticket.getId());
            insert.setString(3, sold ? ticket.getName() : "");
            insert.setString(4, sold ? ticket.getPhoneNumber() : "");
            insert.setInt(5, sold && ticket.isPaid() ? 1 : 0);
            insert.setString(6, sold ? ticket.getSoldAt() : "");
            insert.addBatch();
         }// end of for loop
         insert.executeBatch();
      }// end of try-with-resources
   }// end of insertTickets method

   // The tickets in order. How many tickets a buyer holds is worked out here, not stored.
   private List<Player> readLedger(long itemId) throws SQLException {
      List<Player> ledger = new ArrayList<>();
      try (PreparedStatement select = connection.prepareStatement(
              "SELECT number, buyer_name, buyer_phone, paid, sold_at FROM tickets WHERE item_id = ? ORDER BY number")) {
         select.setLong(1, itemId);
         try (ResultSet result = select.executeQuery()) {
            while (result.next()) {
               ledger.add(new Player(result.getInt(1), result.getString(2), result.getString(3), 0,
                                     result.getInt(4) != 0, result.getString(5)));
            }// end of while loop
         }// end of try-with-resources
      }// end of try-with-resources

      Map<String, Integer> perBuyer = new HashMap<>();
      ledger.stream().filter(Player::isSold).forEach(ticket -> perBuyer.merge(ticket.buyerKey(), 1, Integer::sum));
      ledger.stream().filter(Player::isSold).forEach(ticket -> ticket.setNumberOfTickets(perBuyer.get(ticket.buyerKey())));
      return ledger;
   }// end of readLedger method

   @Override
   public synchronized void close() {
      closeQuietly(connection);
   }// end of close method

   private static void closeQuietly(Connection connection) {
      if (connection != null) {
         try {
            connection.close();
         } catch (SQLException e) {
            System.err.println("Could not close the database: " + e.getMessage());
         }// end of try-catch block
      }// end of if block
   }// end of closeQuietly method

}// end of SqliteRaffleRepository class
