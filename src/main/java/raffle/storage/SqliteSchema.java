package raffle.storage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * The layout of the database and how it grows. The version of the layout is kept in the database itself
 * ({@code PRAGMA user_version}); a database made by an older version of the application is brought up to date
 * step by step, each step in one transaction, so a change never leaves it half done. Never edit a step that has
 * been released: add a new one at the end.
 */
final class SqliteSchema {

   private static final List<List<String>> STEPS = List.of(
           // 1: items, their tickets and the draw history
           List.of("""
                   CREATE TABLE items (
                      id          INTEGER PRIMARY KEY AUTOINCREMENT,
                      title       TEXT NOT NULL UNIQUE,
                      description TEXT NOT NULL,
                      image       TEXT NOT NULL DEFAULT '',
                      price       REAL NOT NULL,
                      created_at  TEXT NOT NULL
                   )""",
                   """
                   CREATE TABLE tickets (
                      item_id     INTEGER NOT NULL REFERENCES items(id) ON DELETE CASCADE,
                      number      INTEGER NOT NULL,
                      buyer_name  TEXT NOT NULL DEFAULT '',
                      buyer_phone TEXT NOT NULL DEFAULT '',
                      paid        INTEGER NOT NULL DEFAULT 0,
                      sold_at     TEXT NOT NULL DEFAULT '',
                      PRIMARY KEY (item_id, number)
                   ) WITHOUT ROWID""",
                   """
                   CREATE TABLE draws (
                      id            INTEGER PRIMARY KEY AUTOINCREMENT,
                      drawn_at      TEXT NOT NULL,
                      item_title    TEXT NOT NULL,
                      ticket_number INTEGER NOT NULL,
                      winner_name   TEXT NOT NULL,
                      winner_phone  TEXT NOT NULL
                   )"""),
           // 2: what every ticket was sold for, so that a later price change does not reach back. Tickets sold
           // so far were sold at the price the item has now.
           List.of("ALTER TABLE tickets ADD COLUMN price_cents INTEGER NOT NULL DEFAULT 0",
                   """
                   UPDATE tickets SET price_cents =
                      CAST(ROUND((SELECT price FROM items WHERE items.id = tickets.item_id) * 100) AS INTEGER)
                   WHERE buyer_name <> ''"""));

   private SqliteSchema() {
   }

   /** The version this application works with. */
   static int currentVersion() {
      return STEPS.size();
   }// end of currentVersion method

   static void migrate(Connection connection) throws SQLException {
      migrate(connection, STEPS.size());
   }// end of migrate method

   /** Brings the database up to the given version (tests use this to build the database of an older release). */
   static void migrate(Connection connection, int target) throws SQLException {
      int version = version(connection);
      if (version > STEPS.size()) {
         throw new SQLException("This data was saved by a newer version of the application (layout " + version
                                + ", this version knows up to " + STEPS.size() + "). Update the application.");
      }// end of if block

      for (int step = version; step < target; step++) {
         connection.setAutoCommit(false);
         try (Statement statement = connection.createStatement()) {
            for (String sql : STEPS.get(step)) {
               statement.execute(sql);
            }// end of for loop
            statement.execute("PRAGMA user_version = " + (step + 1));
            connection.commit();
         } catch (SQLException e) {
            connection.rollback();
            throw e;
         } finally {
            connection.setAutoCommit(true);
         }// end of try-catch block
      }// end of for loop
   }// end of migrate method

   static int version(Connection connection) throws SQLException {
      try (Statement statement = connection.createStatement();
           ResultSet result = statement.executeQuery("PRAGMA user_version")) {
         return result.next() ? result.getInt(1) : 0;
      }// end of try-with-resources
   }// end of version method

}// end of SqliteSchema class
