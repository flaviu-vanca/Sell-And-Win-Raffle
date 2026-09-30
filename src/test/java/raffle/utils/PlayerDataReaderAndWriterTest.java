package raffle.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import raffle.models.Player;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerDataReaderAndWriterTest {

   @TempDir
   Path dir;

   @Test
   void roundTripKeepsQuotesAndCommasInNames() throws IOException {
      Path file = dir.resolve("Bike.csv");
      List<Player> players = List.of(new Player(1, "", "", 0),
                                     new Player(2, "O\"Brien, Jr.", "+40712345678", 3));

      PlayerDataReaderAndWriter.writePlayersToCSV(players, file);
      List<Player> read = PlayerDataReaderAndWriter.readPlayersFromFile(file);

      assertEquals(2, read.size());
      assertFalse(read.get(0).isSold());
      assertEquals("O\"Brien, Jr.", read.get(1).getName());
      assertEquals("+40712345678", read.get(1).getPhoneNumber());
      assertEquals(3, read.get(1).getNumberOfTickets());
   }

   @Test
   void readsFilesWrittenByOlderVersions() throws IOException {
      Path file = dir.resolve("legacy.csv");
      Files.write(file, List.of("ID, Name, Phone Number, Purchased Tickets",
                                "1,,,",
                                "2,\"Ion Popescu\",\"0712345678\",3",
                                ""));

      List<Player> read = PlayerDataReaderAndWriter.readPlayersFromFile(file);

      assertEquals(2, read.size());
      assertFalse(read.get(0).isSold());
      assertEquals(0, read.get(0).getNumberOfTickets());
      assertTrue(read.get(1).isSold());
      assertEquals("Ion Popescu", read.get(1).getName());
   }

   @Test
   void writeSortsByIdWithoutReorderingTheCallersList() throws IOException {
      Path file = dir.resolve("Bike.csv");
      List<Player> players = new ArrayList<>(List.of(new Player(3, "C", "0700000003", 1),
                                                     new Player(1, "A", "0700000001", 1)));

      PlayerDataReaderAndWriter.writePlayersToCSV(players, file);

      assertEquals(3, players.getFirst().getId(), "the caller's list must not be reordered");
      assertEquals(List.of(1, 3), PlayerDataReaderAndWriter.readPlayersFromFile(file).stream().map(Player::getId).toList());
   }
}
