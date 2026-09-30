package raffle.storage;

import raffle.models.DrawEntry;
import raffle.models.Item;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/** Moves all the data from one repository to another: importing the CSV files into the database, or exporting to CSV. */
public final class CsvTransfer {

   /** What was copied. */
   public record Summary(int items, int tickets, int draws) {
   }

   private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

   private CsvTransfer() {
   }

   /** Copies every item with its tickets, and the draw history. The target is expected to be empty. */
   public static Summary copy(RaffleRepository from, RaffleRepository to) throws IOException {
      int tickets = 0;
      List<Item> items = from.items();
      for (Item item : items) {
         List<Player> ledger = from.ledger(item.getTitle());
         to.addItem(item, ledger);
         tickets += ledger.size();
      }// end of for loop

      List<DrawEntry> draws = from.draws();
      for (DrawEntry draw : draws) {
         to.recordDraw(draw.itemTitle(), new Player(draw.ticketId(), draw.winnerName(), draw.phoneNumber(), 0), draw.time());
      }// end of for loop
      return new Summary(items.size(), tickets, draws.size());
   }// end of copy method

   /**
    * Writes all the data as CSV files, in the layout of earlier versions, into a new folder named after the time
    * below {@code exportsDir}.
    *
    * @return the folder with the files
    */
   public static Path exportTo(RaffleRepository from, Path exportsDir) throws IOException {
      Path folder = exportsDir.resolve(STAMP.format(LocalDateTime.now()));
      copy(from, new CsvRaffleRepository(folder));

      // Writing a file twice leaves a ".bak" of the first version: not wanted in an export
      try (Stream<Path> files = Files.walk(folder)) {
         for (Path file : files.filter(path -> path.getFileName().toString().endsWith(".bak")).toList()) {
            Files.deleteIfExists(file);
         }// end of for loop
      }// end of try-with-resources
      return folder;
   }// end of exportTo method

}// end of CsvTransfer class
