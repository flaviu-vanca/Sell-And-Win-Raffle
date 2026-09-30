package raffle.storage;

import raffle.utils.AppPaths;

/** The repository the application runs on. Screens and services get it from here. */
public final class Storage {

   private static RaffleRepository repository;

   private Storage() {
   }

   public static synchronized RaffleRepository repository() {
      if (repository == null) {
         repository = new CsvRaffleRepository(AppPaths.root());
      }// end of if block
      return repository;
   }// end of repository method

   /** Replaces the repository (tests, or a different store chosen at start-up). */
   public static synchronized void use(RaffleRepository replacement) {
      repository = replacement;
   }// end of use method

   public static synchronized void close() {
      if (repository != null) {
         repository.close();
         repository = null;
      }// end of if block
   }// end of close method

}// end of Storage class
