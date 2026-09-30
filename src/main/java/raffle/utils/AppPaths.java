package raffle.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single place that knows where the application keeps its data.
 * <p>
 * The base folder is resolved from {@code user.home} on every call so that tests (and a future
 * "choose data folder" setting) can redirect it.
 */
public final class AppPaths {

   public static final String APP_DIR_NAME = "Sell & Win Raffle";

   private AppPaths() {
   }

   public static Path root() {
      return Paths.get(System.getProperty("user.home"), APP_DIR_NAME);
   }// end of root method

   public static Path dataDir() {
      return root().resolve("data");
   }// end of dataDir method

   public static Path catalogFile() {
      return dataDir().resolve("data.csv");
   }// end of catalogFile method

   public static Path drawHistoryFile() {
      return dataDir().resolve("draws.csv");
   }// end of drawHistoryFile method

   public static Path recordsDir() {
      return root().resolve("records");
   }// end of recordsDir method

   public static Path recordsFile(String itemTitle) {
      return recordsDir().resolve(itemTitle + ".csv");
   }// end of recordsFile method

   public static Path itemDir(String itemTitle) {
      return root().resolve(itemTitle);
   }// end of itemDir method

   public static Path backupsDir() {
      return root().resolve("backups");
   }// end of backupsDir method

}// end of AppPaths class
