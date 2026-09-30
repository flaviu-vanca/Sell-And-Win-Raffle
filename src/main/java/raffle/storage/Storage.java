package raffle.storage;

import raffle.utils.AppPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

/**
 * The repository the application runs on. Data is kept in a SQLite database ({@code data/raffle.db}). When the
 * application starts and finds no database but the CSV files of an earlier version, it converts them first; the
 * CSV files are left exactly as they were.
 */
public final class Storage {

   /** Something the operator should be told after start-up: the message key and its arguments. */
   public record Notice(String messageKey, boolean warning, Object... arguments) {
   }

   private static RaffleRepository repository;
   private static Notice notice;

   private Storage() {
   }

   public static synchronized RaffleRepository repository() {
      if (repository == null) {
         try {
            open(AppPaths.root());
         } catch (IOException e) {
            throw new IllegalStateException("The data could not be opened: " + e.getMessage(), e);
         }// end of try-catch block
      }// end of if block
      return repository;
   }// end of repository method

   /**
    * Opens the data in the application folder, converting the CSV files of an earlier version if there is no
    * database yet. If that conversion fails the CSV files stay in use for this session and a notice says why, so
    * nothing is ever lost or started empty.
    *
    * @throws IOException when the database exists but cannot be opened
    */
   public static synchronized RaffleRepository open(Path root) throws IOException {
      close();
      Path database = databaseFile(root);
      if (! Files.exists(database) && Files.exists(root.resolve("data").resolve("data.csv"))) {
         try {
            CsvTransfer.Summary summary = importCsv(root, database);
            if (summary.items() > 0) {
               notice = new Notice("storage.imported", false, String.valueOf(summary.items()), String.valueOf(summary.tickets()));
            }// end of if block
         } catch (IOException | RuntimeException e) {
            System.err.println("Could not convert the CSV files: " + e.getMessage());
            notice = new Notice("storage.importFailed", true, String.valueOf(e.getMessage()));
            repository = new CsvRaffleRepository(root);
            return repository;
         }// end of try-catch block
      }// end of if block
      repository = new SqliteRaffleRepository(root, database);
      return repository;
   }// end of open method

   static Path databaseFile(Path root) {
      return root.resolve("data").resolve("raffle.db");
   }// end of databaseFile method

   // Converts into a temporary file that only gets its real name when everything was copied and checked
   private static CsvTransfer.Summary importCsv(Path root, Path database) throws IOException {
      Path temporary = database.resolveSibling(database.getFileName() + ".importing");
      Files.deleteIfExists(temporary);
      CsvTransfer.Summary summary;
      try (SqliteRaffleRepository target = new SqliteRaffleRepository(root, temporary)) {
         summary = CsvTransfer.copy(new CsvRaffleRepository(root), target);
         if (target.items().size() != summary.items() || target.draws().size() != summary.draws()) {
            throw new IOException("The converted data does not match the CSV files");
         }// end of if block
      } catch (IOException | RuntimeException e) {
         Files.deleteIfExists(temporary);
         throw e;
      }// end of try-catch block
      Files.move(temporary, database, StandardCopyOption.ATOMIC_MOVE);
      return summary;
   }// end of importCsv method

   /** The notice for the operator, if any. It is handed out once. */
   public static synchronized Optional<Notice> takeNotice() {
      Optional<Notice> taken = Optional.ofNullable(notice);
      notice = null;
      return taken;
   }// end of takeNotice method

   /** Replaces the repository (tests). */
   public static synchronized void use(RaffleRepository replacement) {
      close();
      repository = replacement;
   }// end of use method

   public static synchronized void close() {
      if (repository != null) {
         repository.close();
         repository = null;
      }// end of if block
   }// end of close method

}// end of Storage class
